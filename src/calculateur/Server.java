package calculateur;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.TypeConverterFactoryImpl;
//  import org.apache.xmlrpc.demo.webserver.proxy.impls.AdderImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class Server implements Runnable {
	private static final int DEFAULT_PORT = 8080;
	private int port;
	
	public Server(int port) {
		this.port = port;
	}

	public static void main(String[] args) throws Exception {
		int port = DEFAULT_PORT;
		if (args.length == 1) {
			port = Integer.parseInt(args[0]);
		}

		new Server(port).run();
	}

	@Override
	public void run() {
		System.out.println("== Server launch on port " + port + " ==");

		WebServer webServer = new WebServer(port);

		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

		PropertyHandlerMapping phm = new PropertyHandlerMapping();

		try {
			phm.addHandler("Calculateur", calculateur.Calculateur.class);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}