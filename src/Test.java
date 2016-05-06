import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Test {


	public static void main(String[] args) {
		
		int workerNodeId = 0;
		String cmd = "nova boot --flavor m1.small --image myUbuntuIsAmazing"
				+ " --nic net-id=c1445469-4640-4c5a-ad86-9c0cb6650cca --security-group default"
				+ " --key-name myKeyIsAmazing myUbuntuIsAmazing" + workerNodeId;
		
		executeProcess(cmd);
		
		cmd = "nova list | grep myUbuntuIsAmazing" + workerNodeId;
		
		while (!executeProcess(cmd).contains("Running")) {};
		
		cmd = "neutron floatingip-create public| grep floating_ip_address";
		
		String result = executeProcess(cmd);
		
		System.out.println(result);
		
		String[] array = result.split("\\|");
		
		for (int i = 0; i < array.length; i++) {
			System.out.println(i+ ": " +array[i]);
		}
		
		String ip = array[2].trim();
		System.out.println("ip:" + ip + "|");
		
		cmd = "nova floating-ip-associate myUbuntuIsAmazing" + workerNodeId + " " + ip;
		
		System.out.println(executeProcess(cmd));
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

}
