package client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import util.ColorUtil;

public class Client {

	private static final Random r = new Random();

	public static final int PORT_CLIENT = 19000;
	public static final String REPARTITEUR_IP = "127.0.0.1";
	public static final int REPARTITEUR_PORT = 19005;

	public static XmlRpcClient client;
	public static int requestRate = 18;
	public static int port = PORT_CLIENT;
	public static String repartiteurIp = REPARTITEUR_IP;
	public static int repartiteurPort = REPARTITEUR_PORT;

	private static TimerTask timerTask;
	private static Timer timer;

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println(ColorUtil.YELLOW + "[Client] launch with default params");
		} else if (args.length == 4) {
			port = Integer.parseInt(args[0]);
			requestRate = Integer.parseInt(args[1]);
			repartiteurIp = args[2];
			repartiteurPort = Integer.parseInt(args[3]);
		} else {
			System.out.println(ColorUtil.RED + "[Client] Usage : port requestRate repartiteurIp repartiteurPort");
			System.exit(0);
		}

		connectRepartiteur(repartiteurIp, repartiteurPort);

		startServer();

		runTimer();

		System.out.println(ColorUtil.GREEN + "== Client started on port " + port);
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
		WebServer webServer = new WebServer(port);

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
		createTimerTask();
		timer = new Timer();
		timer.scheduleAtFixedRate(timerTask, 1000, 1000/requestRate);
	}

	public boolean updateRate(int rate) {
		requestRate = rate;

		timerTask.cancel();
		createTimerTask();
		timer.scheduleAtFixedRate(timerTask, 1000, 1000/requestRate);

		return true;
	}

	private static void createTimerTask() {
		timerTask = new TimerTask() {
			@Override
			public void run() {
				new Thread() {
					public void run() {
						Integer i1 = r.nextInt(100);
						Integer i2 = r.nextInt(100);

						boolean methodAdd = r.nextBoolean();

						String method = methodAdd ? "add" : "subtract";
						String methodDisplay = methodAdd ? "+" : "-";

						Object[] params = new Object[] { method, i1, i2 };
						Integer result;
						try {
							result = (Integer) client.execute("Repartiteur.request", params);

							if (result != null) {
								System.out.println(ColorUtil.CYAN + "[Client]["+requestRate+"] "+i1+methodDisplay+i2+" = " + result);
							} else {
								System.out.println(ColorUtil.RED + "[Client]["+requestRate+"] Repartiteur not ready :(");
							}
						} catch (XmlRpcException e) {
							e.printStackTrace();
						}
					}
				}.start();
			}
		};
	}
}