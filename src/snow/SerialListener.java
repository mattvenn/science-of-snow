package snow;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.TooManyListenersException;

public class SerialListener implements SerialPortEventListener{
	static SerialPort serialPort;
	static OutputStream out = null;
	static InputStream in = null;
	private static float var1 = 0;
	private boolean started = false;
	private boolean button1 = false;
	private boolean button2 = false;
	private boolean paper = false;
	private static float var2 = 0;
	private static boolean knob1Changed = false;
	private static boolean knob2Changed = false;
	BufferedReader bufferedReader;
	BufferedWriter bufferedWriter;
	public float getKnob1()
	{
		knob1Changed = false;
		return 1 - var1 / 1024;
	}
	public float getKnob2()
	{
		knob2Changed = false;
		return 1 - var2 / 1024;
	}

	public void buttonLED(int button, boolean state)
	{
		String message = "b";
		message += String.valueOf(button);
		message += state ? "1" : "0";
		start.logger.debug( "sending LED message: " + message.toString());

		try
		{
			out.write( message.getBytes());
			out.flush();
			//			Thread.sleep( 10 );
		}
		catch (Exception e) 
		{
			// TODO: handle exception
		}
	}
	public void clearButtons()
	{
		button1 = false;
		button2 = false;
	}
	public boolean getButton1()
	{
		if( button1 )
		{
			button1 = false;
			return true;
		}
		return false;
	}
	public boolean getButton2()
	{
		if( button2 )
		{
			button2 = false;
			return true;
		}
		return false;
	}

	public boolean changed()
	{
		return knob1Changed || knob2Changed || button1 || button2;
	}
	public boolean knob1Changed()
	{
		return knob1Changed;
	}
	public boolean knob2Changed()
	{
		return knob2Changed;
	}
	public boolean paper()
	{
		return paper;
	}
	public SerialListener( )
	{
		start.logger.info( "opening serial" );
		openSerial();

		try {
			// Add the serial port event listener
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (TooManyListenersException ex) {
			System.err.println(ex.getMessage());
		}
		bufferedReader = new BufferedReader(new InputStreamReader(in));
		bufferedWriter = new BufferedWriter(new OutputStreamWriter(out));
		while( started == false )
		{
			try
			{
				Thread.sleep( 100 );
			}
			catch( InterruptedException e )
			{}
		}
		start.logger.info( "serial setup complete" );
	}
	public void serialEvent(SerialPortEvent event) {
		switch (event.getEventType()) {
		case SerialPortEvent.DATA_AVAILABLE:
			//System.out.println( "got serial event" );
			readSerial();
			break;
		}
	}

	private void readSerial() 
	{
		try
		{
			String msg = bufferedReader.readLine();
			if( msg.startsWith("k1"))
			{
				String var = msg.substring(2, msg.length());
				var1 = Integer.parseInt(var);
				knob1Changed = true;
			}
			else if( msg.startsWith( "k2" ))
			{
				String var = msg.substring(2, msg.length());
				var2 = Integer.parseInt(var);
				knob2Changed = true;
			}
			else if( msg.startsWith( "b1" ))
			{
				button1 = true;
			}
			else if( msg.startsWith( "b2" ))
			{
				button2 = true;
			}
			else if( msg.startsWith( "started" ))
			{
				started = true;
			}
			else if( msg.startsWith( "got: b" ))
			{
				//button OK
				start.logger.debug( "LED message received OK" );
			}
			else if( msg.startsWith( "paper" ))
			{
				if( msg.contains( "0" ))
				{
					paper = false;
					start.logger.warn( "out of paper");
				}
				else
				{
					paper = true;
					start.logger.warn( "paper refilled");
				}
			}
			else
			{
				throw new RuntimeException( "didn't understand msg: " + msg );
			}
			//		System.out.println( "current vars" );

		} catch (IOException e)
		{
			start.logger.warn( "IO exception" + e );
		}
		catch( RuntimeException e )
		{
			start.logger.warn( "bad msg: " + e );
		}
		start.logger.debug( String.format( "k1: %f k2: %f b1: %s", var1, var2, button1 ? "1" : "0" ));
	}
	public void openSerial()
	{
		try
		{
			CommPortIdentifier portId = null;
			System.setProperty("gnu.io.rxtx.SerialPorts", start.serialDevice);

			try
			{
				portId = CommPortIdentifier.getPortIdentifier(start.serialDevice);
			}
			catch( NoSuchPortException e )
			{
				start.logger.error( "couldn't open serial device: " + start.serialDevice );
				start.logger.error( e.toString() );
				System.exit(1 );
			}

			serialPort = (SerialPort) portId.open("snowflake controller", 5000);
			int baudRate = 115200; // 57600; // 57600bps
			// Set serial port to 57600bps-8N1..my favourite
			serialPort.setSerialPortParams(
					baudRate,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			//			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			try
			{
				serialPort.setDTR(false);
				Thread.sleep( 100 );
				serialPort.setDTR(true);
			}
			catch( InterruptedException e )
			{}
			out = serialPort.getOutputStream();
			in = serialPort.getInputStream();

		} catch (UnsupportedCommOperationException ex) {
			System.err.println(ex.getMessage());
		} catch (PortInUseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void closeSerial()
	{
		if (serialPort != null) {
			try {
				// close the i/o streams.
				out.close();
				in.close();
			} catch (IOException ex) {
				// don't care
			}
			// Close the port.
			serialPort.close();

		}
	}
}
