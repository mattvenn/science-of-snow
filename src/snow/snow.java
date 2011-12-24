package snow;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JPanel;


public class snow extends JPanel { 

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Font font = new Font("Dialog", Font.PLAIN, 10);
	Font messageFont = new Font("Dialog", Font.PLAIN, 25);
	static BufferedImage hexFrameWhite;
	static BufferedImage hexFrameBlack;
	static BufferedImage messageBackdrop;
	// constants
	static float b = (float) 1.3; // keep above 1
	static float alpha = (float) 0.08;
	static float theta = (float) 0.025;
	static float k = (float) 0.003;
	static float u = (float) 0.07;
	static float y = (float) 0.00005;

	static int printed = 0;
	int iterations = 0;    
	float oldDiffusionMass = (float)0.5;
	float newDiffusionMass = 0;
	float humidityDisplay = 0;
	float temperatureDisplay = 0;

	int snowFlakeWidth = 0;
	int maxSnowFlakeWidth;
	static int Xcells; 
	static int Ycells;

	Cell[][] snowArray;
	ArrayList<Cell> realCells;
	static double cellWidth;
	static double cellHeight;
	static int controlBarWidth = 40;
	static int printWidth;
	static int printPadding;
	static int realFrameWidth;
	static int frameWidth, frameHeight;
	private static String message;
	float boundingCircleRadius;

	boolean mouseButtonL = false;
	boolean mouseButtonR = false;
	boolean mouseChanged = false;
	int newX = 0;
	int newY = 0;
	int oldX, oldY = 0;
	SerialListener serialListener;
	private boolean showMessage;
	private boolean	initialising = true;
	private long messageStartTime;

	/*
	 * constructor
	 */
	public snow(int Xcells, int Ycells, float cellWidth) {
		snow.cellWidth = cellWidth;
		snow.Xcells = Xcells;
		snow.Ycells = Ycells;
		maxSnowFlakeWidth = (int)(( Xcells * 0.9)/2);
		if (start.useSerial)
		{
			serialListener = new SerialListener();
		}
		else {
			MouseAdapter mouse = (new MouseAdapter() {

				public void mousePressed(MouseEvent e) {
					if (e.getButton() == 1)
						mouseButtonR = true;
					if (e.getButton() == 3)
						mouseButtonL = true;
					mouseChanged = true;
					start.logger.debug( "button detected: " + mouseChanged );
				}

				public void mouseMoved(MouseEvent e) {
					mouseChanged = true;
					oldX = newX;
					oldY = newY;
					newX = e.getX();
					newY = e.getY();
					if ((oldX != newX || oldY != newY) && start.debug)
						showCellStatus();
				}

			});
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
		}

		cellHeight = (cellWidth / 2) * Math.tan(Math.PI / 3);
		frameWidth = (int) (Xcells * cellWidth);
		frameHeight = (int) (Ycells * cellHeight);
		boundingCircleRadius = (float) 0.99 / 2 * Xcells;

		//setup message backdrop
		BufferedImage img = null;
		try {
			img = ImageIO.read(new File(start.baseDir + "messageBackdrop.jpg"));
		} catch (IOException e) {
		}
		messageBackdrop = new BufferedImage(frameWidth + 2 * controlBarWidth, frameHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics g = messageBackdrop.getGraphics();
		g.drawImage(img, controlBarWidth, 0, frameWidth, frameHeight, null);

		//print setup
		printWidth = (int)(frameHeight * 1.4095);
		realFrameWidth = frameWidth + 2*controlBarWidth;
		if( realFrameWidth > printWidth )
		{
			start.logger.error( "frameWidth too large: check control bars");
			System.exit(1);
		}
		printPadding = (printWidth-(realFrameWidth))/2;
		start.logger.debug( "padding print with " + (printWidth-frameWidth)/2 + "pix" );

		//hex frame setup
		makeHexFrame();

		//		initialise();
	}

	public void initialise( )
	{
		start.logger.debug( "initialising...");
		initialising = true;
		//fix me!
		mouseButtonL = false;
		mouseButtonR = false;

		realCells = new ArrayList<Cell>();
		snowArray = new Cell[Xcells][Ycells];

		//		System.out.println("init snowcell array");
		for (int i = 0; i < Xcells; i++) {
			for (int j = 0; j < Ycells; j++) {
				snowArray[i][j] = new snowcell(i, j, Xcells, Ycells, snowArray);
			}
		}


		for (int i = 0; i < Xcells; i++)
		{
			for (int j = 0; j < Ycells; j++) {
				Double xy = getXYFromIndex(i, j);
				Double polar = getPolarFromXY(xy.x, xy.y);
				if ((i == Xcells / 2) && (j == Ycells / 2)) {
				} else if (polar.x >= -Math.PI && polar.x <= 2 * Math.PI / 3) // 1.15
				{
					snowArray[i][j] = new Fakecell(i, j, Xcells, Ycells,
							snowArray);
				}
				// only do calcs for those cells that are real, and are within a
				// certain distance.
				if (!snowArray[i][j].isFake()) {
					if (polar.y < boundingCircleRadius)
						realCells.add(snowArray[i][j]);
					else if (polar.y <= Xcells / 2) {
						snowArray[i][j] = new EdgeCell(i, j, Xcells, Ycells,
								snowArray);
						realCells.add(snowArray[i][j]);
					}
				}
			}
		}

		//	System.out.println("getting neighbours");
		for (int i = 0; i < Xcells; i++) {
			for (int j = 0; j < Ycells; j++) {
				snowArray[i][j].getNeighbours();
			}
		}

		for (int i = 0; i < Xcells; i++) {
			for (int j = 0; j < Ycells; j++) {
				snowArray[i][j].updateNeighbours();
			}
		}
		
		//initialise snowflakeWidth
		roomToGrow();

		initialising = false;
		//	System.out.println(String.format("%d cells, %d real cells", Xcells * Ycells, realCells.size()));
	}

	public void messageSetup()
	{

		for (Cell cell : realCells) {
			cell.setDiffusionMass( (float)0.0 );
			if( cell.getPolar().getY() < Xcells /6 )
				cell.setDiffusionMass( (float)1 );
		}

	}
	public void placeSeed()
	{
		// seed
		snowArray[Xcells / 2][Ycells / 2].setState();
	}
	/*
	 * main part, runs the cell calculations
	 */
	public void calculate() {
		iterations++;
		for (Cell cell : realCells) {
			cell.doDiffusion();
		}
		for (Cell cell : realCells) {
			cell.doFreezing();
			cell.doAttachment();
			cell.doMelting();
			cell.updateState();
		}
		for (Cell cell : realCells) {
			cell.updateNeighbours();
		}
	}

	/*
	 * drawing stuff *******************************************************
	 */

	/*
	 * actually draw it
	 */
	void drawIt(Graphics2D g2d)
	{
		int calcOffset= (int)Math.round (cellWidth / 2);
		int calcCellWidth =	(int)Math.ceil( cellWidth );
		int calcCellHeight = (int)Math.ceil( cellHeight );
		for (int y = 0; y < Ycells; y++) {
			for (int x = 0; x < Xcells; x++) {
				Cell cell = snowArray[x][y];
				//don't draw any cells beyond the edge
				try
				{
					if (cell.getPolar().getY() > boundingCircleRadius)
						continue;
				} catch ( NullPointerException e )
				{
					start.logger.warn( "init: " + initialising );
				}

				float colour = 0;

				if (cell.getState()) {
					// return;
					colour = 200 * cell.getCrystalMass();
				} else {
					// if( ! start.finished )
					colour = 200 * cell.getDiffusionMass();
				}

				int intC = (int) colour;
				if (intC > 255)
					intC = 255;

				// if( start.finished )
				// intC = 255 - intC;
				Color color = new Color(intC, intC, intC);
				g2d.setPaint(color);
				int offset = 0;
				if (y % 2 == 0)
					offset = calcOffset;
				int xPix = (int)Math.round (x * cellWidth + offset);
				xPix += controlBarWidth;
				int yPix = (int) (y * cellHeight);
				g2d.fillRect(xPix, yPix, calcCellWidth, calcCellHeight );
			}
		}

	}

	/*
	 * what we run after the calculations are done
	 */
	public void adjustDraw(Graphics2D g2d,float knob1, float knob2) {
		//		float knob1 = getKnob1();
		//	float knob2 = getKnob2();
		int calcOffset= (int)Math.round (cellWidth / 2);
		int calcCellWidth =	(int)Math.ceil( cellWidth );
		int calcCellHeight = (int)Math.ceil( cellHeight );
		for (int y = 0; y < Ycells; y++) {
			for (int x = 0; x < Xcells; x++) {
				Cell cell = snowArray[x][y];
				float colour = 255;
				if (cell.getState()) {
					// return;
					colour = (float)(knob1 + 0.35) * 300 * cell.getCrystalMass();
				} else {
					// if( ! start.finished )
					colour = (float)((knob2 + 0.5) * 4000 * cell.getDiffusionMass());

				}
				int intC = (int) colour;
				if (intC > 255) {
					intC = 255;
				}
				if (intC < 0) {
					intC = 0;
				}

				Color color = new Color(intC, intC, intC);
				g2d.setPaint(color);
				int offset = 0;
				if (y % 2 == 0)
					offset = calcOffset;
				int xPix = (int)Math.round(x * cellWidth + offset);
				xPix += controlBarWidth;
				int yPix = (int)Math.round(y * cellHeight);
				g2d.fillRect(xPix, yPix, calcCellWidth, calcCellHeight );
			}
		}
	}

	public BufferedImage paintPrint()
	{
	//	int realFrameWidth = frameWidth + 2 * controlBarWidth;
		BufferedImage image = new BufferedImage(realFrameWidth,frameHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(255,255,255));
		g.fillRect(0,0, realFrameWidth, frameHeight);
		//force the values to what we want.
		adjustDraw(g,0.3f,0.5f);
		g.setComposite(AlphaComposite.SrcOver);
		g.drawImage(hexFrameWhite, controlBarWidth, 0, null);
		g = image.createGraphics();

		BufferedImage printImage = new BufferedImage(printWidth,frameHeight, BufferedImage.TYPE_INT_RGB);
		g = printImage.createGraphics();
		//make the whole print white
		g.setColor(new Color(255,255,255));
		g.fillRect(0,0, printWidth, frameHeight);
		//draw on the print iamge
		g.drawImage(image, printPadding,  0, null);
		return printImage;

	}
	public void paintComponent(Graphics g)
	{
		if( initialising )
			return;
		Graphics2D g2d = (Graphics2D) g;
		try
		{
		if( showMessage )
		{
			printMessage(g2d);
			g2d.setComposite(AlphaComposite.SrcOver);
			g2d.drawImage(hexFrameBlack, controlBarWidth, 0, null);
		}
		else if (start.finished) {
			blankScreen( g2d, new Color( 255,255,255 ));
			adjustDraw(g2d,0.3f,0.5f);
//			adjustDraw(g2d,getKnob1(), getKnob2());
			g2d.setComposite(AlphaComposite.SrcOver);
			g2d.drawImage(hexFrameWhite, controlBarWidth, 0, null);
		} else if (iterations % 2 == 0) {
			drawIt(g2d);
			g2d.setComposite(AlphaComposite.SrcOver);
			if (!start.debug)
				g2d.drawImage(hexFrameBlack, controlBarWidth, 0, null);
			drawControlBars(g2d);
		}
		}
		catch( NullPointerException e )
		{
			//sometimes paint gets called while we're initialising?
			//can't seem to stop it.
			start.logger.warn( "null pointer while drawing... FIXME!");
		}
	}

	/* 
	 * message utils
	 */
	public String setMessage( String message, int sleepTime, boolean startButtonBreak, boolean printButtonBreak )
	{
		//		saveImage = true;
		//	fadeOut( currentFrame );
		
		//remove any historic button presses
		clearButtons();
		
		showMessage = true;
		snow.message = message;
		String button = "";

		double endTime = System.currentTimeMillis() + sleepTime * 1000;
		messageStartTime = System.currentTimeMillis();
		while( true )
		{
			if( startButton() && startButtonBreak )
			{
				button = "start";
				break;
			}
			if( printButton() && printButtonBreak )
			{
				button = "print";
				break;
			}
			if( sleepTime != 0 && System.currentTimeMillis() > endTime)
				break;
			if( getMessageOpacity() < 1 )
				repaint();
		}

		showMessage = false;
		return button;
	}

	private float getMessageOpacity()
	{

		double opacity = ( System.currentTimeMillis() - messageStartTime );
		opacity /= 1500;
		//		System.out.println( opacity );
		if( opacity > 1 )
			return 1;
		return (float)opacity;
	}
	public void printMessage( Graphics2D g2d )
	{
		g2d.setColor(new Color(0,0,0));
		g2d.fillRect(0,0, frameWidth + 2 * controlBarWidth, frameHeight);

		float[] scales = { 1f, 1f, 1f, getMessageOpacity() };
		float[] offsets = new float[4];
		RescaleOp rop = new RescaleOp(scales, offsets, null);
		g2d.drawImage( messageBackdrop, rop, 0, 0 );
		g2d.setFont(messageFont);
		Color textcolor = new Color(0, 0, 0);
		g2d.setPaint(textcolor);
		//fix this controlBarWidth stuff
		String[] messages = snow.message.split("\n");
		double startY = ( messages.length / 2 ) ;
		for( int i =0 ; i < messages.length; i ++ )
		{
			drawCenteredString( messages[i], frameWidth + 2* controlBarWidth, frameHeight, startY -- , g2d);
		}
	}

	public void drawCenteredString(String s, int w, int h, double offset, Graphics g) {
		offset *= 2.5;
		FontMetrics fm = g.getFontMetrics();
		int x = (w - fm.stringWidth(s)) / 2;
		int fontHeight = fm.getAscent() - fm.getDescent();
		int y = (fm.getAscent() + (h - fontHeight) / 2);
		y -= fontHeight * offset;
		y -= fontHeight / 2;
		g.drawString(s, x, y);
	}	
	
	void blankScreen( Graphics g, Color color )
	{
		g.setColor(color);
		g.fillRect(0, 0, realFrameWidth, frameHeight);
	}
	
	public void drawControlBars(Graphics2D g2d)
	{
		//black rects
		/*
		g2d.setColor(new Color(255, 255, 255));
		g2d.fillRect(0, 0, controlBarWidth, frameHeight);
		if( ! start.finished )
			g2d.setColor(new Color(255, 0, 0));

		g2d.fillRect(frameWidth + controlBarWidth, 0, frameWidth+ controlBarWidth, frameHeight);
		//now the control bars:
		//humidt
		g2d.setColor(new Color(0, 0, 0));
		g2d.fillRect(0, 0, controlBarWidth, (int)(frameHeight * (1-getKnob1())));
		//temp
		g2d.fillRect(frameWidth + controlBarWidth, 0, frameWidth+ controlBarWidth, (int)(frameHeight * getKnob2()));
		 */
		//humidity
		g2d.setColor(new Color(getKnob1(),getKnob1(),getKnob1()));
		g2d.fillRect(0, 0, controlBarWidth, frameHeight);
		//temp
		if( ! start.finished )
			g2d.setColor(new Color(getKnob2(),0,1 - getKnob2()));
		else
			g2d.setColor(new Color(getKnob2(),getKnob2(),getKnob2()));
		g2d.fillRect(frameWidth + controlBarWidth, 0, frameWidth+ controlBarWidth, frameHeight);

	}

	/*
	 * utilities *******************************************************
	 */

	static void makeHexFrame() {
		hexFrameBlack = new BufferedImage(frameWidth, frameHeight,
				BufferedImage.TYPE_INT_ARGB);
		hexFrameWhite = new BufferedImage(frameWidth, frameHeight,
				BufferedImage.TYPE_INT_ARGB);
		// wrong place

		Graphics2D osg = hexFrameBlack.createGraphics();
		osg.setComposite(AlphaComposite.Src);
		osg.setColor(new Color(0, 0, 0));
		osg.fillRect(0, 0, frameWidth, frameHeight);
		Polygon hex = getHexagon(frameWidth / 2, frameHeight / 2,
				(int) (frameWidth * 0.99 / 2));
		osg.setColor(new Color(0, 0, 0, 0));
		osg.fillPolygon(hex);

		osg = hexFrameWhite.createGraphics();
		osg.setComposite(AlphaComposite.Src);
		osg.setColor(new Color(255, 255, 255));
		osg.fillRect(0, 0, frameWidth, frameHeight);
		osg.setColor(new Color(0, 0, 0, 0));
		osg.fillPolygon(hex);

	}

	static Polygon getHexagon(int x, int y, int h) {
		Polygon hexagon = new Polygon();

		for (int i = 0; i < 7; i++) {
			double hex = Math.PI / 6 + Math.PI / 3.0 * i;
			hexagon.addPoint((int) (Math.round(x + Math.sin(hex) * h)),
					(int) (Math.round(y + Math.cos(hex) * h)));
		}
		return hexagon;
	}


	/*
	 * controller stuff
	 */
	void clearButtons()
	{
		if( ! start.useSerial)
		{
			mouseButtonL = false;
			mouseButtonR = false;
		}
		else
		{
			serialListener.clearButtons();
		}
	}
	public boolean printButton() {
		if (start.useSerial) {
			if (serialListener.getButton2())
				return true;
		} else {
			if (mouseButtonL)
			{
				mouseButtonL = false;
				return true;
			}
		}
		return false;
	}

	public boolean startButton() {
		if( start.useSerial) {
			if( serialListener.getButton1())
				return true;
		}
		else
		{
			if( mouseButtonR)
			{
				mouseButtonR = false;
				return true;
			}
		}
		return false;
	}


	/*
	 * tidy up
	 */
	public void finish() {
		if (start.useSerial)
			serialListener.closeSerial();
	}

	/*
	 * how to know when to stop
	 */
	boolean roomToGrow() {

		//if (start.debug && iterations > start.debugIterations)
		//	return false;
		//if (iterations > start.maxIterations)
		//	return false;

		for( int i = Xcells / 2 ; i < Xcells ; i ++ )
		{
			Cell testCell = snowArray[i][Ycells / 2];
			snowFlakeWidth = i;
			if( ! testCell.getState() )
				break;
		}
		snowFlakeWidth -= Xcells / 2;
		if( snowFlakeWidth > maxSnowFlakeWidth )
			return false;

		return true;

	}

	/*
	 * stuff to do with reading the knobs
	 */
	public void updateControls()
	{
		if( start.useSerial )
		{
			if (knob2Changed())
				updateTemperature(getKnob2());
			if (knob1Changed())
				updateHumidity(getKnob1());
		}
		else
		{
			//we're using mouse
			if (knob1Changed())
			{
				updateHumidity(getKnob1());
				updateTemperature(getKnob2());
			}
		}
	}
	
	public void forceUpdateControls()
	{
		start.logger.debug( "forcing update of controls" );
		updateHumidity(getKnob1());
		updateTemperature(getKnob2());
	}


	boolean knob2Changed() {
		if (start.useSerial)
			return serialListener.knob2Changed();
		else if (mouseChanged) {
			mouseChanged = false;
			return true;
		}

		return false;
	}
	boolean knob1Changed() {
		if (start.useSerial)
			return serialListener.knob1Changed();
		else if (mouseChanged) {
			mouseChanged = false;
			return true;
		}

		return false;
	}

	float getKnob1() {
		if (start.useSerial) {
			return serialListener.getKnob1();
		} else {
			return (float) (1.0 * newX / (frameWidth + 2*controlBarWidth));
		}
	}

	float getKnob2() {
		if (start.useSerial) {
			return serialListener.getKnob2();
		} else {
			return (float) (1 - 1.0 * newY / frameHeight);
		}
	}

	static Point getWidthHeight() {
		return new Point(frameWidth + 2 * controlBarWidth, frameHeight);
	}

	public void updateHumidity(float knob1) {
		// System.out.println( newDiffusionMass );
		//		float knob1 = getKnob1();
		humidityDisplay = (float) (20 + 60 * knob1);
		// newHumidity = false;
		newDiffusionMass = knob1;
		newDiffusionMass /= 2;
		newDiffusionMass += 0.2;
		
		for (Cell cell : realCells) {
			if (cell.getState() == false && cell.getBoundary() == false)
			{
				if (cell.getPolar().getY() > (snowFlakeWidth + Xcells/40))
				{
					cell.setDiffusionMass(newDiffusionMass);
				}
			}
		}
		oldDiffusionMass = newDiffusionMass;
	}
	//public void updateTemperature( float )
	public void updateTemperature(float knob2)
	{
		//		float knob2 = getKnob2();
		//System.out.println( String.format( "temp %.2f", knob2));
		temperatureDisplay = (float) (-5 - 10 * knob2);
		knob2 = 1 - knob2;
		float newYF = knob2;
		b = 1 + newYF;
		alpha = newYF * (float) 0.1;
		theta = newYF * (float) 0.01;
		k = newYF * (float) 0.0001;
		u = newYF * (float) 0.01;
		y = newYF * (float) 0.0001;
	}

	/*
	 * routines for dealing with where cells are (for mirroring optimization)
	 */
	// tested
	static Double getXYFromPolar(double angle, double distance) {
		double y = Math.sin(angle) * distance;
		double x = Math.cos(angle) * distance;
		return new Double(x, y);
	}

	// tested
	static Point2D.Double getPolarFromXY(double x, double y) {
		// System.out.println( "X:" + x + " Y:" + y);
		double angle = Math.atan2(y, x);
		double distance = Math.hypot(x, y);
		return new Point2D.Double(angle, distance);
	}

	// tested
	// X and Y is a vectors from the origin to the top left of the cell (in
	// units of x)
	// give x and y index into the matrix
	static Double getXYFromIndex(int xIndex, int yIndex) {
		double y, x;
		x = xIndex - (Xcells / 2) + 0.5;
		if (yIndex % 2 != 0)
			x -= 0.5;
		y = (Ycells / 2) - yIndex;
		y /= cellWidth / cellHeight;
		return new Double(x, y);
	}

	// tested
	// return x and y matrix index from X and Y
	static Point getIndexFromXY(double x, double y) {
		double yRatio = cellHeight / cellWidth;
		int yIndex = (int) Math.round(Ycells / 2 - y / yRatio);
		if (yIndex % 2 == 0)
			x -= 0.5;
		int xIndex = (int) Math.round(x);
		xIndex += Xcells / 2;
		return new Point((int) xIndex, (int) yIndex);
	}

	/*
	 * debug routine
	 */
	void showCellStatus() {
		int y = (int) (newY / cellHeight);
		int xOffset = 0;
		if (y % 2 == 0)
			xOffset = (int)Math.round (cellWidth / 2);
		int x = (int)Math.round((newX - xOffset) / cellWidth);

		Cell cell = snowArray[x][y];
		System.out.println(String.format(
				"%s | x:%2d y:%2d | x:%2d y:%2d | a:%2.2f d:%2.2f | seg:%d",
				cell.isFake() ? "fake" : "real", x, y, cell.getX(),
						cell.getY(), cell.getPolar().getX(), cell.getPolar().getY(),
						cell.getSegment()));

	}
	public void buttonLED( int button, boolean state)
	{
		if( start.useSerial )
			serialListener.buttonLED( button, state);
	}
	
	public boolean paper()
	{
		if( start.useSerial )
			return serialListener.paper();
		return true;
	}
}
