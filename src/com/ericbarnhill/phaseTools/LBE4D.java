package com.ericbarnhill.phaseTools;


import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import edu.emory.mathcs.jtransforms.dct.DoubleDCT_1D;
import edu.emory.mathcs.jtransforms.dct.DoubleDCT_2D;
import edu.emory.mathcs.jtransforms.dct.DoubleDCT_3D;

public class LBE4D {
	ImagePlus image;
	int width, height, depth, timeSteps, imageDepth, parallelImages, imageNumber;
	int dim;
	double[][][][] origPixels, term1Pixels, term2Pixels, cosinePixels, sinePixels, estimate, factor, finalPixels;
	DoubleDCT_1D DCTx, DCTy, DCTz, DCTt;
	ImageStack[] stacks;
    		
	
	void sanityCheck() {
		int totalSlicesInterface = depth*timeSteps*parallelImages;
		int totalSlicesStack = image.getImageStackSize();
		if ( totalSlicesInterface == totalSlicesStack ) {
			return;
		} else {
			IJ.error("Slice Specifications Do Not Match Image Stack");
			throw new RuntimeException();
		}
	}
	
	void initVariables() {
		image = ZeroNaN.NaNToZero(image);
		DCTx = new DoubleDCT_1D(width);
		DCTy = new DoubleDCT_1D(height);
		DCTz = new DoubleDCT_1D(depth);
		DCTt = new DoubleDCT_1D(timeSteps);
		imageDepth = depth*timeSteps;
		stacks = new ImageStack[parallelImages];
	}
	
	
	void loadArray() {
		origPixels = new double[width][height][depth][timeSteps];
		for (int t = 0; t < timeSteps; t++) {
			for (int z = 0; z < depth; z++) {
				int sliceIndex = imageNumber*imageDepth + t*depth + z;
				float[] imagePixels = (float[])image.getStack().getProcessor(sliceIndex+1).getPixels();
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						double value = imagePixels[x + y*width];
						origPixels[x][y][z][t] = value;
					} //x
				} //y
			} //z
		} //t
	}
	
	void unwrapArray() {
		
		IJ.showStatus("Image " +(imageNumber+1)+ " Term 1 of 2");

		cosinePixels = getCosine(origPixels);
		sinePixels = getSine(origPixels);
		term1Pixels = getSine(origPixels);
		getDCT(term1Pixels, true);
		//runImage(term1Pixels, "DCT Fwd", true);
		term1Pixels = multiplyByCoordinates(term1Pixels);
		getDCT(term1Pixels, false);
		//runImage(term1Pixels, "DCT Inv", true);
		term1Pixels = multiplyByCosine(term1Pixels);
		getDCT(term1Pixels, true);
		term1Pixels = divideByCoordinates(term1Pixels);
		getDCT(term1Pixels, false);
		
		IJ.showStatus("Image " +(imageNumber+1)+ " Term 2 of 2");

		
		term2Pixels = getCosine(origPixels);
		getDCT(term2Pixels, true);
		term2Pixels = multiplyByCoordinates(term2Pixels);
		getDCT(term2Pixels, false);
		term2Pixels = multiplyBySine(term2Pixels);
		getDCT(term2Pixels, true);
		term2Pixels = divideByCoordinates(term2Pixels);
		getDCT(term2Pixels, false);
		
		finalPixels = finalValues(term1Pixels, term2Pixels);
	}
		

	double[][][][] getSine (double[][][][] pixels) {
		double[][][][] sinePixels = new double[width][height][depth][timeSteps];
		for (int t = 0; t < timeSteps; t++) {
			for (int z = 0; z < depth; z++) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						double value = Math.sin(pixels[x][y][z][t]);
						sinePixels[x][y][z][t] = Math.sin(pixels[x][y][z][t]);
					} //x
				} //y
			} //z
		} //t
		return sinePixels;
	}
	
	double[][][][] getCosine (double[][][][] pixels) {
		double[][][][] cosinePixels = new double[width][height][depth][timeSteps];
		for (int t = 0; t < timeSteps; t++) {
			for (int z = 0; z < depth; z++) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						cosinePixels[x][y][z][t] = Math.cos(pixels[x][y][z][t]);
					} //x
				} //y
			} //z
		} //t
		return cosinePixels;
	}
		
	double[][][][] multiplyBySine (double[][][][] pixels) {
		double[][][][] multipliedPixels = new double[width][height][depth][timeSteps];
		for (int t = 0; t < timeSteps; t++) {
			for (int z = 0; z < depth; z++) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						multipliedPixels[x][y][z][t] = pixels[x][y][z][t] * sinePixels[x][y][z][t];
					} //x
				} //y
			} //z
		} //t
		return multipliedPixels;
	}
	
	double[][][][] multiplyByCosine (double[][][][] pixels) {
		double[][][][] multipliedPixels = new double[width][height][depth][timeSteps];
		for (int t = 0; t < timeSteps; t++) {
			for (int z = 0; z < depth; z++) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						multipliedPixels[x][y][z][t] = pixels[x][y][z][t] * cosinePixels[x][y][z][t];
					} //x
				} //y
			} //z
		} //t
		return multipliedPixels;
	}
	
	double[][][][] multiplyByCoordinates (double[][][][] pixels) {
		double[][][][] multipliedPixels = new double[width][height][depth][timeSteps];
		for (int t = 0; t < timeSteps; t++) {
			for (int z = 0; z < depth; z++) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						double mask = (x+1)*(x+1) + (y+1)*(y+1) + (z+1)*(z+1) + (t+1)*(t+1);
						multipliedPixels[x][y][z][t] = pixels[x][y][z][t] * mask;
					} //x
				} //y
			} //z
		} //t
		return multipliedPixels;
	}
	
	double[][][][] divideByCoordinates (double[][][][] pixels) {
		double[][][][] dividedPixels = new double[width][height][depth][timeSteps];
		for (int t = 0; t < timeSteps; t++) {
			for (int z = 0; z < depth; z++) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						double mask = (x+1)*(x+1) + (y+1)*(y+1) + (z+1)*(z+1) + (t+1)*(t+1);
						dividedPixels[x][y][z][t] = pixels[x][y][z][t] / mask;
					} //x
				} //y
			} //z
		} //t
		return dividedPixels;
	}
	
		
	
	public void getDCT(double[][][][] pixels, boolean forward) {

		//   X
		
		for (int t = 0; t < timeSteps; t++) {
			for (int z = 0; z < depth; z++) {
				for (int y = 0; y < height; y++) {
					double[] xArray = new double[width];
					for (int x = 0; x < width; x++) {
						xArray[x] = pixels[x][y][z][t];
					} //x
					if (forward) DCTx.forward(xArray, true);
					if (!forward) DCTx.inverse(xArray, true);
					for (int x = 0; x < width; x++) {
						pixels[x][y][z][t] = xArray[x];
					} //x
				} //y
			} //z
		} //t
		
		//   Y
		
		for (int t = 0; t < timeSteps; t++) {
			for (int z = 0; z < depth; z++) {
				for (int x = 0; x < width; x++) {
					double[] yArray = new double[height];
					for (int y = 0; y < height; y++) {
						yArray[y] = pixels[x][y][z][t];
					}
					if (forward) DCTy.forward(yArray, true);
					if (!forward) DCTy.inverse(yArray, true);
					for (int y = 0; y < height; y++) {
						pixels[x][y][z][t] = yArray[y];
					} //y
				} //x
			} //z
		} //t
		
		//   Z
		
		for (int t = 0; t < timeSteps; t++) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double[] zArray = new double[depth];
					for (int z = 0; z < depth; z++) {
						zArray[z] = pixels[x][y][z][t];
					}
					if (forward) DCTz.forward(zArray, true);
					if (!forward) DCTz.inverse(zArray, true);
					for (int z = 0; z < depth; z++) {
						pixels[x][y][z][t] = zArray[z];
					} //z
				} //x
			} //y
		} //t
		
		//   T
		
		for (int z = 0; z < depth; z++) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double[] tArray = new double[timeSteps];
					for (int t = 0; t < timeSteps; t++) {
							tArray[t] = pixels[x][y][z][t];
					}
					if (forward) DCTt.forward(tArray, true);
					if (!forward) DCTt.inverse(tArray, true);
					for (int t = 0; t < timeSteps; t++) {
						pixels[x][y][z][t] = tArray[t];
					} //t
				} //x
			} //y
		} //z
			
		return;
	}
	
	ImageStack runImage(double[][][][] pixels, String title, boolean show) {
		ImageStack stack = new ImageStack(width, height);
		for (int t = 0; t < timeSteps; t++) {
			for (int z = 0; z < depth; z++) {
				float[] imagePixels = new float[height*width];
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						double value = pixels[x][y][z][t];
						imagePixels[x + y*width] = (float)value;
					} //x
				} //y
				FloatProcessor processor = new FloatProcessor(width, height, imagePixels);
				stack.addSlice(processor);
			} //z
		} //t
		
		if (show) {
			ImagePlus image = new ImagePlus(title, stack);
			image.show();
		}
		return stack;
	}
	
	double[][][][] finalValues(double[][][][] term1Pixels, double[][][][] term2Pixels) {
		double[][][][] finalPixels = new double[width][height][depth][timeSteps];
		for (int t = 0; t < timeSteps; t++) {
			for (int z = 0; z < depth; z++) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						double mask = (x+1)*(x+1) + (y+1)*(y+1) + (z+1)*(z+1) + (t+1)*(t+1);
						finalPixels[x][y][z][t] = term1Pixels[x][y][z][t] - term2Pixels[x][y][z][t];
					} //x
				} //y
			} //z
		} //t
		return finalPixels;
	}
	
	public ImagePlus unwrapImage(ImagePlus image, int volumeDepth, int timeSteps, int parallelImages) {
		this.image = image;
		this.width = image.getWidth();
		this.height = image.getHeight();
		this.timeSteps = timeSteps;
		this.depth = volumeDepth;
		this.parallelImages = parallelImages;
		initVariables();
		sanityCheck();
		
		for (imageNumber = 0; imageNumber < parallelImages; imageNumber++) {
			loadArray();
			unwrapArray();
			stacks[imageNumber] = runImage(finalPixels, Integer.toString(imageNumber), false);
		}
		
		ImageStack finalStack = new ImageStack(width, height);
		for (ImageStack stack : stacks) {
			for (int n = 0; n < stack.getSize(); n++) {
				finalStack.addSlice(stack.getProcessor(n+1));
			}
		}
		ImagePlus finalImage = new ImagePlus("4D LBE Unwrap", finalStack);
		return finalImage;
		
	}		

	
	
}
