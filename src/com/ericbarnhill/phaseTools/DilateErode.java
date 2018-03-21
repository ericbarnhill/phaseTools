package com.ericbarnhill.phaseTools;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

import org.apache.commons.math4.stat.descriptive.rank.Median;

/**
 * Least Phase Unwrapper for PhaseTools;
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

public class DilateErode {

	ImagePlus phaseImages, unwrappedImages;
	//FloatProcessor slice;
	float[] slicePixels;
	int height, width, volumeDepth, totalColumns, totalPartitions, imageDepth, totalDepth;
	int startingSliceNumber, startingSliceDepth, startingPartition, pixelColumn;
	int timeSteps, parallelImages, imageNumber, index, partitionsPerImage;
	double[][] pixelColumns;
	boolean temporal, volume, auto;
	double[] lastValid;
	int[] lastValidSlice;
	boolean[] mask;
	double threshold, maxWindow;
	PhaseEroder eroder;
	int threshTally;


	public DilateErode(){
	}

	private void buildPixelColumns() {
		//slicePixels = new float[totalColumns];
		for (int m = 0; m < totalDepth; m++) {
			slicePixels = (float[])phaseImages.getStack().getProcessor(m+1).convertToFloat().getPixels();
			for (int n = 0; n < slicePixels.length; n++) {
				pixelColumns[n][m-index] = slicePixels[n];
			}
		}
		return;
	}



	private void syncPixels() {


			int start = startingSliceNumber;
			int top = (imageNumber+1)* imageDepth - volumeDepth;
			int bottom = imageNumber * imageDepth + volumeDepth;
			//System.out.println("Top "+top+" bottom "+bottom);
			syncToFirstPixel(start);
			dilateErodeSlice(start);
			volumeSync(start);
			syncToFirstPixel(start);
			for ( int n = start; n < top; n += volumeDepth) {
				//System.out.println("Going up - on slice "+n);
				boolean positive = true;
				boolean boundary = false;
				int current = n;
				int next = n + volumeDepth;
				if (current == start || next == top) boundary = true;
				syncNextPixel(current, next, positive, boundary);
				volumeSync(next);
			}
			syncToFirstPixel(start);
			for ( int n = start; n >= bottom; n -= volumeDepth) {
				//System.out.println("Going down - on slice "+n);
				boolean positive = false;
				boolean boundary = false;
				int current = n;
				int next = n - volumeDepth;
				if (current == start || next == bottom) boundary = true;
				syncNextPixel(current, next, positive, boundary);
				volumeSync(next);
			}


	}



	private void volumeSync(int currentSlice) {
		int start = currentSlice;
		int bottom = (int)Math.floor(currentSlice / (volumeDepth*1.0) )*volumeDepth;
		int top = bottom + volumeDepth-1;
		dilateErodeSlice(start);
		syncToFirstPixel(start);
		for (int n = start;
				n < top; n++) {
			//
			//System.out.print(n+" ");
			//
			boolean positive = true;
			boolean boundary = false;
			int current = n;
			int next = n+1;
			if (current == bottom || next == top) boundary = true;
			syncNextPixel(current, next, positive, boundary);
		}
		//
		//System.out.print("\n");
		//
		syncToFirstPixel(start);
		for (int n = start;
				n > bottom; n--) {
			//
			//System.out.print(n+" ");
			//
			boolean positive = false;
			boolean boundary = false;
			int current = n;
			int next = n-1;
			if (current == top || next == bottom) boundary = true;
			syncNextPixel(current, next, positive, boundary);
		}
	}

	private void syncToFirstPixel(int start) {
		for (pixelColumn = 0; pixelColumn < pixelColumns.length; pixelColumn++) {
			if (mask[pixelColumn]) {
				lastValid[pixelColumn] = pixelColumns[pixelColumn][start];
				lastValidSlice[pixelColumn] = start;
			}
		}
		return;
	}

	private void syncNextPixel(int current, int next, boolean positive, boolean boundary) {

		for (pixelColumn = 0; pixelColumn < pixelColumns.length; pixelColumn++) {

			if (mask[pixelColumn]) {

				double gap = Math.round( (pixelColumns[pixelColumn][next] - lastValid[pixelColumn]) / (Math.PI*2) );
				double leastPhaseValue = pixelColumns[pixelColumn][next] - gap*Math.PI*2;
				pixelColumns[pixelColumn][next] = leastPhaseValue;


			}



		}

		dilateErodeSlice(next);


		for (pixelColumn = 0; pixelColumn < pixelColumns.length; pixelColumn++) {

			if (mask[pixelColumn]) {

				updateLastValid(current, next, boundary);

			}

		}



		return;
	}

	private void dilateErodeSlice(int next) {

		double[] slice = generateSlice(next);
		for (int n = 1; n < maxWindow; n++) {
			slice = eroder.erode(slice, mask, width, height, n);
		}
		for (int n = (int)maxWindow; n > 0; n--) {
			slice = eroder.erode(slice, mask, width, height, n);
		}
		rewriteSlice(slice, next);
	}


	private double[] generateSlice(int next) {
		double[] slice = new double[pixelColumns.length];
		for (pixelColumn = 0; pixelColumn < pixelColumns.length; pixelColumn++) {
			slice[pixelColumn] = pixelColumns[pixelColumn][next];
		}
		return slice;
	}

	private void rewriteSlice(double[] erodedSlice, int next) {
		for (pixelColumn = 0; pixelColumn < pixelColumns.length; pixelColumn++) {
			double previous =  pixelColumns[pixelColumn][next];
			double eroded = erodedSlice[pixelColumn];
			pixelColumns[pixelColumn][next] = eroded;
		}
	}

	private void updateLastValid(int current, int next, boolean boundary) {

		int x = pixelColumn % width;
		int y  = (int)Math.floor(pixelColumn / width);
		double secondDifference = 0;
		double prev = 0; double mid = 0; double follow = 0;
		if (!boundary) {
			prev = pixelColumns[pixelColumn][lastValidSlice[pixelColumn]];
			mid = pixelColumns[pixelColumn][current];
			follow = pixelColumns[pixelColumn][next];
			secondDifference = prev - 2 * mid + follow;
		} else {
			mid = pixelColumns[pixelColumn][current];
			follow = pixelColumns[pixelColumn][next];
			secondDifference = pixelColumns[pixelColumn][next] - pixelColumns[pixelColumn][current]; // first difference at boundary
		}
		//
		//  System.out.println(x + " "+ y +" reliability : "+current+" "+prev+" "+mid+" "+next+" "+secondDifference);
		//
		if ( Math.abs(secondDifference) < threshold) {
			threshTally++;
			lastValid[pixelColumn] = pixelColumns[pixelColumn][current];
			lastValidSlice[pixelColumn] = current;
		}
		return;
	}

	private void buildUnwrappedImages() {
		ImageStack unwrappedStack = new ImageStack( width, height );
		for (int m = 0; m < totalDepth; m++) {
			slicePixels = new float[totalColumns];
			for (int n = 0; n < slicePixels.length; n++) {
				slicePixels[n] = (float)pixelColumns[n][m];
			}
			unwrappedStack.addSlice( Integer.toString(m), new FloatProcessor(width, height, slicePixels) );
		}
		unwrappedImages = new ImagePlus("DE Unwrap", unwrappedStack);
	}

	double getReliability(int sliceNumber) { // getProcessor off by 1 from other measures

		float[] slicePixels = (float[])( phaseImages.getStack().getProcessor(sliceNumber+1).getPixels() );
		double[] reliabilities = new double[slicePixels.length];
		for (int n = 0; n < slicePixels.length; n++) {
			reliabilities[n] = 0;
			try {
				int[] neighbourSet = new int[] {n-width-1, n-width, n-width+1, n-1, n+1, n+width-1, n+width, n+width+1};
				for (int i: neighbourSet) reliabilities[n] += Math.abs(slicePixels[i]);
			} catch (Throwable t) {
				// reliability left as zero on boundaries
			}
		}
		Median median = new Median();
		double reliability = median.evaluate(reliabilities);
		//
		System.out.println("reliability of slice "+sliceNumber+" "+reliability);
		//
		return reliability;
	}


		public ImagePlus unwrapImage(ImagePlus phaseImages, int volumeDepth, int timeSteps, int parallelImages,
				int maxWindow,	double threshold, boolean[] mask ) {
			this.phaseImages = phaseImages;
			this.height = phaseImages.getHeight();
			this.width = phaseImages.getWidth();
			this.phaseImages = phaseImages;
			this.totalDepth = phaseImages.getImageStackSize();
			this.volumeDepth = volumeDepth;
			this.timeSteps = timeSteps;
			this.imageDepth = volumeDepth * timeSteps;
			this.parallelImages = parallelImages;
			this.totalColumns = width*height;
			this.threshold = threshold;
			this.startingSliceNumber = phaseImages.getCurrentSlice()-1;
			this.imageNumber = (int)Math.floor(startingSliceNumber / imageDepth);
			this.imageDepth = (int)( totalDepth / (parallelImages*1.0) );
			this.maxWindow = maxWindow;
			this.mask = mask;

			startingPartition = (int)Math.floor(startingSliceNumber / volumeDepth);
			startingSliceDepth = startingSliceNumber % volumeDepth;

			totalPartitions = totalDepth / volumeDepth;
			partitionsPerImage = (int)( totalPartitions / (parallelImages*1.0) );
			eroder = new PhaseEroder();
			pixelColumns = new double[totalColumns][totalDepth];
			lastValid = new double[height*width];
			lastValidSlice = new int[height*width];


			buildPixelColumns();
			syncPixels();

			buildUnwrappedImages();

			return unwrappedImages;
		}


}
