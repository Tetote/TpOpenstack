package repartiteur;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

public class WorkerNode {
	
	private int id;
	private String ip;
	
	public WorkerNode(int id, String ip) {
		this.id = id;
		this.ip = ip;
	}

	public String getIp() {
		return ip;
	}
	
	public int getId() {
		return id;
	}
	
	public XmlRpcClient createConfiguration() {
		// create configuration
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL("http://"+this.ip+":19020/xmlrpc"));
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
		return client;
	}	

}
