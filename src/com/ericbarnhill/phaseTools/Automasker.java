package com.ericbarnhill.phaseTools;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatProcessor;

import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Automasker for MRE Images for ImageJ
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



public class Automasker {

	double seedThreshold;
	double anatomyThreshold;
	int height, width, timeSteps, depth;



	public Automasker () {

	}

	public ImagePlus getBinaryMask(ImagePlus anatomyImages, ImagePlus phaseImages, double seedThreshold, double anatomyThreshold, int depth) {
		this.seedThreshold = seedThreshold;
		this.anatomyThreshold = anatomyThreshold;
		this.width = anatomyImages.getWidth();
		this.height = anatomyImages.getHeight();
		this.depth = depth;
		timeSteps = (int)Math.round(anatomyImages.getImageStackSize()*1.0/depth*1.0);
		System.out.println("Time Steps: "+timeSteps);
		ImagePlus meanImages = generateMeanImage(anatomyImages);
		meanImages.show();
		ImagePlus binaryMask = generateBinaryMask(meanImages);
		binaryMask.show();
		return binaryMask;
	}

	public ImagePlus applyBinaryMask(ImagePlus phaseImages, ImagePlus binaryMask) {
		IJ.showStatus("Applying Binary Mask");
		this.width = phaseImages.getWidth();
		this.height = phaseImages.getHeight();
		double maskDepth = binaryMask.getImageStackSize();
		ImageStack maskedStack = new ImageStack(width, height);
		for (int n = 1; n <= phaseImages.getImageStackSize(); n++) {
			//System.out.println("on slice" + n);
			FloatProcessor maskedSlice = new FloatProcessor(width,
					height );
			float[] phasePixels = (float[])phaseImages.getStack().getProcessor(n).convertToFloat().getPixels();
			double maskValue = (n-1)%maskDepth+1;
			float[] maskPixels = (float[])binaryMask.getStack().getProcessor((int)maskValue).convertToFloat().getPixels();
			for (int i = 0; i < width; i++) {
				for (int j=0; j < height; j++) {
					maskedSlice.putPixelValue( i, j, phasePixels[i+j*width]*maskPixels[i+j*width]);
				}
			}
		maskedStack.addSlice(maskedSlice);
		}

		ImagePlus maskedImage = new ImagePlus("applied mask", maskedStack);
		return maskedImage;
	}

	public PolygonRoi generateROI(ImagePlus binaryMask) {
		ArrayList<Integer> xList = new ArrayList<Integer>();
		ArrayList<Integer> yList = new ArrayList<Integer>();
		FloatProcessor binaryProcessor = (FloatProcessor)binaryMask.getProcessor().convertToFloat();
		for (int y = 0; y < binaryProcessor.getHeight(); y++) { // get left boundary points
			leftLoop:
			for (int x = 0; x < binaryProcessor.getWidth() ; x++) {
				if ( binaryProcessor.getPixelValue(x,y)  == 1 ) {
					xList.add(x);
					yList.add(y);
					break leftLoop;
				}
			}
		}
		for (int y = (binaryProcessor.getHeight() - 1); y >= 0; y--) { // get right boundary points
			rightLoop:
			for (int x = (binaryProcessor.getWidth() - 1) ; x >= 0 ; x--) {
				if ( binaryProcessor.getPixelValue(x,y)  == 1 ) {
					xList.add(x);
					yList.add(y);
					break rightLoop;
				}
			}
		}
		int[] xArray = ArrayUtils.toPrimitive(xList.toArray(new Integer[0]));
		int[] yArray = ArrayUtils.toPrimitive(yList.toArray(new Integer[0]));
		PolygonRoi ROI = new PolygonRoi(xArray, yArray, xArray.length, Roi.POLYGON);
		//System.out.println(ROI.getLength());
		//System.out.println(ROI.getNCoordinates() );
		//System.out.println(xArray.length);
		return ROI;
	}



	private ImagePlus generateMeanImage(ImagePlus anatomyImage) {
		ImageStack meanStack = new ImageStack(anatomyImage.getProcessor().getWidth(),
				anatomyImage.getProcessor().getHeight() );
		for (int n = 1; n<= depth; n++) {
			FloatProcessor meanProcessor = new FloatProcessor(anatomyImage.getWidth(), anatomyImage.getHeight());
			for (int i = 0; i < anatomyImage.getWidth() ; i++) {
				for (int j = 0; j < anatomyImage.getHeight(); j++) {
					float pixelMean = 0;
					for (int k = 1; k<= anatomyImage.getStackSize(); k = k+depth ) {
						pixelMean += anatomyImage.getStack().getProcessor(k+n-1).getPixelValue(i,j);
					}
					pixelMean = pixelMean/timeSteps;
					meanProcessor.putPixelValue(i, j, pixelMean);
				}
			}
			meanStack.addSlice(meanProcessor);
		}
		ImagePlus meanImage = new ImagePlus("mean stack", meanStack);
		return meanImage;
	}

	private ImagePlus generateBinaryMask(ImagePlus meanImage) {
		int width = meanImage.getProcessor().getWidth();
		int height = meanImage.getProcessor().getHeight();
		int meanDepth = meanImage.getImageStackSize();
		IJ.showStatus("Seeding Binary Mask...");
		ImageStack binaryStack = new ImageStack(width,height );
			for (int n = 1; n<= meanDepth; n++) {
				FloatProcessor binaryMask = new FloatProcessor(width, height);
				//SEED
				for (int x = 0; x < width ; x++) {
					for (int y = 0; y < height; y++) {
						double value = meanImage.getStack().getProcessor(n).getPixelValue(x,y);
						//System.out.println("value at "+n+" "+x+" "+y+" is "+value);
						//System.out.println(seedThreshold);
						if (value < seedThreshold) {
							binaryMask.putPixelValue(x,y, Float.NaN);
							//System.out.println("seed at" + n + " " + x + " " + y);
						}
					}
				}
				binaryStack.addSlice(binaryMask);
			}

			IJ.showStatus("Growing Seeds...");

				//GROW SEED
			for (int n = 1; n<= meanDepth; n++) {
				//System.out.println("Seed depth "+n);
				float[] meanPixels = (float[])meanImage.getStack().getProcessor(n).convertToFloat().getPixels();
				float[] binaryPixels = (float[])binaryStack.getProcessor(n).convertToFloat().getPixels();
				for (int range = (int)seedThreshold; range < seedThreshold+5; range++) {
					//System.out.println("--Range value "+range);
					for (int x = 1; x < width - 1 ; x++) { //skip outer ring of pixels to do edge check
						for (int y = 1; y < height - 1 ; y++) {
							int index = x + y*width;
							if (!Float.isNaN( meanPixels[index]) ) {
								int contiguousTally = 0;
								for (int i = x-1; i <= x+1; i++) {
									for (int j = y-1; j <= y+1; j++) {
										int localIndex = x + y*width;
										if (  Float.isNaN( binaryPixels[localIndex] )  ) {
											contiguousTally++;
											//System.err.println("Contiguous NaN");
										} // if contiguous
									} // x contiguous
								} // y contiguous
								if (contiguousTally >= 5) {
									binaryPixels[index] = Float.NaN;
								} // if tally high enough, grow seed
							} // if current pixel is not a NaN
						} // for y
					} // for x
				} // for each value between seed and anatomy cutoffs
				FloatProcessor grownBinary = new FloatProcessor(width, height, binaryPixels);
				binaryStack.addSlice("grown", grownBinary, n);
				binaryStack.deleteSlice(n);
			} // for each slice
				//MAKE BINARY MASK

			for (int n = 1; n<= meanDepth; n++) {
				float[] binaryStackPixels = (float[])binaryStack.getProcessor(n).convertToFloat().getPixels();
				for (int x = 0; x < meanImage.getWidth() ; x++) {
					for (int y = 0; y < meanImage.getHeight(); y++) {
						int index = x + y*width;
						if (  Float.isNaN( binaryStackPixels[index]) ) {
							binaryStackPixels[index] = 0;
						} else {
							binaryStackPixels[index] = 1;
						} // if NaN make it a zero in the mask
					} // for y
				} // for x
				binaryStack.addSlice("masked", new FloatProcessor(width, height, binaryStackPixels), n);
				binaryStack.deleteSlice(n);
			} // for n

			ImagePlus binaryMaskImage = new ImagePlus("Binary Stack", binaryStack);
			return binaryMaskImage;
	}

}






