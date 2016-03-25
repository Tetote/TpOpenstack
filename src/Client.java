
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.apache.xmlrpc.client.util.ClientFactory;
//  import org.apache.xmlrpc.demo.proxy.Adder;

public class Client {
	public static void main(String[] args) throws Exception {

		int nbRequete = 18;
		String nodeLocation = "127.0.0.1";
		int port = 8080;

		if (args.length == 3) {
			nbRequete = Integer.parseInt(args[0]);
			nodeLocation = args[1];
			port = Integer.parseInt(args[2]);
		}

		// create configuration
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL("http://"+nodeLocation+":"+port+"/xmlrpc"));
		config.setEnabledForExtensions(true);  
		config.setConnectionTimeout(60 * 1000);
		config.setReplyTimeout(60 * 1000);

		XmlRpcClient client = new XmlRpcClient();

		// use Commons HttpClient as transport
		client.setTransportFactory(
				new XmlRpcCommonsTransportFactory(client));
		// set configuration
		client.setConfig(config);

		// make the a regular call
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				Object[] params = new Object[]
						{ new Integer(2), new Integer(3) };
				Integer result;
				try {
					result = (Integer) client.execute("Calculator.add", params);
					System.out.println("2 + 3 = " + result);
				} catch (XmlRpcException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, 0, 1000/nbRequete);

		// make a call using dynamic proxy
		/*          ClientFactory factory = new ClientFactory(client);
          Adder adder = (Adder) factory.newInstance(Adder.class);
          int sum = adder.add(2, 4);
          System.out.println("2 + 4 = " + sum);
		 */
	}
}