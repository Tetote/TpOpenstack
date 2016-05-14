package repartiteur;

import org.apache.xmlrpc.client.XmlRpcClient;

public class WorkerNode {
	
	private int id;
	private String ip;
	private XmlRpcClient client;
	
	public WorkerNode(int id, String ip, XmlRpcClient client) {
		this.id = id;
		this.ip = ip;
		this.client = client;
	}

	public String getIp() {
		return ip;
	}
	
	public int getId() {
		return id;
	}
	
	public XmlRpcClient getClient() {
		return client;
	}

}
