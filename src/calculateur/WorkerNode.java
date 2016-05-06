package calculateur;

public class WorkerNode {
	
	private String ip;
	private String port;
	
	public WorkerNode(String ip, String port) {
		super();
		this.ip = ip;
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public String getPort() {
		return port;
	}

}
