package com.ericbarnhill.phaseTools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;

import com.ericbarnhill.niftijio.NiftiVolume;
import com.ericbarnhill.niftijio.NiftiHeader;
import com.ericbarnhill.niftijio.FourDimensionalArray;

import com.ericbarnhill.arrayMath.ArrayMath;

import org.apache.commons.math4.stat.descriptive.rank.Max;
import org.apache.commons.math4.stat.descriptive.rank.Min;
import org.apache.commons.math4.stat.descriptive.rank.Median;

import org.apache.commons.lang3.ArrayUtils;

import org.apache.commons.cli.*;

public class RG2D { 

    static final double INCREMENT_THRESHOLD = 3.14;
	double[][] phase;
    double[][] magnitude;
	List<Voxel> voxels;
	List<Edge> edges;
	List<Group> groups;
    List<Float> magnitudes;

	float[] slicePixels, SlicePixels;
	int acqs, width, height, area, timeSteps, depth, volume;
	double incrementThreshold;
	static int[][] differences;

	public RG2D() {}


	private class Voxel {

		float value;
		float reliability = 0;
		int group = 0;
		int increment = 0;

		private Voxel(float value) {
			this.value = value;
		}

	}

	private class Edge {

		int voxel1;
		int voxel2;
		float reliability;
		int increment;

		private Edge(int voxel1, int voxel2, boolean temporal) {
			this.voxel1 = voxel1;
			this.voxel2 = voxel2;
			double factor = 1;
			this.reliability = (float)( (voxels.get(voxel1).reliability  + voxels.get(voxel2).reliability)*factor );
			double voxel1Value = voxels.get(voxel1).value;
			double voxel2Value = voxels.get(voxel2).value;
			double difference = voxel1Value - voxel2Value;
			if ( difference > incrementThreshold) {
				this.increment = -1;
			} else if ( difference < -1*incrementThreshold) {
				this.increment = 1;
			} else {
				this.increment = 0;
			}
		}

	}

	private class Group {

		ArrayList<Integer> voxelsInGroup;
		int groupNumber;

		private Group(int voxelSeed) {
			voxelsInGroup = new ArrayList<Integer>(0);
			voxelsInGroup.add(new Integer(voxelSeed));
			this.groupNumber = voxelSeed;
		}

		private void absorbGroup(Group g, int incrementAdj) {

			for (Integer i : g.voxelsInGroup ) {
				int currentIncrement = voxels.get(i.intValue()).increment;
				voxels.get(i.intValue()).increment = currentIncrement + incrementAdj;
				voxels.get(i.intValue()).group = groupNumber;
				voxelsInGroup.add(i);
			}
			g.voxelsInGroup = new ArrayList<Integer>(1);
		}

	}

	private void initVariablesArray() {
		differences = Differences2D.getDifferences();
        width = phase.length;
        height = phase[0].length;
        area = width*height;
	}

	private void loadVoxelsMagnitude() {
		voxels = new ArrayList<Voxel>();
		groups = new ArrayList<Group>();
        magnitudes = new ArrayList<Float>();
		edges = new ArrayList<Edge>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float magValue = (float)magnitude[x][y];
                magnitudes.add(magValue);
                float value = (float)phase[x][y];
                if (Float.isNaN(value)) {
                    voxels.add(new Voxel(Float.NaN));
                } else {
                    voxels.add(  new Voxel(value)  );
                }
            } // for x
        } // for y
	}

	private void loadVoxelsArray() {
		voxels = new ArrayList<Voxel>();
		groups = new ArrayList<Group>();
		edges = new ArrayList<Edge>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float value = (float)phase[x][y];
                if (Float.isNaN(value)) {
                    voxels.add(new Voxel(Float.NaN));
                } else {
                    voxels.add(  new Voxel(value)  );
                }
            } // for x
        } // for y
    }

int[] returnCoordinates(int voxelNumber) {
    int x = (int) (  (  voxelNumber / (width*1.0)  - Math.floor( voxelNumber / (width*1.0) )  )*width  );
    int y = (int) Math.floor(
            ( (voxelNumber / (area*1.0) ) - Math.floor( voxelNumber / (area*1.0) ) )*area / (width*1.0)
            );
    return new int[] {x, y};
}

void calculateVoxelReliabilitiesMagnitude() {
    int voxelNumber = 0;
    for (Voxel v : voxels) {
        if (v.value == v.value) {
            v.reliability = getReliability(voxelNumber)/magnitudes.get(voxelNumber);
        } else v.reliability = Float.NaN;
        voxelNumber++;
    }

}

void calculateVoxelReliabilities() {
    int voxelNumber = 0;

    for (Voxel v : voxels) {
        if (v.value == v.value) {
            v.reliability = getReliability(voxelNumber);
        } else v.reliability = Float.NaN;
        voxelNumber++;
    }

}

float getReliability(int voxelNumber) {
    int tally = 0;
    double sum = 0;
    double penalty = 1;
    int[] coordinates = returnCoordinates(voxelNumber);
    int centerX = coordinates[0];
    int centerY = coordinates[1];
    double centerVal = voxels.get(voxelNumber).value;
    for (int d = 0; d < differences.length; d = d + 2) {
        int x1 = centerX + differences[d][0];
        int y1 = centerY + differences[d][1];
        int x2 = centerX + differences[d+1][0];
        int y2 = centerY + differences[d+1][1];
        int address1 = x1 + y1*width;
        int address2 = x2 + y2*width;

        double val1 = Double.NaN;
        if (address1 > 0 && address1 < voxels.size()) val1 = voxels.get(address1).value;
        double val2 = Double.NaN;
        if (address2 > 0 && address2 < voxels.size()) val2 = voxels.get(address2).value;
        double diff1 = (val1 - centerVal);
        double diff2 = (val2 - centerVal);
        if (val1 == val1 && val2 == val2) { //checks for NaN
            double factor  = Math.sqrt( Math.pow(diff1 + diff2 - 2*centerVal, 2) );
            sum += factor;
            tally++;
        } else if (val1 == val1) {
            double factor = Math.sqrt( Math.pow(diff1 - centerVal, 2));
            sum += factor;
            tally++;
        } else if (val2 == val2) {
            double factor = Math.sqrt( Math.pow(diff2 - centerVal, 2));
            sum += factor;
            tally++;
        }
    }
    float reliability = (float)(-sum);
    if (tally == 0) {
        reliability = Float.NaN;
    }
    return reliability;
}

/*
private static void writeReliabilityMap(NiftiVolume vol) {
    // THIS METHOD NOT CALLED
    // TO START CALLING IT AGAIN, NEEDS DIMENSIONS ADJUSTED
    double[][][] reliability = new double[width][height][depth][timeSteps];
    int index;
    final int area = width*height;
    final int volume = width*height*depth;
    for (int t = 0; t < timeSteps; t++) {
        for (int z = 0; z < depth; z++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    index = x + y*width + z*area + t*volume; 
                    reliability[x][y][z][t] = voxels.get(index).reliability;
                }
            }
        }
    }
    vol.data = new FourDimensionalArray(reliability);
    NiftiHeader hdr = vol.header;
    hdr.setDatatype(NiftiHeader.NIFTI_TYPE_FLOAT64);
    hdr.scl_slope = 0;
    try {
        vol.write("reliability.nii");
    } catch (Exception e) {
        e.printStackTrace();
    }
}
*/

void loadEdgesAndGroups() {
    for (int voxelNumber = 0; voxelNumber < voxels.size(); voxelNumber++) {
        int[] coordinates = returnCoordinates(voxelNumber);
        int x = coordinates[0];
        int y = coordinates[1];
        if (voxelNumber + 1 < voxels.size()) { 
            Edge xEdge = new Edge(voxelNumber, voxelNumber + 1, false);
            if (xEdge.reliability == xEdge.reliability) edges.add(xEdge);
        }
        if  (voxelNumber+width < voxels.size()) {
            Edge yEdge = new Edge(voxelNumber, voxelNumber + width, false);
            if (yEdge.reliability == yEdge.reliability) edges.add(yEdge);
        }
        groups.add( new Group(voxelNumber) );
        voxels.get(voxelNumber).group = voxelNumber;
    }

}

void mergeGroups(){
    Collections.sort(edges, new Comparator<Edge>() {
            @Override
            public int compare(Edge edge1, Edge edge2) {
                if (edge1.reliability < edge2.reliability) {
                    return 1;
					} else if (edge1.reliability > edge2.reliability) {
						return -1;
					} else return 0;
				}
			}
		);
		for ( Edge edge : edges ) {
			if (edge.reliability == edge.reliability) {
				int voxel1 = edge.voxel1;
				Voxel v1 = voxels.get(voxel1);
				int groupOne = v1.group;
				Group group1 = groups.get( groupOne );
				@SuppressWarnings("unused")
				int voxel2 = edge.voxel2;
				@SuppressWarnings("unused")
				Voxel v2 = voxels.get(edge.voxel2);
				int groupTwo = voxels.get(edge.voxel2).group;
				Group group2 = groups.get( groupTwo );
				if (groupOne != groupTwo) {
					if ( group1.voxelsInGroup.size() >= group2.voxelsInGroup.size() ) {
						int incrementAdj =
								voxels.get(edge.voxel1).increment -
								edge.increment -
								voxels.get(edge.voxel2).increment;
						group1.absorbGroup(group2, incrementAdj);
					} else {
						int incrementAdj =
								voxels.get(edge.voxel2).increment +
								edge.increment -
								voxels.get(edge.voxel1).increment;
						group2.absorbGroup(group1, incrementAdj);
					}
				} // if groups are different
			} // if reliability is above zero
		} // while edges PQ is not empty
	}

	double[][] getArray() {

		double[][] unwrap = new double[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int voxelNumber = x + y*width;
                int increment = voxels.get(voxelNumber).increment;
                float reliability = voxels.get(voxelNumber).reliability;
                //System.out.println(voxelNumber + " " + increment + " " + reliability);
                float value = (float)( voxels.get(voxelNumber).value + Math.PI*2*increment );
                unwrap[x][y] = value;
            } // for x
        } // for y
		return unwrap;
	}

	public double[][] unwrapPhase(double[][] phase, double incrementThreshold) {
		this.phase = phase;
        this.incrementThreshold = incrementThreshold; 
        initVariablesArray(); 
        loadVoxelsArray();
		calculateVoxelReliabilities();
		loadEdgesAndGroups();
		mergeGroups();
		double[][] unwrap = getArray();
		return unwrap;
	}

	public double[][] unwrapPhase(double[][] phase, double[][] magnitude, double incrementThreshold) {
		this.phase = phase;
        this.magnitude = magnitude;
		this.incrementThreshold = incrementThreshold;
		initVariablesArray();
		loadVoxelsMagnitude();
		calculateVoxelReliabilitiesMagnitude();
		loadEdgesAndGroups();
		mergeGroups();
		double[][] unwrap = getArray();
		return unwrap;
	}

	public double[][] unwrapPhase(double[][] phase, double[][] magnitude) {
		return unwrapPhase(phase, magnitude, INCREMENT_THRESHOLD);
	}

    static class ArgSet {
        String[] paths;
    }

    public static ArgSet parseArgs(String[] args) {
        Options options = new Options();
        options.addOption(new Option("i", "input",true, "Input nifti"));
        options.addOption(new Option("o", "output", true, "Output path (default same as input)"));
        options.addOption(new Option("m", "magnitude",true, "Magnitude path"));
        ArgSet as = new ArgSet();
        as.paths = new String[3];
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("i")) {
                as.paths[0] = cmd.getOptionValue("i");
            } else {
                as.paths[0] = null;
            }
            System.out.println("Input path: "+as.paths[0]);
            if (cmd.hasOption("o")) {
                as.paths[1] = cmd.getOptionValue("o");
            } else {
                //as.paths[1] = as.paths[0];
            }
            System.out.println("Output path: "+as.paths[1]);
            if (cmd.hasOption("m")) {
                as.paths[2] = cmd.getOptionValue("m");
            } else {
                as.paths[2] = null;
            }
            System.out.println("Magnitude path: "+as.paths[2]);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return as;
    }
        

    public static void main(String[] args) {
        ArgSet as = parseArgs(args);
        if (as.paths[0] == null) {
            System.out.println("Input filename required");
            return;
        }
        RG2D rg2d = new RG2D();
        try {
        NiftiVolume phaseVol = NiftiVolume.read(as.paths[0]);
        double[][][][] phase = phaseVol.data.toArray(); // NIFTI is 4D even with 3D data
        //normalize in 3D
        int fi = phase.length;
        int fj = phase[0].length;
        int fk = phase[0][0].length;
        int fl = phase[0][0][0].length;
        phase = ArrayMath.devectorize(
                    ArrayMath.multiply(
                        ArrayMath.normalize(
                            ArrayMath.vectorize(phase)
                            ),
                        2*Math.PI),
                    fi, fj, fk);
        phase = ArrayMath.shiftDimR(phase);
        if (as.paths[2].endsWith("nii") || as.paths[2].endsWith(".nii.gz")) {
            NiftiVolume magVol = NiftiVolume.read(as.paths[2]);
            double[][][][] magnitude = magVol.data.toArray();
            magnitude = ArrayMath.shiftDimR(ArrayMath.shiftDimR(ArrayMath.shiftDimR(magnitude)));
            magnitude = ArrayMath.normalize(ArrayMath.medianFilter3d(magnitude,2));
            for (int m = 0; m < fk; m++) {
                for (int n = 0; n < fl; n++) { //to be parallelized
                    phase[m][n] = rg2d.unwrapPhase(phase[m][n], magnitude[m][n], INCREMENT_THRESHOLD);
                }
            }
        } else {
            for (int m = 0; m < fk; m++) {
                for (int n = 0; n < fl; n++) { //to be parallelized
                    phase[m][n] = rg2d.unwrapPhase(phase[m][n], INCREMENT_THRESHOLD);
                }
            }
        }
        phase = ArrayMath.shiftDimL(ArrayMath.shiftDimL(phase));
        phaseVol.data = new FourDimensionalArray(phase);
        NiftiHeader phaseHdr = phaseVol.header;
        phaseHdr.setDatatype(NiftiHeader.NIFTI_TYPE_FLOAT64);
        phaseHdr.scl_slope = 0;
        phaseVol.write(as.paths[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
