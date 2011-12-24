package snow;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Worker extends Thread {
	public Process process;
	Integer exit;
	ArrayList<String> output = new ArrayList<String>();
	BufferedReader d;
	
	Worker(Process process) 
	{
		this.process = process;
		DataInputStream in = new DataInputStream( process.getInputStream()); 
		d = new BufferedReader(new InputStreamReader(in));
	}
	
	public void run() 
	{
		try { 
			String readLine;
			while ((readLine = d.readLine()) != null) { 
				//should look like request id is postcard-96
				output.add( readLine );
				start.logger.debug(readLine); 
			} 
			exit = process.waitFor();
		} catch (InterruptedException ignore) {
			start.logger.warn( "timeout while running external command");
			return;
		} catch (IOException e) 
		{ 
			start.logger.warn( "IO error while running external command: " + e.toString());
			return;
		} 
	}  
}