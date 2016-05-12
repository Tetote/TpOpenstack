package client;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

public class Update_Client {

	public static void main(String[] args) {

		int requestRate = 15;
		String host = "127.0.0.1";
		int port = 19000;

		if (args.length == 3) {
			requestRate = Integer.parseInt(args[1]);
			host = args[2];
			port = Integer.parseInt(args[3]);
		} else {
			System.out.println("Usage: Update_Client nbReq host port");
		}

		// create configuration
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL("http://"+host+":"+port+"/xmlrpc"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		config.setEnabledForExtensions(true);
		config.setConnectionTimeout(60 * 1000);
		config.setReplyTimeout(60 * 1000);

		XmlRpcClient client = new XmlRpcClient();

		// use Commons HttpClient as transport
		client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));
		// set configuration
		client.setConfig(config);

		Object[] params = new Object[]
				{ new Integer(requestRate) };
		Boolean result = false;
		try {
			result = (Boolean) client.execute("Client.updateRate", params);
		} catch (XmlRpcException e) {
			e.printStackTrace();
		}

		if (result) {
			System.out.println("Client updated");
		} else {
			System.out.println("Client not updated!");
		}
	}
}
