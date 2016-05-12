package repartiteur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

import calculateur.Server;

public class Repartiteur {

	private static final int DEFAULT_PORT = 2000;
	private static int port = DEFAULT_PORT;

	private static int MAX_REQUEST = 20;

	private static Server server;

	private static int cptRequest;

	// List<WorkerNode>
	private static List<WorkerNode> calculateurs;

	public static void main(String[] args) {
		if (args.length == 1) {
			port = Integer.parseInt(args[0]);
		}

		calculateurs = new ArrayList<WorkerNode>();

		run();
	}


	public static void run() {
		System.out.println("== Repartiteur launch on port " + port + " ==");

		WebServer webServer = new WebServer(port);

		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		try {
			phm.addHandler("Repartiteur", repartiteur.Repartiteur.class);
		} catch (XmlRpcException e) {
			e.printStackTrace();
		}
		xmlRpcServer.setHandlerMapping(phm);

		XmlRpcServerConfigImpl serverConfig =
				(XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
		serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);

		try {
			webServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		cptRequest = 0;

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				// System.out.println("charge: " + cptRequest);

				if (cptRequest > MAX_REQUEST) {
					// addWorkerNode();
				}

				int nbWorkerNodes = calculateurs.size();
				if (nbWorkerNodes * MAX_REQUEST > cptRequest) {
					// delWorkerNode();
				}

				cptRequest = 0;
			}
		}, 0, 1000);

		// TODO: remove
		//server = new Server(1500);
		//server.run();

		//addWorkerNode();
	}

	public static void addWorkerNode() {
		int workerNodeId = calculateurs.size();
		String cmd = "nova boot --flavor m1.small --image myUbuntuIsAmazing"
				+ " --nic net-id=c1445469-4640-4c5a-ad86-9c0cb6650cca --security-group default"
				+ " --key-name myKeyIsAmazing myUbuntuIsAmazing" + workerNodeId;

		executeProcess(cmd);

		cmd = "nova list | grep myUbuntuIsAmazing" + workerNodeId;

		System.out.println("Spawning VM...");

		while (!executeProcess(cmd).contains("Running")) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println("VM OK...");

		cmd = "neutron floatingip-create public | grep floating_ip_address";

		String result = executeProcess(cmd);

		String ip = result.split("\\|")[2].trim();
		System.out.println("ip:" + ip);

		cmd = "nova floating-ip-associate myUbuntuIsAmazing" + workerNodeId + " " + ip;

		executeProcess(cmd);

		System.out.println("Waiting ssh...");

		cmd = "ssh ubuntu@" + ip + " 'nohup java -jar Server.jar 19020 >/dev/null 2>/dev/null &'";

		while (executeProcessReturnCode(cmd) != 0) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println("ssh OK...");

		calculateurs.add(new WorkerNode(workerNodeId, ip));
	}

	// TODO: test
	public static void delWorkerNode() {
		WorkerNode workerNode = calculateurs.get(calculateurs.size()-1);
		calculateurs.remove(workerNode);

		String cmd = "nova delete myUbuntuIsAmazing" + workerNode.getId();

		executeProcess(cmd);

		cmd = "neutron floatingip-list | grep " + workerNode.getIp();

		String result = executeProcess(cmd);

		String idVM = result.split("\\|")[1].trim();
		System.out.println("id:" + idVM + "|");

		cmd = "neutron floatingip-delete " + idVM;
	}

	public int request(String method, int i1, int i2) {
		cptRequest++;

		System.out.println("Request received");

		// create configuration
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL("http://195.220.53.46:19020/xmlrpc"));
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		config.setEnabledForExtensions(true);  
		config.setConnectionTimeout(60 * 1000);
		config.setReplyTimeout(60 * 1000);

		XmlRpcClient client = new XmlRpcClient();

		// use Commons HttpClient as transport
		client.setTransportFactory(
				new XmlRpcCommonsTransportFactory(client));
		// set configuration
		client.setConfig(config);

		Object[] params = new Object[]
				{ new Integer(i1), new Integer(i2) };
		Integer result = null;
		try {
			result = (Integer) client.execute("Calculateur." + method, params);
		} catch (XmlRpcException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static String executeProcess(String cmd) {
		ProcessBuilder process = new ProcessBuilder("/bin/sh", "-c", cmd);

		Process p;
		StringBuilder sb = new StringBuilder();
		try {
			p = process.start();

			try {
				p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			InputStream is = p.getInputStream(); 
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String ligne;

			while (( ligne = br.readLine()) != null) { 
				sb.append(ligne).append(System.lineSeparator());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}

	public static int executeProcessReturnCode(String cmd) {
		ProcessBuilder process = new ProcessBuilder("/bin/sh", "-c", cmd);

		Process p = null;
		try {
			p = process.start();

			try {
				p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return p.exitValue();
	}

}
