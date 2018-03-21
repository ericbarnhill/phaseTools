package com.ericbarnhill.phaseTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.TreeSet;

import org.apache.commons.math4.stat.descriptive.rank.Median;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Sync Neighbours Unwrapper for PhaseTools;
 * 
 * [detail]
 * [attribution]
 * @author Eric Barnhill
 * @version 0.1
 */

    /*
    Permission to use the software and accompanying documentation provided on these pages for educational, 
    research, and not-for-profit purposes, without fee and without a signed licensing agreement, is hereby
    granted, provided that the above copyright notice, this paragraph and the following two paragraphs 
    appear in all copies. The copyright holder is free to make upgraded or improved versions of the 
    software available for a fee or commercially only.

    IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, 
    OR CONSEQUENTIAL DAMAGES, OF ANY KIND WHATSOEVER, ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS 
    DOCUMENTATION, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

    THE COPYRIGHT HOLDER SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
    WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE AND ACCOMPANYING 
    DOCUMENTATION IS PROVIDED "AS IS". THE COPYRIGHT HOLDER HAS NO OBLIGATION TO PROVIDE MAINTENANCE, 
    SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
    */

public class PhaseEroder {

	
	double[] pixels, qualities;
	boolean[] hitPixels;
	PriorityQueue<Pixel> dilateHeap, erodeHeap;
	ArrayList<Pixel> pixelsToRemove;
	TreeSet<Pixel> synchronizedPixels, origPixels;
	int height, width, volumeDepth, totalColumns, totalPartitions, imageDepth;
	int startingSliceNumber, startingSliceDepth, startingPartition, pixelColumn;
	double[][] pixelColumns;
	boolean temporal, volume, auto, phaseShift;
	double lastValid, threshold, quality;
	int repeats = 0;
	int size, arraySize;
	boolean[] mask;


	public PhaseEroder(){
	}
	
	private class Pixel{
		int x, y;
		double value, quality;
		
			private Pixel(int x, int y, double value, double quality) {
				this.x = x;
				this.y = y;
				this.value = value;
				this.quality = quality;
			}
	}
	
	
	private void initDataStructures() {
		
		qualities = new double[width*height];
		
		phaseShift = false;
		
		dilateHeap = new PriorityQueue<Pixel>(width*height, new Comparator<Pixel>() {
			@Override // EXTRACT MAX
			public int compare(Pixel entry1, Pixel entry2) {
				if (entry1.quality < entry2.quality) {
					return 1;
				} else if (entry1.quality > entry2.quality) {
					return -1;
				}
				return 0;
			}
		});
		
		erodeHeap = new PriorityQueue<Pixel>(width*height, new Comparator<Pixel>() {
			@Override // EXTRACT MIN
			public int compare(Pixel entry1, Pixel entry2) {
				if (entry1.quality > entry2.quality) {
					return 1;
				} else if (entry1.quality < entry2.quality) {
					return -1;
				}
				return 0;
			}
		});
			
	}
		
	
	private void loadPixelsIntoDilateHeap() {
		double quality = 0;
		hitPixels = new boolean[width*height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int location = x + y*width;
				double center = pixels[location];
				quality = getQuality(location, center);
				if ( mask[location] && quality == quality) {
					dilateHeap.add( new Pixel(x, y, center, quality) );
				}
			}
		}
	
	}
	
	private void loadPixelsIntoErodeHeap() {
		
		hitPixels = new boolean[width*height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int location = x + y*width;
				if (mask[location]) {
					double center = pixels[location];
					double quality = getQuality(location, center);
					erodeHeap.add( new Pixel(x, y, center, quality ) );
				}
			}
		}
	
	}
		
	private void dilate() {
		while ( !dilateHeap.isEmpty()) { //twentyPercent ) {
			Pixel pixel = dilateHeap.poll();
			int location = pixel.x + pixel.y*width;
			quality = pixel.quality;
			if (pixel.quality > -Double.MAX_VALUE) {
				if (!hitPixels[location]) {
					syncNeighbours(pixel.x, pixel.y, pixel.value);
				}
			}
			hitPixels[location] = true;
		}
	}
	
	private void erode() {
		while ( erodeHeap.size() > 0 ) { //twentyPercent ) {
			Pixel pixel = erodeHeap.poll();
			int x = pixel.x;
			int y = pixel.y;
			int location = x + y*width;
			if (pixel.quality > -Double.MAX_VALUE) {
				if (!hitPixels[location]) {
					syncWithNeighbours(pixel.x, pixel.y, pixel.value);
				}
			}
		}
	}
	
	private void syncNeighbours(int centerX, int centerY, double centerVal) {
		for (int x = centerX-1; x < centerX+2; x++) {
			for (int y = centerY-1; y < centerY+2; y++) {
				if (x >= 0 && y >= 0 && x < width && y < height) {
					int location = x + y*width;
					try {
						double neighbourVal = pixels[location];
						if ( !hitPixels[location]) {
							double neighbourValSync = syncPixel(centerVal, neighbourVal, centerX, centerY, x, y);
							pixels[location] = neighbourValSync;
							hitPixels[location] = true;
						}
					}	catch (Throwable t) {}
				}
			}
		}
	}
	
	private boolean syncWithNeighbours(int centerX, int centerY, double pixelValue) {
		ArrayList<Integer> neighbourIncrements = new ArrayList<Integer>(1);
		for (int x = centerX-1; x < centerX+2; x++) {
			for (int y = centerY-1; y < centerY+2; y++) {
				if (x >= 0 && y >= 0 && x < width && y < height) {
					int location = x + y*width;
					try {
						double neighbourVal = pixels[location];
						int neighbourIncrement = (int)Math.round( (neighbourVal - pixelValue) / (Math.PI*2) );
						neighbourIncrements.add( neighbourIncrement );
					} catch (Throwable t) {}
				}
			}
		}
		
		boolean sync = true;
		int first = neighbourIncrements.get(0).intValue();
		for (Integer i : neighbourIncrements) {
			if (i.intValue() != first) sync = false;
		}
		if (sync) {
			double newValue = pixelValue + first*Math.PI*2;
			int location = centerX + (centerY*width);
			pixels[location] = newValue;
		}
		return false;
	}
	
	
	private double syncPixel(double centerVal, double neighbourVal, int centerX, int centerY, int x, int y) {
		
		double gap = Math.round( ( neighbourVal - centerVal) / (Math.PI*2) );
		double leastPhaseValue = neighbourVal - gap*Math.PI*2;
		return leastPhaseValue;
	}
	
	private double getQuality(int location, double centerValue) {
		int centerX = location % width;
		int centerY = (int)Math.floor(location / width);
		double qualitySum = 0;
		int tally = 0;
		double quality = -Double.MAX_VALUE;
		if (centerX >= size && centerY >= size && centerX < width-size && centerY < height-size) {
			for (int x = centerX-size; x <= centerX+size; x++) {
				for (int y = centerY-size; y <= centerY+size; y++) {
					if (x >= 0 && y >= 0 && x < width && y < height) {
						int pixelLocation = x + y*width;
						try {
							qualitySum += pixels[pixelLocation];
							tally++;
						}	catch (Throwable t) {}
					}
				}
			}
			quality = -Math.abs( qualitySum - (tally*centerValue) );
			if (quality == 0) quality = Double.NaN;
		}

		//if (penalty > 1) hitPixels[location] = true;
		return quality;
	}
	
	
	
	/**private void writePixelsToSlice() {
		pixels = new double[width*height];
		for (Pixel p: synchronizedPixels) {
			int pixelPlacement = p.x + p.y*width;
			pixels[pixelPlacement] = p.value;
		}
		return;
	}*/
	
	public double[] erode(double[] pixels, boolean[] mask, int width, int height, int maxSize) {
		this.height = height;
		this.width = width;
		this.pixels = pixels;
		this.arraySize = pixels.length;
		this.mask = mask;
		initDataStructures();
		for (int i = 1; i <= maxSize; i++) {
			this.size = i;
			loadPixelsIntoDilateHeap();
			dilate();
			loadPixelsIntoErodeHeap();
			erode();
		}
		for (int i = maxSize; i >= 1; i--) {
			this.size = i;
			loadPixelsIntoDilateHeap();
			dilate();
			loadPixelsIntoErodeHeap();
			erode();
		}
		return this.pixels;
	}

	
		
		
}
