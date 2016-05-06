package repartiteur;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class Repartiteur {
	
	private static final int DEFAULT_PORT = 2000;
	
	private static int port = DEFAULT_PORT;
	
	// Map<IP,port>
	private static Map<String, String> calculateurs;

	public static void main(String[] args) throws Exception {
		if (args.length == 1) {
			port = Integer.parseInt(args[0]);
		}
		
		calculateurs = new HashMap<String, String>();
		
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
	}
	
	// TODO: ecrire la méthode !
	public int request(String method, int i1, int i2) {
		return 0;
	}

}
