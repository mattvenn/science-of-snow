package snow;

import java.awt.geom.Point2D;

public interface Cell {

	public int getSegment();
	public int getAttachedNeighbours();
	public Point2D getPolar();
	public int getX();
	public int getY();
	public void getNeighbours();
	public void updateNeighbours();
	public void setState();
	public float getDiffusionMass();
	public float getCrystalMass();
	public void setDiffusionMass( float mass );
	public boolean getState();
	public boolean getBoundary();
	public void doFreezing();
	public void doAttachment();
	public void doMelting();
	public void doDiffusion();
	public void updateState();
	public boolean isFake();
	
}
