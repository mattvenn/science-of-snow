package snow;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
//some comment
public class snowcell implements Cell{

	int X, Y;
	float diffusionMass = (float) 0.2;
	float initDiffusionMass = diffusionMass;
	float oldDiffusionMass;
	float crystalMass = 0;
	float boundaryMass = 0;

	float newDiffusionMass = 0;
	float newCrystalMass = 0;
	float newBoundaryMass = 0;
	boolean newState = false;

	Point2D polar;
	
	boolean finished = false;
	boolean doCalc = false;
	boolean changed = false;
	int attachedNeighbours = 0;
	boolean state = false;
	boolean boundary = false;
	int width, height;
	Cell[][] snowArray;
	Cell[] neighbours;

	public snowcell(int x, int y, int w, int h, Cell snowarray[][]) {
		X = x;
		Y = y;
		width = w;
		height = h;
		snowArray = snowarray;

		Double XY = snow.getXYFromIndex(x, y);
		polar = snow.getPolarFromXY(XY.x, XY.y);
		
	}
	public Point2D getPolar()
	{
		return polar;
	}
	public int getSegment()
	{
		return 0;
	}
	public int getX()
	{
		return X;
		
	}
	public int getY()
	{
		return Y;
	}
	public boolean isFake()
	{
		return false;
	}
	// step 1
	public void doDiffusion() {
		//changed = false;
		if (state == false) {
			newDiffusionMass = 0;
			if (boundary) {
				// reflecting boundary conditions
				for (Cell cell : neighbours) {
					if (cell.getState()) {
						newDiffusionMass += getDiffusionMass();
					} else {
						newDiffusionMass += cell.getDiffusionMass();
					}
				}
				// my own diffusion mass
				newDiffusionMass += getDiffusionMass();
				newDiffusionMass /= (neighbours.length + 1);
			} else {
				// discrete diffusion with uniform weight of 1/7
				for (Cell cell : neighbours) {
					// cell.printState();
					newDiffusionMass += cell.getDiffusionMass();
				}
				// my own diffusion mass
				newDiffusionMass += getDiffusionMass();
				newDiffusionMass /= (neighbours.length + 1);
			}
		} else {
			newDiffusionMass = diffusionMass; // should always be 0?
			if (diffusionMass != 0)
				System.out.println("diffusion mass not 0!");
		}

//		if (newDiffusionMass != 0)
			//changed = true;
	}

	// step 2
	public void doFreezing() {
		diffusionMass = newDiffusionMass;
		if (boundary) {
			newBoundaryMass = boundaryMass + (1 - snow.k) * diffusionMass;
			newCrystalMass = crystalMass + snow.k * diffusionMass;
			// surely this next bit can't be right?
			diffusionMass = 0;
			//changed = true;
		} else {
			newBoundaryMass = boundaryMass;
			newCrystalMass = crystalMass;
		}
	}

	// step 3
	public void doAttachment() {

		boundaryMass = newBoundaryMass;
		crystalMass = newCrystalMass;
		if (boundary) {

			if (attachedNeighbours <= 2) {
				if (boundaryMass > snow.b)
					newState = true;
			} else if (attachedNeighbours == 3) {
				if (boundaryMass >= 1) {
					newState = true;
				} else {
					float summedDiffusion = diffusionMass;
					for (Cell cell : neighbours) {
						summedDiffusion += cell.getDiffusionMass();
					}

					if (summedDiffusion < snow.theta && boundaryMass >= snow.alpha)
						newState = true;
				}
			} else if (attachedNeighbours >= 4) {
				newState = true;
			}

		}
		//if (newState)
			//changed = true;
	}

	// step 4
	public void doMelting() {

		if (boundary) {
			diffusionMass = diffusionMass + snow.u * boundaryMass + snow.y * crystalMass;
			crystalMass = (1 - snow.y) * crystalMass;
			boundaryMass = (1 - snow.u) * boundaryMass;
			//changed = true;
		}

	}

	public void updateState() {
		if (newState) {
			newState = false;
			state = true;
			crystalMass = boundaryMass + crystalMass;
			diffusionMass = 0;
			boundaryMass = 0;
			boundary = false;
			doCalc = false;
		}
	}

	public void updateNeighbours() {
		// finishing the freeze
		if (state == false) {
			attachedNeighbours = 0;
			for (Cell cell : neighbours) {
				if (cell.getState()) {
					boundary = true;
					attachedNeighbours++;

				}
				/*
				if (changed) {
					cell.doCalc = true;
				}
				*/
			}
		}
		oldDiffusionMass = diffusionMass;
	}

	public boolean doCalcs() {

		if (state)
			return false;
		if (boundary)
			return true;
		if (doCalc)
			return true;
		return false;

	}

	public void setState() {
		state = true;
		diffusionMass = 0;
		boundaryMass = 0;
		crystalMass = 1;
		boundary = false;

	}
	
	public boolean getState() {
		return state;
	}
	
	public int getAttachedNeighbours(){
		return attachedNeighbours;
	}
	public boolean getBoundary() {
		return boundary;
	}

	public void setDiffusionMass( float mass )
	{
		diffusionMass = mass;
	}
	public float getDiffusionMass() {
		return diffusionMass;
	}

	public float getCrystalMass() {
		return crystalMass;
	}

	public float getBoundaryMass() {
		return boundaryMass;
	}

	public void printState() {
		// System.out.print( "x:" + nf( X, 3 ) + " y:" + nf( Y, 3 ) + " df: " +
		// nf( diffusionMass, 1, 2 ) );
		// System.out.print( " state:" + state );
		// System.out.println("");
	}

	public void getNeighbours() {
		ArrayList<Cell> neigboursTmp = new ArrayList<Cell>();
		int x = X;
		int y = Y;

		if (x > 1)
			neigboursTmp.add(snowArray[x - 1][y]);
		if (x < width - 1)
			neigboursTmp.add(snowArray[x + 1][y]);
		if (y > 1)
			neigboursTmp.add(snowArray[x][y - 1]);
		if (y < height - 1)
			neigboursTmp.add(snowArray[x][y + 1]);

		if (y % 2 == 0) {
			// odd rows
			if ((x < width - 1) && (y > 1))
				neigboursTmp.add(snowArray[x + 1][y - 1]);
			if ((x < width - 1) && (y < height - 1))
				neigboursTmp.add(snowArray[x + 1][y + 1]);
		} else {
			if ((x > 1) && (y > 1))
				neigboursTmp.add(snowArray[x - 1][y - 1]);
			if ((x > 1) && (y < height - 1))
				neigboursTmp.add(snowArray[x - 1][y + 1]);
		}
		neighbours = neigboursTmp.toArray( new Cell[neigboursTmp.size()]);
	}
}
