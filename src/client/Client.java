package client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

public class Client {

	public static final int PORT_CLIENT = 19000;

	public static XmlRpcClient client;
	public static int requestRate = 18;

	public static void main(String[] args) throws Exception {

		String host = "127.0.0.1";
		int port = 19005;

		if (args.length == 3) {
			requestRate = Integer.parseInt(args[0]);
			host = args[1];
			port = Integer.parseInt(args[2]);
		}

		connectRepartiteur(host, port);

		startServer();

		runTimer();

		System.out.println("== Client started on port " + PORT_CLIENT);
	}

	public static void connectRepartiteur(String ip, int port) {
		// create configuration
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL("http://"+ip+":"+port+"/xmlrpc"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		config.setEnabledForExtensions(true);  
		config.setConnectionTimeout(60 * 1000);
		config.setReplyTimeout(60 * 1000);

		client = new XmlRpcClient();

		// use Commons HttpClient as transport
		client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));
		// set configuration
		client.setConfig(config);
	}

	public static void startServer() {
		WebServer webServer = new WebServer(PORT_CLIENT);

		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

		PropertyHandlerMapping phm = new PropertyHandlerMapping();

		try {
			phm.addHandler("Client", client.Client.class);
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
	}

	public static void runTimer() {
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				new Thread() {
					public void run() {				
						Object[] params = new Object[]
								{ new String("add"), new Integer(2), new Integer(3) };
						Integer result;
						try {
							result = (Integer) client.execute("Repartiteur.request", params);
							System.out.println("2 + 3 = " + result);
						} catch (XmlRpcException e) {
							e.printStackTrace();
						}
					};
				}.start();
			}
		}, 1000, 1000/requestRate);
	}

	public boolean updateRate(int rate) {
		requestRate = rate;

		return true;
	}
}