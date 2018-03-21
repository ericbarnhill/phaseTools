package varianceMeasurements;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Gradient_Variance implements PlugIn {

	ImagePlus unwrappedImage, trueImage;
	double[] amplitude, wavenumber, noise;
	int[] varianceArray, normVarianceArray;
	String[] CSVLines;
	double variance;
	FileWriter fw;
	PrintWriter pw;
	int volumeDepth, tally, volumes, width, height, area;
	ArrayList<Double> exact, unwrap;
	double amp;


	@Override
	public void run(java.lang.String arg) {
		
		initFields();
		loadArrays();
		writeVariance();
		IJ.error("All Done");
		return;
		
	}
	
	void initFields() {
		unwrappedImage = WindowManager.getImage("unwrap");
		trueImage = WindowManager.getImage("true");
		
		volumeDepth = 10;
		volumes = (int)(  unwrappedImage.getImageStackSize() / ( volumeDepth * 1.0)  );
		CSVLines = new String[volumes];
		varianceArray = new int[volumes];
		normVarianceArray = new int[volumes];
		width = unwrappedImage.getWidth();
		height = unwrappedImage.getHeight();
		area = height*width;
		
		try {
			fw = new FileWriter("/home/ericbarnhill/Documents/code/varianceResults.csv",true);
			pw = new PrintWriter(fw);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		amplitude = new double[]{2, 4, 6, 8, 10, 12, 14, 16};
		wavenumber = new double[] {32, 64, 96, 128, 160, 192, 224, 256};
		noise = new double[] {1, 2, 3, 4, 5};
	}
	
	void loadArrays() {
		tally = 0;
		double partitionVariance = 0;
		double partitionNormVariance = 0;
		for (double amp : amplitude) {
			
			this.amp = amp;
			
			for (double wav: wavenumber) {
				for (double noi: noise) {
					
					exact = new ArrayList<Double>();
					unwrap = new ArrayList<Double>();
					partitionVariance = 0;					
					partitionNormVariance = 0;


					for (int d = 0; d < volumeDepth; d++) {
						
						float[] truePixels = (float[])trueImage.getStack().getProcessor(  ( tally*volumeDepth+d+1 )  ).getPixels();
						float[] unwrappedPixels = (float[])unwrappedImage.getStack().getProcessor(  ( tally*volumeDepth+d+1 )  ).getPixels();
						
						for (int p = 0; p < truePixels.length; p++) {
							exact.add( new Double(truePixels[p]) );
							unwrap.add( new Double(unwrappedPixels[p]) );
						}
					
					}
					
					partitionVariance += calculateVariance(false);
					partitionNormVariance += calculateVariance(true);
					
					varianceArray[tally] = (int)Math.round(partitionVariance);
					normVarianceArray[tally] = (int)Math.round(partitionNormVariance);
					CSVLines[tally] = amp+","+wav+","+noi;
					tally++;
					
				}
			}
		}
		
	}
	
	double calculateVariance(boolean normalise) {
		
		double partitionVariance = 0;
		
		for (int n = 0; n < exact.size(); n++) {
			try {
				double exactPhaseVar = getPhaseVariance(n, "Exact", normalise);
				double unwrapPhaseVar = getPhaseVariance(n, "Unwrap", normalise);
				double difference = exactPhaseVar - unwrapPhaseVar;
				partitionVariance += Math.pow(difference, 2);
			} catch (Throwable t) {}
		}
		
		return partitionVariance;
	}
	
	double getPhaseVariance(int n, String type, boolean normalise) throws Throwable {
		
		double factor;
		
		double phaseVariance = 0;
		if (normalise) {
			factor = amp*2*Math.PI;
		} else {
			factor = 1;
		}
		
		int[] varianceNegatives = new int[] {n-area, n-width, n-1};
		int[] variancePositives = new int[]	{n+area, n+width, n+1};
		
		if ( type.equals("Exact") ) {
			for (int i: varianceNegatives) phaseVariance -= exact.get(i) / factor;
			for (int i: variancePositives) phaseVariance += exact.get(i) / factor;
		} else if ( type.equals("Unwrap") ) {
			for (int i: varianceNegatives) phaseVariance -= unwrap.get(i) / factor;
			for (int i: variancePositives) phaseVariance += unwrap.get(i) / factor;
		}
		
		return phaseVariance;
		
	}
	
	
	
	void writeVariance() {
		pw.println("Block,Variance,NormVariance,Amp,WavNum,Noise");
		for (int n = 0; n < tally; n++) {
			String variance = String.format("%d", varianceArray[n]);
			String normVariance = String.format("%d", normVarianceArray[n]);
			pw.println(n+","+variance+","+normVariance+","+CSVLines[n]);
		}
		pw.flush();
	}
	
}

