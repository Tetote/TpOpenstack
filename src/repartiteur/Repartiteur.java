package repartiteur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

	private static final int DEFAULT_PORT = 19005;
	private static int port = DEFAULT_PORT;

	private static int MAX_REQUEST = 20;

	private static int cptRequest;

	private static ArrayDeque<WorkerNode> calculateurs;
	
	public static void main(String[] args) {
		if (args.length == 1) {
			port = Integer.parseInt(args[0]);
		}

		calculateurs = new ArrayDeque<WorkerNode>();

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

		XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
		serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);

		try {
			webServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		addWorkerNode();
		
		cptRequest = 0;

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				// System.out.println("charge: " + cptRequest);

				if (cptRequest > MAX_REQUEST) {
					addWorkerNode();
				}

				int nbWorkerNodes = calculateurs.size();
				if (nbWorkerNodes * MAX_REQUEST > cptRequest) {
					delWorkerNode();
				}

				cptRequest = 0;
			}
		}, 0, 1000);		
	}

	public static void addWorkerNode() {
		int workerNodeId = calculateurs.size();
		String cmd = "nova boot --flavor m1.small --image myUbuntuIsAmazing"
				+ " --nic net-id=c1445469-4640-4c5a-ad86-9c0cb6650cca --security-group myRuleIsAmazing"
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

		calculateurs.addLast(new WorkerNode(workerNodeId, ip));
	}

	// TODO: test
	public static void delWorkerNode() {
		WorkerNode workerNode = calculateurs.pollLast();

		if (workerNode != null) {
			String cmd = "nova delete myUbuntuIsAmazing" + workerNode.getId();

			executeProcess(cmd);

			cmd = "neutron floatingip-list | grep " + workerNode.getIp();

			String result = executeProcess(cmd);

			String idVM = result.split("\\|")[1].trim();
			System.out.println("id:" + idVM + "|");

			cmd = "neutron floatingip-delete " + idVM;
		}		
	}

	//TODO: add thread
	public Future<Integer> request(String method, int i1, int i2) {
		cptRequest++;
		WorkerNode workerNode = calculateurs.pollFirst();		
		calculateurs.addLast(workerNode);
		
		int result = 0;
		ExecutorService pool = Executors.newFixedThreadPool(2); // creates a pool of threads for the Future to draw from

		Future<Integer> value = pool.submit(new Callable<Integer>() {
		    @Override
		    public Integer call() {
		    	System.out.println("Request received");

	    		XmlRpcClient client = workerNode.createConfiguration();

	    		Object[] params = new Object[]
	    				{ new Integer(i1), new Integer(i2) };
	    		Integer result = null;
	    		try {
	    			result = (Integer) client.execute("Calculateur." + method, params);
	    		} catch (XmlRpcException e) {
	    			e.printStackTrace();
	    		}
	    		
	    		System.out.println("Get result " + result);
	    		return result;
		    }
		});	    
		return value;
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
