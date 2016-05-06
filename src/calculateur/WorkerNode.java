package calculateur;

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

}
