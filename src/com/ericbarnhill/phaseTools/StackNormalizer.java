package com.ericbarnhill.phaseTools;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class StackNormalizer{
	
	public StackNormalizer() {}
		
	public ImagePlus normalizeStack(ImagePlus image) {
		ImageStack floatStack = new ImageStack(image.getWidth(), image.getHeight());
		for (int n = 1; n <= image.getImageStackSize(); n++) {
			ImageProcessor slice = image.getStack().getProcessor(n);
			FloatProcessor floatSlice = (FloatProcessor)slice.convertToFloat();
			floatStack.addSlice(floatSlice);
		}
		ImagePlus floatImage = new ImagePlus("Float Stack", floatStack);
		float min = 0;
		float max = 0;
		float val = 0;
		boolean first = true;
		ImagePlus normImage = floatImage.duplicate();
		int width = normImage.getWidth();
		int height = normImage.getHeight();
		int depth = normImage.getImageStackSize();
		normImage.setTitle("Normalized Image");
		for (int i = 1; i <= depth; i++) { // GET MIN AND MAX
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < height; k++) {
					val = (float)(normImage.getImageStack().getProcessor(i).getPixelValue(j,k) );
					if (val == val) {
						if (first) {
							val = min;
							val = max;
							first = false;
						}
						if (val < min) min = val;
						if (val > max) max = val;
					}
				}
			}
		}
		for (int i = 1; i <= depth; i++) { //REWRITE IMAGE AS NORMALIZED
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < height; k++) {
					val = normImage.getImageStack().getProcessor(i).getPixelValue(j,k);
					val = (float)( (val-min)/(max-min)*Math.PI*2 );
					if (val == 0) val = Float.NaN;
					normImage.getImageStack().getProcessor(i).putPixelValue(j, k, val);
					normImage.getImageStack().getProcessor(i).setMinAndMax(0, Math.PI*2);
				}
			}
		}
		//normImage.show();
		return normImage;
	}
	
	public ImagePlus normalizeStack(ImagePlus image, ImagePlus binaryMask) {
		ImageStack floatStack = new ImageStack(image.getWidth(), image.getHeight());
		for (int n = 1; n <= image.getImageStackSize(); n++) {
			ImageProcessor slice = image.getStack().getProcessor(n);
			FloatProcessor floatSlice = (FloatProcessor)slice.convertToFloat();
			floatStack.addSlice(floatSlice);
		}
		ImagePlus floatImage = new ImagePlus("Float Stack", floatStack);
		float min = 0;
		float max = 0;
		float val = 0;
		int slice = 0;
		float binaryVal = 0;
		boolean first = true;
		ImagePlus normImage = floatImage.duplicate();
		int width = normImage.getWidth();
		int height = normImage.getHeight();
		int depth = normImage.getImageStackSize();
		int maskDepth = binaryMask.getImageStackSize();
		normImage.setTitle("Normalized Image");
		for (int i = 1; i <= depth; i++) { // GET MIN AND MAX
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < height; k++) {
					val = (float)(normImage.getImageStack().getProcessor(i).getPixelValue(j,k) );
					if (val == val) {
						if (first) {
							val = min;
							val = max;
							first = false;
						}
						if (val < min) min = val;
						if (val > max) max = val;
					}
				}
			}
		}
		for (int i = 1; i <= depth; i++) { //REWRITE IMAGE AS NORMALIZED
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < height; k++) {
					val = normImage.getImageStack().getProcessor(i).getPixelValue(j,k);
					val = (float)( (val-min)/(max-min)*Math.PI*2 );
					slice = ( (i-1) % maskDepth ) + 1;
					binaryVal = binaryMask.getStack().getProcessor(slice).getPixelValue(j,k);
					if (val == 0) {
						if (binaryVal == 1) val = 0;
						if (binaryVal == 0) val = Float.NaN;
					}
					normImage.getImageStack().getProcessor(i).putPixelValue(j, k, val);
					normImage.getImageStack().getProcessor(i).setMinAndMax(0, Math.PI*2);
				}
			}
		}
		//normImage.show();
		return normImage;
	}

	
}
