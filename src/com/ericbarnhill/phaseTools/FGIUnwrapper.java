package com.ericbarnhill.phaseTools;

import org.apache.commons.math4.complex.Complex;

public class FGIUnwrapper {
	
	int h, w, d;
	
	public FGIUnwrapper() {
		
	}
	
	
	class Voxel {
		
		boolean isPrimary;
		int primaryRank, operativeGradient;
		double xGradient, yGradient, zGradient;
		
		public Voxel (double xg, double yg, double zg) {
			
			xGradient = xg;
			yGradient = yg;
			zGradient = zg;
			
			
		}
		
		
		
	}
	
	double[][][] getAngle(double[][][] field) {
		
		double[][][] angle = new double[w][h][d];
		
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				for (int z = 0; z < d; z++) {
					angle[x][y][z] = Complex.I.multiply(field[x][y][z]).exp().getArgument();
				}
			}
		}
		
		return angle;
		
	}
	
	Complex[][][] getExp(double[][][] field) {
		
		Complex[][][] angle = new Complex[w][h][d];
		
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				for (int z = 0; z < d; z++) {
					angle[x][y][z] = Complex.I.multiply(field[x][y][z]).exp();
				}
			}
		}
		
		return angle;
		
	}
	
	
	public double[][][] unwrap(double[][][] wrappedImage) {
		
		double[][][] angle = getAngle(wrappedImage);
		Complex[][][] imageExp = getExp(angle);
		
		return angle; // FOR TESTING
		
	}
	
	
	

}
