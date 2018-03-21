package varianceMeasurements;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.commons.math4.stat.descriptive.moment.Mean;
import org.apache.commons.math4.stat.descriptive.rank.Median;
import org.apache.commons.math4.stat.descriptive.rank.Min;

public class Value_Variance implements PlugIn {

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
		Min min = new Min();

		
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
						
						double[] trueDuplicate = new double[truePixels.length];
						double[] unwrappedDuplicate = new double[unwrappedPixels.length];
						for (int i = 0; i < trueDuplicate.length; i++) trueDuplicate[i] = truePixels[i];
						for (int i = 0; i < unwrappedDuplicate.length; i++) unwrappedDuplicate[i] = unwrappedPixels[i];
						Mean mean = new Mean();
						double trueMean = mean.evaluate(trueDuplicate);
						double unwrappedMean = mean.evaluate(unwrappedDuplicate);
						double difference = unwrappedMean - trueMean;
						
						
						for (int p = 0; p < truePixels.length; p++) {
							exact.add( new Double(truePixels[p]) );
							unwrap.add( new Double(unwrappedPixels[p]) - difference);
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
		double factor;
		
		if (normalise) {
			factor = amp*2*Math.PI;
		} else {
			factor = 1;
		}

		for (int n = 0; n < exact.size(); n++) {
			try {
				double difference = (exact.get(n) - unwrap.get(n) ) / factor ; 
				partitionVariance += Math.pow(difference, 2);
			} catch (Throwable t) {}
		}
		
		return partitionVariance;
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

