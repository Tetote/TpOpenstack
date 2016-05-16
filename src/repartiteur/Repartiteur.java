package repartiteur;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Random;
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

import util.AsciiArt;
import util.ColorUtil;
import util.CommandUtil;

public class Repartiteur {

	private static final Random r = new Random();

	private static final int DEFAULT_PORT = 19005;
	private static int port = DEFAULT_PORT;

	private static final int SERVER_PORT = 19020;

	private static int MAX_REQUEST = 50;

	private static int cptRequest;
	private static int nbVmInCreation;
	private static int nbVmInDeletion;

	private static ArrayDeque<WorkerNode> calculateurs;

	public static void main(String[] args) {
		if (args.length == 1) {
			port = Integer.parseInt(args[0]);
		}

		calculateurs = new ArrayDeque<WorkerNode>();

		System.out.println(AsciiArt.getNyanCat());

		run();
	}


	public static void run() {
		startServer();

		System.out.println(ColorUtil.YELLOW + "[Repartiteur] Creating main WorkerNode");
		addWorkerNode();

		cptRequest = 0;
		nbVmInCreation = 0;

		startMainTimer();

		// Suppression automatique des VMs lors d'un CTRL+C
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println(ColorUtil.YELLOW + "\n[Repartiteur] Shutdown all VM... Please wait...");
				for (WorkerNode node : calculateurs) {
					delWorkerNode(node);
				}
			}
		});

		System.out.println(ColorUtil.GREEN + "== Repartiteur launch on port " + port + " ==");
	}

	public static void startServer() {
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
	}

	/**
	 * Timer qui permet la detection de montee de charge (creation de VM) et descente de charge (suppression de VM)
	 */
	public static void startMainTimer() {
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				// System.out.println(ColorUtil.YELLOW + "[Repartiteur] Charge: " + cptRequest);
				int cptRequestT = cptRequest;
				int nbWorkerNodes = calculateurs.size();

				if (cptRequestT > (MAX_REQUEST * (nbWorkerNodes+nbVmInCreation))) {
					nbVmInCreation++;

					new Thread() {
						public void run() {
							addWorkerNode();
							nbVmInCreation--;
						};
					}.start();
				}

				if ((nbWorkerNodes-nbVmInDeletion)  > (cptRequestT/MAX_REQUEST + 1) && nbWorkerNodes > 1) {
					nbVmInDeletion++;

					new Thread() {
						public void run() {
							delWorkerNode();
							nbVmInDeletion--;
						};
					}.start();
				}

				cptRequest = 0;
			}
		}, 0, 1000);
	}

	/**
	 * Permet de creer un WorkerNode
	 */
	public static void addWorkerNode() {
		String workerNodeId = String.valueOf(System.currentTimeMillis()) + (char) (r.nextInt(122-97+1)+97);
		System.out.println(ColorUtil.YELLOW + "[Repartiteur][VM"+workerNodeId+"] Spawning a VM");
		String cmd = "nova boot --flavor m1.small --image myUbuntuIsAmazing"
				+ " --nic net-id=c1445469-4640-4c5a-ad86-9c0cb6650cca --security-group myRuleIsAmazing"
				+ " --key-name myKeyIsAmazing myUbuntuIsAmazing" + workerNodeId;


		// Attente de la creation de la VM (si trop de VMs...)
		while (CommandUtil.executeProcessReturnCode(cmd) != 0) {
			System.out.println(ColorUtil.RED + "[Repartiteur][VM"+workerNodeId+"] VM not created... Retrying...");
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		cmd = "nova list | grep myUbuntuIsAmazing" + workerNodeId;

		System.out.println(ColorUtil.YELLOW + "[Repartiteur][VM"+workerNodeId+"] Checking VM status...");

		// Attente du boot de la VM
		while (!CommandUtil.executeProcess(cmd).contains("Running")) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println(ColorUtil.YELLOW + "[Repartiteur][VM"+workerNodeId+"] "+ColorUtil.GREEN+"VM OK!");

		cmd = "neutron floatingip-create public | grep floating_ip_address";

		String result = CommandUtil.executeProcess(cmd);

		String ip = result.split("\\|")[2].trim();

		cmd = "nova floating-ip-associate myUbuntuIsAmazing" + workerNodeId + " " + ip;

		CommandUtil.executeProcess(cmd);

		System.out.println(ColorUtil.YELLOW + "[Repartiteur][VM"+workerNodeId+"] Waiting ssh for ip "+ip+"...");

		cmd = "ssh ubuntu@" + ip + " 'nohup java -jar Server.jar "+SERVER_PORT+" >/dev/null 2>/dev/null &'";

		// Attente du SSH
		while (CommandUtil.executeProcessReturnCode(cmd) != 0) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println(ColorUtil.YELLOW + "[Repartiteur][VM"+workerNodeId+"] "+ColorUtil.GREEN+"VM Ready!");

		// create configuration
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL("http://"+ip+":"+SERVER_PORT+"/xmlrpc"));
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		config.setEnabledForExtensions(true);  
		config.setConnectionTimeout(60 * 1000);
		config.setReplyTimeout(60 * 1000);

		XmlRpcClient client = new XmlRpcClient();

		// use Commons HttpClient as transport
		client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));
		// set configuration
		client.setConfig(config);

		calculateurs.addLast(new WorkerNode(workerNodeId, ip, client));
	}

	/**
	 * Permet de supprimer le dernier WorkerNode
	 */
	public static void delWorkerNode() {
		delWorkerNode(calculateurs.pollLast());
	}

	/**
	 * Permet de supprimer un WorkerNode
	 * @param workerNode
	 */
	public static void delWorkerNode(WorkerNode workerNode) {

		System.out.println(ColorUtil.YELLOW + "[Repartiteur] Deleting VM "+workerNode.getIp()+" ...");

		String cmd = "nova delete myUbuntuIsAmazing" + workerNode.getId();

		CommandUtil.executeProcess(cmd);

		cmd = "neutron floatingip-list | grep " + workerNode.getIp();

		String result = CommandUtil.executeProcess(cmd);

		String idVM = result.split("\\|")[1].trim();

		cmd = "neutron floatingip-delete " + idVM;

		CommandUtil.executeProcess(cmd);
	}

	/**
	 * Reception d'une requete d'un client
	 * @param methode add ou subtract
	 * @param i1 nombre 1
	 * @param i2 nombre 2
	 * @return resultat calcule par un WorkerNode
	 */
	public Integer request(String method, int i1, int i2) {
		cptRequest++;

		WorkerNode workerNode = calculateurs.peekFirst();

		if (workerNode == null) {
			return null;
		}

		System.out.println(ColorUtil.YELLOW + "[Repartiteur] Sending request to "+workerNode.getIp() + "...");

		synchronized (calculateurs) {
			calculateurs.addLast(calculateurs.pollFirst());
		}

		Object[] params = new Object[]
				{ new Integer(i1), new Integer(i2) };
		Integer result = null;
		try {
			result = (Integer) workerNode.getClient().execute("Calculateur." + method, params);
		} catch (XmlRpcException e) {
			e.printStackTrace();
		}

		return result;
	}

}
