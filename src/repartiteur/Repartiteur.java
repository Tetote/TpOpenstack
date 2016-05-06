package repartiteur;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import calculateur.WorkerNode;

public class Repartiteur {

	private static final int DEFAULT_PORT = 2000;
	private static int port = DEFAULT_PORT;


	private static Server server;

	private static int cptRequest;

	// List<WorkerNode>
	private static List<WorkerNode> calculateurs;

	public static void main(String[] args) throws Exception {
		if (args.length == 1) {
			port = Integer.parseInt(args[0]);
		}

		calculateurs = new ArrayList<WorkerNode>();

		run();
	}


	public static void run() throws Exception {
		System.out.println("== Repartiteur launch on port " + port + " ==");

		WebServer webServer = new WebServer(port);

		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		/* Load handler definitions from a property file.
		 * The property file might look like:
		 *   Calculator=org.apache.xmlrpc.demo.Calculator
		 *   org.apache.xmlrpc.demo.proxy.Adder=org.apache.xmlrpc.demo.proxy.AdderImpl
		 */

		phm.addHandler("Repartiteur", repartiteur.Repartiteur.class);

		/* You may also provide the handler classes directly,
		 * like this:
		 * phm.addHandler("Calculator",
		 *     org.apache.xmlrpc.demo.Calculator.class);
		 * phm.addHandler(org.apache.xmlrpc.demo.proxy.Adder.class.getName(),
		 *     org.apache.xmlrpc.demo.proxy.AdderImpl.class);
		 */
		xmlRpcServer.setHandlerMapping(phm);

		XmlRpcServerConfigImpl serverConfig =
				(XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
		serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);

		webServer.start();

		cptRequest = 0;

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				// System.out.println("charge: " + cptRequest);
				cptRequest = 0;
			}
		}, 0, 1000);


		server = new Server(1500);
		server.run();
	}

	public static void addWorkerNode() {

	}

	public static void delWorkerNode() {

	}


	// TODO: ecrire la méthode !
	public int request(String method, int i1, int i2) {
		cptRequest++;
		
		

		// create configuration
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL("http://127.0.0.1:1500/xmlrpc"));
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
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
			// System.out.println("2 + 3 = " + result);
		} catch (XmlRpcException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

}
