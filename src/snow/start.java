package snow;
/*
 * todo:
 *
 * 	must fix:
 * 
 *  would be cool to fix:
 *  
 *  catch printer errors - detect paper shortage/ink levels
 *  exp with other constansts?
 *  
 *  done:
 *  
 *  remove 'control jumping'
 *  sometimes print preview still has control bars.
 *  fix multi button presses
 *  fix mouse control doesn't update properly
 *  logging
 *  serial port usb1 or 2?
 *  resolution issue with smaller pixel size?
 *  better looking messaging, fades
 *  better looking control bars
 *  messaging responds to user input in real time!
 *  fix control bars on print
 *  fix problems with initialising
 *  cycle
 *  (very basic): instruction messages
 *  check postcards still right size, assertion on print padding
 *  separate knob updates
 *  fix up humidity changing
 *  fix up print image adjustment (diff mass)
 *  add scales for temp and humidity
 *  how to print?
 *  sort decent image saving
 *  fix bounding error 
 *  fix flicker
 *  
 */


import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.net.SocketAppender;

public class start {

	static final Logger logger = Logger.getLogger(snow.class);

	static boolean useSerial = false;
	static String serialDevice;
	static boolean debug = false;
	static Level debugLevel = Level.INFO;

	static float cellSize;
	static int width;
	static int height;
	static int startButton = 1;
	static int printButton = 2;

	static boolean debugText = true;
	static boolean mirroring =true; //true;
	static int debugIterations = 10;
	static int maxIterations = 20000;
	static String baseDir;
	static String filename = baseDir + "state.xml";
	static String imageDir = baseDir + "images/";

	static Integer numSnowFlakes = 0;
	static Properties properties = new Properties();
	//some numbers cock up the mirroring calcs and run out of time to fix.
	static boolean finished = false;
	static boolean printing = false;
	public static void main(String args[]) 
	{
		OptionParser parser = new OptionParser() {
			{
				/*accepts( "c" ).withRequiredArg().ofType( Integer.class )
				.describedAs( "count" ).defaultsTo( 1 );
				accepts( "q" ).withOptionalArg().ofType( Double.class )
				.describedAs( "quantity" );*/
				accepts( "serial" ).withRequiredArg().describedAs("file" );
				accepts( "remotelog" );
				accepts( "basedir" ).withOptionalArg().describedAs( "file" );
			}
		};

		OptionSet options = parser.parse( args );
		if( options.has( "basedir" ) )
		{
			baseDir = (String)options.valueOf( "basedir" );
		}
		else
		{
			baseDir = "/home/matthew/work/artistengineer/scienceofsnow/";
		}
		if( options.has( "serial" ))
		{
			useSerial = true;
			serialDevice = (String)options.valueOf( "serial" );
		}
		filename = baseDir + "state.xml";
		imageDir = baseDir + "images/";
		WriterAppender fileAppender = null;
		String pattern = "%d{ISO8601} %-5p [%t]: %m%n";
		PatternLayout layout = new PatternLayout(pattern);
		ConsoleAppender consoleAppender = new ConsoleAppender(layout);
		try 
		{
			FileOutputStream output = new FileOutputStream(baseDir + "/snow.log", true);
			fileAppender = new WriterAppender(layout,output);
		} 
		catch(Exception e) 
		{
			System.out.println( "couldn't open log file: " + e.toString());
			System.exit(1);
		}

		logger.addAppender(consoleAppender);
		logger.addAppender(fileAppender);
		logger.setLevel(debugLevel);
		logger.info( "starting snowflake generator");
		if( options.has( "remotelog" ))
		{
			logger.info( "enabling remote logging");
			SocketAppender socketAppender = new SocketAppender("mattvenn.net", 50000);
			logger.addAppender( socketAppender);
		}

		loadState();
		if( debug )
		{
			cellSize = (float)40.0;
			width = 13;
			height = 13;
		}
		else
		{
			cellSize = (float)1.7;
			width = 403; //403; //403x403 at 1.7pix is good for 800x600
			height = 403; //403; 
		}

		int[] pixels = new int[16 * 16];
		Image transimage = Toolkit.getDefaultToolkit().createImage(
				new MemoryImageSource(16, 16, pixels, 0, 16));
		Cursor transparentCursor =
			Toolkit.getDefaultToolkit().createCustomCursor
			(transimage, new Point(0, 0), "invisibleCursor");

		snow snowInst = new snow(width,height,cellSize);
		snowInst.setCursor( transparentCursor );
		Point frameSize = snow.getWidthHeight();
		int frameWidth = (int)frameSize.getX();
		int frameHeight = (int)frameSize.getY();
		logger.debug( String.format( "%d x %d", frameWidth, frameHeight));

		JFrame frame = new JFrame("JFrame Source Demo");
		frame.setUndecorated(true);

		frame.getContentPane().add(snowInst);

		frame.setSize( frameWidth,frameHeight);
		frame.setVisible(true);
		boolean startMessage = true;
		boolean restart = false;

		while( true )
		{
			finished = false;
			restart = false;
			snowInst.initialise();
			snowInst.placeSeed();
			snowInst.calculate();
			snowInst.forceUpdateControls();
			snowInst.buttonLED( printButton, false );
			if( startMessage )
			{
				snowInst.buttonLED( startButton, true);
				snowInst.setMessage( "the science of snow\npress start", 0, true, false);
				snowInst.setMessage( "you can adjust humidity and temperature\nof the cloud as it grows", 4, false, false);
			}
			logger.info( "starting grow: " + numSnowFlakes );
			double startTime = 	System.currentTimeMillis();
			//this only seems to work the first time starting.
			while(debug || snowInst.roomToGrow())
			{
				snowInst.calculate();
				snowInst.updateControls();
				snowInst.repaint();
				if( snowInst.startButton() )
				{
					restart = true;
					startMessage = false;
					logger.info( "restarted" );
					break;
				}
			}
			if( ! restart )
			{

				double duration = System.currentTimeMillis() - startTime;
				logger.info( "finished in: " + duration / 1000 + "s" );
				finished = true;
				snowInst.repaint();
				//	snowInst.adjustDraw(g,0.3f,0.5f);
				snowInst.buttonLED( startButton, false );
				try {
					Thread.sleep( 3000 );
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				snowInst.buttonLED( startButton, true );
				snowInst.buttonLED( printButton, true );
				String button = snowInst.setMessage( "press print to make a card (£1),\nor press start to make another", 20 , true, true);
				if( button == "" )
					startMessage = true;
				else if( button == "start" )
					startMessage = false;
				else if( button == "print" )
				{	
				
					startMessage = true;
					/*
				snowInst.setMessage( "press start to adjust the image,\npress print when you're finished", 0 , true);
				//adjust the colours
				while( true )
				{
					if( snowInst.knob1Changed() || snowInst.knob2Changed() )
					{
						snowInst.repaint();
						//snowInst.updateControls();
					}
					if( snowInst.printButton())
					{
						if( ! debug )
						{
					 */
					BufferedImage printImage = snowInst.paintPrint();
					String imageName = writeImage( printImage );

					Printit print = new Printit();
					
					snowInst.buttonLED( printButton, true );
					snowInst.setMessage( "is there paper in the printer?\npress print", 0, false, true);
					if( print.printsWaiting() > 0 )
					{
						snowInst.setMessage( "is the printer's orange button lit?\npress the orange button and then press print", 0, false, true);
						snowInst.buttonLED( printButton, false );
					}
					snowInst.buttonLED( startButton, false );
					snowInst.buttonLED( printButton, false );
				
					//check ink levels
					if( numSnowFlakes % 10 == 0 )
						print.inkLevels();
					logger.info( "prints waiting: " + print.printsWaiting() );
					if( print.printsWaiting() > 1)
						print.cancelPrints();
					print.print(imageName);

					//state stuff
					numSnowFlakes ++;
					saveState();

					snowInst.setMessage( "printing postcard", 2, false, false );
					/*
						}
						break;
					}
				}
					 */
				}
			}
		}
		//System.out.println( "finished" );    
		//closes serial ports
		//snowInst.finish();
		//System.exit(0);
	}



	static String writeImage( BufferedImage image )
	{
		String imageFile = String.format( "%s%06d.png", imageDir, numSnowFlakes );
		try {
			logger.debug( "saving image to " + imageFile );
			File outputfile = new File(imageFile);
			ImageIO.write(image, "png", outputfile);
			return imageFile;
		} catch (IOException e) {
			//should do something else here so we know theres a problem
			logger.error( "problem writing image to imageFile: " + e );
		}	
		return imageFile;
	}

	static void loadState()
	{
		try 
		{
			properties.loadFromXML(new FileInputStream(filename));
			numSnowFlakes = Integer.parseInt( properties.getProperty( "numSnowFlakes" ) );
		}
		catch (IOException e) 
		{
			logger.error( "failed to load state from " + filename + ":" + e );
			System.exit( 1 );
		}
	}
	static void saveState()
	{
		properties.put( "numSnowFlakes",numSnowFlakes.toString());
		try
		{
			FileOutputStream propertiesFile = new FileOutputStream(filename);
			properties.storeToXML( propertiesFile, null );
		}
		catch( IOException e )
		{
			logger.error( "failed to store state to " + filename + ":" + e );
			System.exit( 1 );
		}
	}


}

