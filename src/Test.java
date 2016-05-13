import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Test {


	public static void main(String[] args) {
		
		int workerNodeId = 0;
		String cmd = "nova boot --flavor m1.small --image myUbuntuIsAmazing"
				+ " --nic net-id=c1445469-4640-4c5a-ad86-9c0cb6650cca --security-group myRuleIsAmazing"
				+ " --key-name myKeyIsAmazing myUbuntuIsAmazing" + workerNodeId;

		executeProcess(cmd);

		cmd = "nova list | grep myUbuntuIsAmazing" + workerNodeId;

		System.out.println("Spawning VM...");
		
		while (!executeProcess(cmd).contains("Running")) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("VM OK...");

		cmd = "neutron floatingip-create public | grep floating_ip_address";

		String result = executeProcess(cmd);
		
		String ip = result.split("\\|")[2].trim();
		System.out.println("ip:" + ip);

		cmd = "nova floating-ip-associate myUbuntuIsAmazing" + workerNodeId + " " + ip;

		executeProcess(cmd);
		
		System.out.println("Waiting ssh...");
		
		cmd = "ssh ubuntu@" + ip + " 'nohup java -jar Server.jar 19020 >/dev/null 2>/dev/null &'";
		
		while (executeProcessReturnCode(cmd) != 0) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("ssh OK...");
	}
	
	public static String executeProcess(String cmd) {
		ProcessBuilder process = new ProcessBuilder("/bin/sh", "-c", cmd);
		
		Process p;
		StringBuilder sb = new StringBuilder();
		try {
			p = process.start();
			
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			InputStream is = p.getInputStream(); 
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String ligne;

			while (( ligne = br.readLine()) != null) { 
				sb.append(ligne).append(System.lineSeparator());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
	public static int executeProcessReturnCode(String cmd) {
		ProcessBuilder process = new ProcessBuilder("/bin/sh", "-c", cmd);

		Process p = null;
		try {
			p = process.start();

			try {
				p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return p.exitValue();
	}

}
