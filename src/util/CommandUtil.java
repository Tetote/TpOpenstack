package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CommandUtil {

	public static String executeProcess(String cmd) {
		ProcessBuilder process = new ProcessBuilder("/bin/sh", "-c", cmd);

		Process p;
		StringBuilder sb = new StringBuilder();
		try {
			p = process.start();

			try {
				p.waitFor();
			} catch (InterruptedException e) {
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
