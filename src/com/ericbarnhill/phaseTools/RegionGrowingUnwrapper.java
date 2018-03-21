package com.ericbarnhill.phaseTools;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.lang3.ArrayUtils;


//VOXEL ARRAY KEY
//x = voxel number
//voxel[x][0] = value
//voxel[x][1] = reliability
//voxel[x][2] = sort location
//voxel[x][3] = number of group
//voxel[x][4] = increment

//EDGE ARRAY KEY
//x/3 = hEdge x/3+1 vEdge x/3+2 nEdge
//edge[x][0] = location
//edge[x][1] = reliability
//edge[x][2] = increment

//NEIGHBOR ARRAY KEY
//pixels in array proceed in rows, cols, slices order (NIfTi standard order)

//GROUPSCOUNT KEY
//groupsCount[x] = number of pixels in the group

public class RegionGrowingUnwrapper{
	double incrementThreshold;
	double[][] voxel;
	double[][] edge;
	double[] neighbors;
	float[] slicePixels, paddedSlicePixels;
	int[] groupCounts;
	int width, height, depth, area;
	int paddedWidth, paddedHeight, paddedDepth, paddedArea;
	int voxelNumber;
	boolean incrementImage, reliabilityImage;

	public RegionGrowingUnwrapper() {
		}


	ImagePlus unwrapImage(ImagePlus image, double incrementThreshold, boolean incrementImage, boolean reliabilityImage) {
		this.incrementImage = incrementImage;
		this.reliabilityImage = reliabilityImage;
		width = image.getWidth();
		height = image.getHeight();
		area = width*height;
		depth = image.getImageStackSize();
		paddedWidth = width+2;
		paddedHeight = height+2;
		paddedArea = paddedWidth*paddedHeight;
		paddedDepth = depth+2;
		voxel = new double[paddedWidth*paddedHeight*paddedDepth][5];
		edge = new double[paddedWidth*paddedHeight*paddedDepth*3][3];
		neighbors = new double[27];
		loadVoxelsIntoArray(image);
		calculateVoxelReliability(image);
		calculateEdgeReliability(incrementThreshold);
		sortEdges();
		gatherVoxels();
		if (incrementImage) generateIncrementWeightedImage();
		if (reliabilityImage) generateReliabilityWeightedImage();
		ImagePlus newImage = generateImage();
		return newImage;
	}
	
	


	void loadVoxelsIntoArray(ImagePlus image) {
		for (int n = 0; n < voxel.length; n++) voxel[n][0] = Double.NaN;
		for (int i = 0; i < depth; i++) {
			paddedSlicePixels = (float[])image.getImageStack().getProcessor(i+1).convertToFloat().getPixels();
			for (int j = 0; j < height; j++) {
				for (int k = 0; k < width; k++) {
					voxelNumber = (k+1) + (j+1)*paddedWidth + (i+1)*paddedArea; // +1s to put values in middle of NaN padding
					voxel[(int)voxelNumber][0] = paddedSlicePixels[k + (j*width)];
				}
			}
		}
		return;
	}
				
	void calculateVoxelReliability(ImagePlus image) {
		double h, v, n, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, reliability;
		for (int i = 0; i < paddedDepth; i++) {
			for (int j = 0; j < paddedHeight; j++) {
				for (int k = 0; k < paddedWidth; k++) {
					voxelNumber = i*paddedArea +  j*paddedWidth + k;
					if (i == 0 || j == 0 || k == 0 || i == paddedDepth-1 || j == paddedHeight-1 || k == paddedWidth-1) {
						voxel[(int)voxelNumber][1] = Double.NaN;
					} else {
						loadNeighbors(i, j, k); 
						h = (neighbors[12] - neighbors[13]) - (neighbors[13] - neighbors[14]);
						v = (neighbors[10] - neighbors[13]) - (neighbors[13] - neighbors[16]);
						n = (neighbors[4] - neighbors[13]) - (neighbors[13] - neighbors[22]);
						d1 = (neighbors[9] - neighbors[13]) - (neighbors[13] - neighbors[17]);
						d2 = (neighbors[15] - neighbors[13]) - (neighbors[13] - neighbors[11]);
						d3 = (neighbors[18] - neighbors[13]) - (neighbors[13] - neighbors[8]);
						d4 = (neighbors[19] - neighbors[13]) - (neighbors[13] - neighbors[7]);
						d5 = (neighbors[20] - neighbors[13]) - (neighbors[13] - neighbors[6]);
						d6 = (neighbors[21] - neighbors[13]) - (neighbors[13] - neighbors[5]);
						d7 = (neighbors[24] - neighbors[13]) - (neighbors[13] - neighbors[2]);
						d8 = (neighbors[23] - neighbors[13]) - (neighbors[13] - neighbors[3]);
						d9 = (neighbors[25] - neighbors[13]) - (neighbors[13] - neighbors[1]);
						d10 = (neighbors[26] - neighbors[13]) - (neighbors[13] - neighbors[0]);
						double[] reliabilityArray = new double[]{h, v, n, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10};
						double reliabilityDenom = 0;
						for (int m = 0; m < reliabilityArray.length; m++) {
							if (reliabilityArray[m] == reliabilityArray[m]) {
								reliabilityDenom += Math.pow(reliabilityArray[m],2);
							}
						}
						reliability = 1 / reliabilityDenom;
						//prevent processing of padding
						if ( Double.isInfinite(reliability) ) reliability = Double.MAX_VALUE; 
						//prevent spiking of border pixels
						if ( Double.isNaN(h) || Double.isNaN(v) || Double.isNaN(d1) || Double.isNaN(d2) ) reliability = 0; 
						//prevent spiking of first and last slices in 3D, will still have correct relative values for 2D
						if ( Double.isNaN(n) || Double.isNaN(d3) || Double.isNaN(d4) || Double.isNaN(d5) || Double.isNaN(d6) || Double.isNaN(d7) ||
							Double.isNaN(d8) || Double.isNaN(d9) || Double.isNaN(d10) ) reliability = reliability/1000; 
						voxel[(int)voxelNumber][1] = reliability;
						//if (j > 60 && j < 70 && k > 80 && k < 90) System.out.println((k-1) + " " + (j-1) + " " + reliabilityDenom + " " +reliability);
						
					}
				}
			}
		}
		return; 
	}

	void loadNeighbors(int i, int j, int k) { 
		neighbors[0] = voxel[paddedArea*(i-1)+paddedWidth*(j-1)+(k-1)][0];
		neighbors[1] = voxel[paddedArea*(i-1)+paddedWidth*(j-1)+(k)][0];
		neighbors[2] = voxel[paddedArea*(i-1)+paddedWidth*(j-1)+(k+1)][0];
		neighbors[3] = voxel[paddedArea*(i-1)+paddedWidth*(j)+(k-1)][0];
		neighbors[4] = voxel[paddedArea*(i-1)+paddedWidth*(j)+(k)][0];
		neighbors[5] = voxel[paddedArea*(i-1)+paddedWidth*(j)+(k+1)][0];
		neighbors[6] = voxel[paddedArea*(i-1)+paddedWidth*(j+1)+(k-1)][0];
		neighbors[7] = voxel[paddedArea*(i-1)+paddedWidth*(j+1)+(k)][0];
		neighbors[8] = voxel[paddedArea*(i-1)+paddedWidth*(j+1)+(k+1)][0];
		neighbors[9] = voxel[paddedArea*(i)+paddedWidth*(j-1)+(k-1)][0];
		neighbors[10] = voxel[paddedArea*(i)+paddedWidth*(j-1)+(k)][0];
		neighbors[11] = voxel[paddedArea*(i)+paddedWidth*(j-1)+(k+1)][0];
		neighbors[12] = voxel[paddedArea*(i)+paddedWidth*(j)+(k-1)][0];
		neighbors[13] = voxel[paddedArea*(i)+paddedWidth*(j)+(k)][0];
		neighbors[14] = voxel[paddedArea*(i)+paddedWidth*(j)+(k+1)][0];
		neighbors[15] = voxel[paddedArea*(i)+paddedWidth*(j+1)+(k-1)][0];
		neighbors[16] = voxel[paddedArea*(i)+paddedWidth*(j+1)+(k)][0];
		neighbors[17] = voxel[paddedArea*(i)+paddedWidth*(j+1)+(k+1)][0];
		neighbors[18] = voxel[paddedArea*(i+1)+paddedWidth*(j-1)+(k-1)][0];
		neighbors[19] = voxel[paddedArea*(i+1)+paddedWidth*(j-1)+(k)][0];
		neighbors[20] = voxel[paddedArea*(i+1)+paddedWidth*(j-1)+(k+1)][0];
		neighbors[21] = voxel[paddedArea*(i+1)+paddedWidth*(j)+(k-1)][0];
		neighbors[22] = voxel[paddedArea*(i+1)+paddedWidth*(j)+(k)][0];
		neighbors[23] = voxel[paddedArea*(i+1)+paddedWidth*(j)+(k+1)][0];
		neighbors[24] = voxel[paddedArea*(i+1)+paddedWidth*(j+1)+(k-1)][0];
		neighbors[25] = voxel[paddedArea*(i+1)+paddedWidth*(j+1)+(k)][0];
		neighbors[26] = voxel[paddedArea*(i+1)+paddedWidth*(j+1)+(k+1)][0];
		if (voxelNumber > 17000 && voxelNumber < 19000) {
		}
		return; 
	}			

	void calculateEdgeReliability(double incrementThreshold) {
		double hDiff, vDiff, nDiff, mainVox, hNeighbor, vNeighbor, nNeighbor;
		for (int i = 0; i < paddedDepth-1; i++) {
			for (int j = 0; j < paddedHeight-1; j++) {
				for (int k = 0; k < paddedWidth-1; k++) {
					voxelNumber = i*paddedArea + j*paddedWidth + k;
					//run for x, y , and z forward edge of each voxel
					int hEdge = voxelNumber * 3;
					int vEdge = voxelNumber * 3 + 1;
					int nEdge = voxelNumber * 3 + 2;
					edge[hEdge][0] = hEdge;
					edge[vEdge][0] = vEdge;
					edge[nEdge][0] = nEdge;
					edge[hEdge][1] = voxel[(int)voxelNumber][1] + voxel[(int)(voxelNumber+1)][1];
					edge[vEdge][1] = voxel[(int)voxelNumber][1] + voxel[(int)(voxelNumber+paddedWidth)][1];
					edge[nEdge][1] = voxel[(int)voxelNumber][1] + voxel[(int)(voxelNumber+paddedArea)][1];
					mainVox = voxel[(int)voxelNumber][0];
					hNeighbor = voxel[(int)(voxelNumber+1)][0];
					vNeighbor = voxel[(int)(voxelNumber+paddedWidth)][0];
					nNeighbor = voxel[(int)(voxelNumber+paddedArea)][0];
					hDiff = mainVox - hNeighbor;
					vDiff = mainVox - vNeighbor;
					nDiff = mainVox - nNeighbor;
					if (hDiff > incrementThreshold) {//Math.PI
						edge[hEdge][2]  = -1;
					} else if (hDiff < -1*incrementThreshold) {
						edge[hEdge][2]  = 1;
					}
					if (vDiff > incrementThreshold) {
						edge[vEdge][2]  = -1;
					} else if (vDiff < -1*incrementThreshold) {
						edge[vEdge][2]  = 1;
					}
					if (nDiff > incrementThreshold) {
						edge[nEdge][2]  = -1;
					} else if (nDiff < -1*incrementThreshold) {
						edge[nEdge][2]  = 1;
					}
				}
			}
		}
		return;
	}

	void sortEdges() {
		for (int i = 0; i < edge.length; i++) {
				if (Double.isNaN(edge[i][1])) edge[i][1] = -1;
		}
		Arrays.sort(edge, new Comparator<double[]>() {
				@Override
				public int compare(double[] entry1, double[] entry2) {
				if (entry1[1] < entry2[1]) {
					return 1;
				} else return 0;
				}
			}
		);
		for (int i = 0; i < edge.length; i++) {
				if (edge[i][1] == -1) edge[i][1] = Double.NaN;
		}
		return;
	}

	void gatherVoxels() {
		int voxelPart1;
		int voxelPart2;
		for (int i = 0; i < edge.length; i++) {
			voxelPart1 = (int)Math.floor(edge[i][0]/3);
			voxelPart2 = (int)(edge[i][0] % 3);
			if (voxelPart2 == 0) {
				calculateMerge(voxelPart1, voxelPart1+1, i);
			} else if (voxelPart2 == 1) {
				calculateMerge(voxelPart1, voxelPart1+paddedWidth, i);
			} else if (voxelPart2 == 2) {
				calculateMerge(voxelPart1, voxelPart1+paddedArea, i);
			}
		}
		return;
	}

	void calculateMerge(int voxel1, int voxel2, int i) {
		if (voxel[voxel1][3] == 0 && voxel[voxel2][3] == 0) {
			groupCounts = ArrayUtils.add(groupCounts, 1);
			voxel[voxel1][3] = groupCounts.length-1; //seed new group
			}
		if (voxel[voxel1][3] != voxel[voxel2][3]) {
			if (groupCounts[(int)voxel[voxel2][3]] == 1) {
				voxel[voxel2][3] = voxel[voxel1][3];
				groupCounts[(int)voxel[voxel1][3]]++;
				voxel[voxel2][4] = voxel[voxel1][4]-edge[i][2];
				} else if (groupCounts[(int)voxel[voxel1][3]] == 1) {
				voxel[voxel1][3] = voxel[voxel2][3];
				groupCounts[(int)voxel[voxel2][3]]++;
				voxel[voxel1][4] = voxel[voxel2][4]+edge[i][2];
			} else if ( groupCounts[(int)voxel[voxel1][3]] > groupCounts[(int)voxel[voxel2][3]] ) {
				mergeGroups1(voxel1, voxel2, i);
			} else if ( groupCounts[(int)voxel[voxel2][3]] > groupCounts[(int)voxel[voxel1][3]] ) {
				mergeGroups2(voxel1, voxel2, i);
			}
		}
	}

	void mergeGroups1(int voxel1, int voxel2, int i) { 
		int receivingGroup = (int)voxel[voxel1][3];
		int mergingGroup = (int)voxel[voxel2][3];
		int newIncrement = (int)(voxel[voxel1][4]-edge[i][2]-voxel[voxel2][4]);
		for (int j = 0; j < voxel.length; j++) {
			if (voxel[j][3] == mergingGroup) {
				voxel[j][3] = receivingGroup;
				groupCounts[mergingGroup]--;
				groupCounts[receivingGroup]++;
				voxel[j][4] = voxel[j][4] + newIncrement;
				}
		}
		return;
	}

	void mergeGroups2(int voxel1, int voxel2, int i) {
		int receivingGroup = (int)voxel[voxel2][3];
		int mergingGroup = (int)voxel[voxel1][3];
		int newIncrement = (int)(voxel[voxel2][4]+edge[i][2]-voxel[voxel1][4]);
		for (int j = 0; j < voxel.length; j++) {
			if (voxel[j][3] == mergingGroup) {
				voxel[j][3] = receivingGroup;
				groupCounts[mergingGroup]--;
				groupCounts[receivingGroup]++;
				voxel[j][4] = voxel[j][4] + newIncrement;
				}
		}
		return;
	}

	ImagePlus generateImage() {
		double voxelValue;
		ImageStack imStack = new ImageStack(width, height);
		
		for (int i = 0 ; i < voxel.length; i++) {
			voxelValue = (voxel[i][0] + voxel[i][4]*Math.PI*2);
			voxel[i][0] = voxelValue;
		}
		
		slicePixels = new float[height*width];
		
		for (int i = 0; i < depth; i++) {
			for (int j = 0; j < height; j++) {
				for (int k = 0; k < width; k++) {
					voxelNumber = (k+1) + (j+1)*paddedWidth + (i+1)*paddedArea;
					slicePixels[k + (j*width)] = (float)voxel[(int)voxelNumber][0];
				}
			}
			imStack.addSlice(new FloatProcessor(width, height, slicePixels)); //if condition removes padding slices
		}
		ImagePlus unwrappedImage = new ImagePlus("Unwrapped Image", imStack);
		return unwrappedImage;
	}


	void generateReliabilityWeightedImage() {
		//display reliability-weighted image
		ImageStack reliabilityStack = new ImageStack(width, height);
		for (int i = 0; i < depth; i++) {
			FloatProcessor slice = new FloatProcessor(width, height);
			for (int j = 0; j < height; j++) {
				for (int k = 0; k < width; k++) {
					voxelNumber = (i+1)*paddedArea + (j+1)*paddedWidth + (k+1);
					slice.putPixelValue(k, j, voxel[voxelNumber][1]);
				}
			}
			reliabilityStack.addSlice("slice",slice);
			slice.setMinAndMax(slice.getMin(), slice.getMax());
		}
		ImagePlus reliabilityImage = new ImagePlus("Reliability weighted image", reliabilityStack);
		reliabilityImage.show();
		//for (int i = 0; i < 8; i++) {IJ.run("In [+]"); }
		return;
	}

	void generateIncrementWeightedImage() {
		double voxelValue;
		ImageStack stack = new ImageStack(width, height);
		for (int i = 0; i < depth; i++) {
			FloatProcessor slice = new FloatProcessor(width, height);
			for (int j = 0; j < height; j++) {
				for (int k = 0; k < width; k++) {
					voxelNumber = (k+1) + (j+1)*paddedWidth + (i+1)*paddedArea;
					voxelValue = voxel[(int)voxelNumber][4];
					slice.putPixelValue(k, j, voxelValue);
				
				}
			}
			stack.addSlice("slice",slice);
			slice.setMinAndMax(slice.getMin(), slice.getMax());
		}
		ImagePlus incrementImage = new ImagePlus("Increment Weighted Image", stack);
		incrementImage.show();
		//for (int i = 0; i < 8; i++) {IJ.run("In [+]"); }
		return;
	}
	
}