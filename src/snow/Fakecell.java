package snow;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

public class Fakecell implements Cell{
	Cell mirror;
	int fakeX;
	int fakeY;
	Point2D polar;
	int segment = 0;
	Cell snowarray[][];
	public Fakecell(int x, int y, int w, int h, Cell snowarray[][]) {
		this.snowarray = snowarray;
		
		Double XY = snow.getXYFromIndex(x, y);
		polar = snow.getPolarFromXY(XY.x, XY.y);
		
	//map from 0 to 2PI
		double fakeAngle = polar.getX();
		//checked
		if( polar.getX() <= - 2 * Math.PI / 3 )
		{
			fakeAngle = polar.getX() - Math.PI / 3;
			segment = 1;
		}
		//checked
		else if( polar.getX() <= - Math.PI / 3)
		{
			fakeAngle = polar.getX() - 2 * Math.PI / 3;
			segment = 2;
		}
		//checked
		else if( polar.getX() <= 0 )
		{
			fakeAngle = polar.getX() - Math.PI;
			segment = 3;
		}	
		else if( polar.getX() <= Math.PI / 3 )
		{
			fakeAngle = polar.getX() + 2 * Math.PI / 3;
			segment = 4;
		}
		else if( polar.getX() <= 2 * Math.PI / 3)
		{
			fakeAngle = polar.getX() + Math.PI / 3;
			segment = 5;
		}
		//segment = (int) ((realAngle + Math.PI ) / ( Math.PI / 3 ));
		Double fakeXY = snow.getXYFromPolar( fakeAngle, polar.getY() );
		Point fakeIndex = snow.getIndexFromXY( fakeXY.x, fakeXY.y);
		//if( fakeIndex.y % 2 == 0 && fakeIndex.x > 0)
			//fakeIndex.x --;
		try
		{
			mirror = snowarray[fakeIndex.x][fakeIndex.y];
			fakeX = fakeIndex.x;
			fakeY = fakeIndex.y;
		} catch ( ArrayIndexOutOfBoundsException e )
		{
			mirror = snowarray[snow.Xcells-1][snow.Ycells-1];
			fakeX = snow.Xcells -1;
			fakeY = snow.Ycells -1;
		}
	}
	
	public Point2D getPolar()
	{
		return polar;
	}
	public int getSegment()
	{
		return segment;
	}
	public int getX()
	{
		return fakeX;
		
	}
	public int getAttachedNeighbours(){
		return mirror.getAttachedNeighbours();
	}
	public int getY()
	{
		return fakeY;
	}
	public boolean isFake()
	{
		return true;
	}
	
	public float getDiffusionMass()
	{
//		Cell mirror2 = snowarray[fakeX][fakeY];
	//	System.out.println( mirror2.isFake() );
		return mirror.getDiffusionMass();
	}
	public float getCrystalMass()
	{
		return mirror.getCrystalMass();
	}
	public boolean getState()
	{
		return mirror.getState();
	}
	public boolean getBoundary()
	{
		return mirror.getBoundary();
	}

	public void updateState( ){}
	public void getNeighbours(){}
	public void updateNeighbours(){}
	public void setState(){}
	public void setDiffusionMass( float mass ){}
	public void doFreezing(){}
	public void doAttachment(){}
	public void doMelting(){}
	public void doDiffusion(){}
}
