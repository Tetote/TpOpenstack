package repartiteur;

import org.apache.xmlrpc.client.XmlRpcClient;

public class WorkerNode {

	private String id;
	private String ip;
	private XmlRpcClient client;

	public WorkerNode(String id, String ip, XmlRpcClient client) {
		this.id = id;
		this.ip = ip;
		this.client = client;
	}

	public String getIp() {
		return ip;
	}

	public String getId() {
		return id;
	}

	public XmlRpcClient getClient() {
		return client;
	}

}
