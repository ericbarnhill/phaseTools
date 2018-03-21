package com.ericbarnhill.phaseTools;

import ij.ImagePlus;


/**
 * SchofieldZhu_Unwrapper - Imagej PLugin
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

public class ZeroNaN{   

 	protected int dimension;

    public ZeroNaN(){}
    
	public static ImagePlus zeroToNaN(ImagePlus image) {
		for (int slice = 1; slice <= image.getImageStackSize(); slice++) {
			for (int row = 0; row < image.getProcessor().getHeight(); row++) {
				for (int column = 0; column < image.getProcessor().getWidth(); column++) {
					if ( image.getStack().getProcessor(slice).getPixelValue(column, row) == 0) {
						image.getStack().getProcessor(slice).putPixelValue(column, row, Float.NaN);
					}
				}
			}
		}
	return image;
	}

	public static ImagePlus NaNToZero(ImagePlus image) {
		for (int slice = 1; slice <= image.getImageStackSize(); slice++) {
			for (int row = 0; row < image.getProcessor().getHeight(); row++) {
				for (int column = 0; column < image.getProcessor().getWidth(); column++) {
					if (  Float.isNaN( image.getStack().getProcessor(slice).getPixelValue(column, row) )  ) {
						image.getStack().getProcessor(slice).putPixelValue(column, row, 0);
					}
				}
			}
		}
	return image;
	}
}
        
