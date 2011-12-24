package snow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Printit {
	/* to get ink levels run this:
	 * sudo escputil  -i -r /dev/usb/lp0  -q
	 */

	void print( String imageName )
	{
		start.logger.info( "printing: " + imageName );
		// -d destination (named printer) 
		runCommand( "lp -d postcard2  -o landscape -o media=Custom.105x148mm -o scaling=100 " + imageName );
	}

	void inkLevels()
	{
		start.logger.info( "ink status:" );
		ArrayList<String> output = runCommand( "sudo escputil  -i -r /dev/usb/lp0 -q" );
		Pattern pattern = Pattern.compile("\\s+(\\w+|\\w+ \\w+)\\s+(\\d+).*" );
		if( output.size() != 7 )
		{
			start.logger.warn( "couldn't read ink status" );
			return;
		}

		for( int colour = 1; colour < 7; colour ++ )
		{
			Matcher matcher = pattern.matcher( output.get( colour ) );
			if( ! matcher.matches() )
				continue;
			String colourStr = matcher.group(1);
			String percentStr = matcher.group(2);
			int percent = Integer.parseInt(percentStr.trim());
			String logMsg = String.format( "%s : %d%%", colourStr, percent );
			if( percent < 10 )
				start.logger.warn( logMsg );
			else
				start.logger.info( logMsg );
		}
	}

	int printsWaiting()
	{
		ArrayList<String> output = runCommand( "lpstat -o postcard" );
		return output.size(); 
	}
	void cancelPrints()
	{
		start.logger.warn( "cancelling " + printsWaiting() + " prints" );
		runCommand( "sudo cancel -a postcard");
	}

	ArrayList<String> runCommand(String command) 
	{
		ArrayList<String> output = new ArrayList<String>();
		int timeout = 5000;
		try { 
			Runtime runtime = Runtime.getRuntime();
			Process process = runtime.exec(command);
			Worker worker = new Worker(process);
			worker.start();
			try {
				worker.join(timeout);
				if (worker.exit != null)
					output = worker.output;
				else
					start.logger.warn( "timeout while executing external command" );
			} catch(InterruptedException ex) {
				worker.interrupt();
				Thread.currentThread().interrupt();
			} finally {
				process.destroy();
			}
		} catch (IOException e1) { 
			start.logger.error( "error running external command: " + e1.toString());
		}
		return output;
				  
	}

}
