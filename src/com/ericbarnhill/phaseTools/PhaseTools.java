package com.ericbarnhill.phaseTools;

import static java.util.concurrent.TimeUnit.SECONDS;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.process.FloatProcessor;
import ij.util.Java2;

import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;

public class PhaseTools extends javax.swing.JFrame {
	
	private static final long serialVersionUID = 1L;
	
	//DEFAULTS
	int mask = 50;
	int exclusion = 25;
	int volumeDepth = 0;
	int maxWindow = 0;
	double rgaThreshold = 3.14;
	double erodeThreshold = 6;
	boolean estimate = true;
	boolean rounded = false;
	boolean	volume = true;
	boolean temporal = true;
	boolean auto = false;
	int timeSteps = 8;
	int parallelImages = 3;
    java.awt.GridBagConstraints gbc;        
	
	//FIELDS
	javax.swing.JComboBox imageList, maskList;
	javax.swing.JTextField maskSeedTextField, exclusionSeedTextField, volumeDepthTextField, rgaThresholdTextField; 
	javax.swing.JTextField timeStepsTextField, parallelTextField, maxWindowTextField, thresholdTextField;
	javax.swing.JCheckBox estimateCheckBox, roundedCheckBox, twoDCheckBox, threeDCheckBox, volumeCheckBox, temporalCheckBox, autoCheckBox;
	    
  
    ImageStack[] unwrappedPartitionArray;
    
    public PhaseTools() {
        Java2.setSystemLookAndFeel();
        initWindow();
        imagePanel();
        preProcessingPanel();
        unwrappingPanel();
        utilitiesPanel();
        updateFileList(maskList);
    }
    
    private void updateFileList(JComboBox cb){
    	//SafeScheduledService timer = new SafeScheduledService(1);
    	ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
    	timer.scheduleAtFixedRate(new Runnable() {
    		@Override
 	    	 public void run() {
    			try {
	 	    	 int[] ids=WindowManager.getIDList();
	 	         int n= WindowManager.getImageCount();
	 	         if (n > 0){
	 	        	for (int i = 1; i < imageList.getItemCount(); i++) {
	 	        		boolean correspondingWindow = false;
	            		 String itemTitle = (String)imageList.getItemAt(i).toString();
		 	             for (int j = 0; j < WindowManager.getImageCount() ; j++){
		 	            	 String windowTitle = WindowManager.getImage(ids[j]).getTitle();	
		 	            	 if ( windowTitle.equals(itemTitle) ) correspondingWindow = true;
		 	             }
		 	            if (!correspondingWindow) {
		 	            	try {
		 	            		imageList.removeItemAt(i); 
		 	            		maskList.removeItemAt(i); 
		 	            	} catch (Throwable t) {
		 	            		System.out.println("i is "+i);
		 	            	}
		 	            }
	            	 }
	 	             for (int i = 0 ; i < WindowManager.getImageCount() ; i++){
	 	            	 boolean inList = false;
	 	            	 String windowTitle = WindowManager.getImage(ids[i]).getTitle();
	 	            	 for (int j = 0; j < imageList.getItemCount(); j++) {
	 	            		 String itemTitle = (String)imageList.getItemAt(j).toString();
	 	            		 if ( windowTitle.equals(itemTitle) ) inList = true;
	 	            	 }
	 	            	 if (!inList) {
	 	            		 imageList.addItem(windowTitle);
	 	            		 maskList.addItem(windowTitle);
	 	            	 }
	 	             }
	 	         } 
    			} catch (Throwable t) {
	 	        	 t.printStackTrace();
	 	         }
    		}
 	       }, 1, 1, SECONDS);
    	return;
    }
    
    
    //INITIALIZE THE GUI
    
    private void initWindow() {
    	
        getContentPane().setLayout(new java.awt.GridBagLayout());
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PhaseTools for ImageJ");
        return;
    }
    
    private void imagePanel() {
    	
        javax.swing.JPanel imagePanel = new javax.swing.JPanel();
        imagePanel.setLayout(new java.awt.GridBagLayout());
        imagePanel.setBorder(new javax.swing.border.TitledBorder("Image"));

	    javax.swing.JLabel imageLabel = new javax.swing.JLabel();
        imageLabel.setText("Image To Unwrap: ");
        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 0;
	        gbc.gridy = 0;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.EAST;
		    gbc.insets = new java.awt.Insets(6, 6, 12, 15);
        imagePanel.add(imageLabel, gbc);

        imageList = new javax.swing.JComboBox();
        imageList.setEditable(false);
        imageList.setMaximumRowCount(5);
        imageList.setPreferredSize(new java.awt.Dimension(250, 25));
        imageList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Image Selected" }));
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 1;
	        gbc.gridy = 0;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 2;
	        gbc.gridheight = 1;
	    gbc.anchor = java.awt.GridBagConstraints.WEST;
	    gbc.insets = new java.awt.Insets(6, 6, 12, 15);
        imagePanel.add(imageList, gbc);
        
        javax.swing.JLabel volumeDepthLabel = new javax.swing.JLabel();
        volumeDepthLabel.setText("Volume Depth (1 for 2D):");
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 0;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.EAST;
		    gbc.insets = new java.awt.Insets(6, 6, 12, 15);
	    imagePanel.add(volumeDepthLabel, gbc);
        
        volumeDepthTextField = new javax.swing.JTextField();
        volumeDepthTextField.setPreferredSize(new java.awt.Dimension(30, 20));
        volumeDepthTextField.setText(Integer.toString(volumeDepth));
        volumeDepthTextField.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	volumeDepth = getInteger(volumeDepthTextField);
	            }
	        });
        volumeDepthTextField.addFocusListener(new java.awt.event.FocusAdapter() {
	            public void focusLost(java.awt.event.FocusEvent evt) {
	            	volumeDepth = getInteger(volumeDepthTextField);
	            }
	        });
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 1;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
		    gbc.insets = new java.awt.Insets(6, 6, 12, 15);
	    imagePanel.add(volumeDepthTextField, gbc);
	    
	    javax.swing.JLabel timeStepsLabel = new javax.swing.JLabel();
        timeStepsLabel.setText("Time Steps (1 for single volume):");
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 2;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.EAST;
		    gbc.insets = new java.awt.Insets(6, 6, 12, 15);
	    imagePanel.add(timeStepsLabel, gbc);
        
        timeStepsTextField = new javax.swing.JTextField();
        timeStepsTextField.setPreferredSize(new java.awt.Dimension(30, 20));
        timeStepsTextField.setText(Integer.toString(timeSteps));
        timeStepsTextField.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	timeSteps = getInteger(timeStepsTextField);
	            }
	        });
        timeStepsTextField.addFocusListener(new java.awt.event.FocusAdapter() {
	            public void focusLost(java.awt.event.FocusEvent evt) {
	            	timeSteps = getInteger(timeStepsTextField);
	            }
	        });
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 3;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
		    gbc.insets = new java.awt.Insets(6, 6, 12, 15);
	    imagePanel.add(timeStepsTextField, gbc);
	    
	    javax.swing.JLabel parallelLabel = new javax.swing.JLabel();
        parallelLabel.setText("Images In Parallel:");
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 4;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.EAST;
		    gbc.insets = new java.awt.Insets(6, 6, 12, 15);
	    imagePanel.add(parallelLabel, gbc);
        
        parallelTextField = new javax.swing.JTextField();
        parallelTextField.setPreferredSize(new java.awt.Dimension(30, 20));
        parallelTextField.setText(Integer.toString(parallelImages));
        parallelTextField.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	parallelImages = getInteger(parallelTextField);
	            }
	        });
        parallelTextField.addFocusListener(new java.awt.event.FocusAdapter() {
	            public void focusLost(java.awt.event.FocusEvent evt) {
	            	parallelImages = getInteger(parallelTextField);
	            }
	        });
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 5;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
		    gbc.insets = new java.awt.Insets(6, 6, 12, 15);
	    imagePanel.add(parallelTextField, gbc);
        
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(6, 12, 6, 12);
        getContentPane().add(imagePanel, gbc);
	    
        return;
    }
    
    void preProcessingPanel() {
               
        javax.swing.JPanel preProcessingPanel = new javax.swing.JPanel();
        preProcessingPanel.setLayout(new java.awt.GridBagLayout());
        preProcessingPanel.setBorder(new javax.swing.border.TitledBorder("Pre-Processing"));
        
        
        javax.swing.JButton automaskAndNormalizeButton = new javax.swing.JButton();
        automaskAndNormalizeButton.setText("Automask and Normalize");
        automaskAndNormalizeButton.setPreferredSize(new java.awt.Dimension(250, 30));
	        automaskAndNormalizeButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                automaskAndNormalize(evt, maskList);
	            }
	        });

	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 0;
	        gbc.gridy = 0;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 2;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(12, 6, 18, 3);
        preProcessingPanel.add(automaskAndNormalizeButton, gbc);
        
        javax.swing.JButton justAutomaskButton = new javax.swing.JButton();
        justAutomaskButton.setText("Just Automask");
        justAutomaskButton.setPreferredSize(new java.awt.Dimension(250, 30));
	        justAutomaskButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                justAutomask(evt, maskList);
	            }
	        });

	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 2;
	        gbc.gridy = 0;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 2;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(12, 6, 18, 3);
        preProcessingPanel.add(justAutomaskButton, gbc);
        
        javax.swing.JButton justNormalizeButton = new javax.swing.JButton();
        justNormalizeButton.setText("Just Normalize");
        justNormalizeButton.setPreferredSize(new java.awt.Dimension(250, 30));
	        justNormalizeButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                justNormalize(evt, maskList);
	            }
	        });

	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 4;
	        gbc.gridy = 0;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 2;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(12, 6, 18, 3);
        preProcessingPanel.add(justNormalizeButton, gbc);
        
        javax.swing.JPanel secondPreProcPanel = new javax.swing.JPanel();
        secondPreProcPanel.setLayout(new java.awt.GridBagLayout());
        
        javax.swing.JLabel maskSeedLabel = new javax.swing.JLabel();
        maskSeedLabel.setText("Mask seed: ");
        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 0;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(0, 30, 18, 15);
	    secondPreProcPanel.add(maskSeedLabel, gbc);
        
        maskSeedTextField = new javax.swing.JTextField();
        //maskSeedTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        maskSeedTextField.setPreferredSize(new java.awt.Dimension(30, 20));
        maskSeedTextField.setText(Integer.toString(mask));
	        maskSeedTextField.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	mask = getInteger(maskSeedTextField);
	            }
	        });
	        maskSeedTextField.addFocusListener(new java.awt.event.FocusAdapter() {
	            public void focusLost(java.awt.event.FocusEvent evt) {
	            	mask = getInteger(maskSeedTextField);
	            }
	        });
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 1;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(0, 0, 18, 30);
	    secondPreProcPanel.add(maskSeedTextField, gbc);
        
        javax.swing.JLabel exclusionSeedLabel = new javax.swing.JLabel();
        exclusionSeedLabel.setPreferredSize(new java.awt.Dimension(120, 20));
        exclusionSeedLabel.setText("Exclusion seed: ");
        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 2;
	        gbc.gridy = 1;
	        gbc.weightx = 2;
	        gbc.weighty = 2;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(0, 0, 18, 0);
	    secondPreProcPanel.add(exclusionSeedLabel, gbc);

        exclusionSeedTextField = new javax.swing.JTextField();
        //exclusionSeedTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        exclusionSeedTextField.setPreferredSize(new java.awt.Dimension(30, 20));
        exclusionSeedTextField.setText(Integer.toString(exclusion));
	        exclusionSeedTextField.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	exclusion = getInteger(exclusionSeedTextField);
	            }
	        });
	        exclusionSeedTextField.addFocusListener(new java.awt.event.FocusAdapter() {
	            public void focusLost(java.awt.event.FocusEvent evt) {
	            	exclusion = getInteger(exclusionSeedTextField);
	            }
	        });
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 3;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(0, 0, 18, 20);
	    secondPreProcPanel.add(exclusionSeedTextField, gbc);
	    
	    javax.swing.JLabel maskStackLabel = new javax.swing.JLabel();
        maskStackLabel.setText("Mask image stack: ");
        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 4;
	        gbc.gridy = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(0, 0, 18, 15);
	    secondPreProcPanel.add(maskStackLabel, gbc);

        maskList = new javax.swing.JComboBox();
        maskList.setEditable(false);
        maskList.setMaximumRowCount(5);
        maskList.setPreferredSize(new java.awt.Dimension(300, 25));
        maskList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Image Selected" }));
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 5;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.EAST;
	        gbc.insets = new java.awt.Insets(0, 0, 18, 30);
	     secondPreProcPanel.add(maskList, gbc);
      
	     
	     gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 0;
	        gbc.gridy = 2;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 6;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        //gbc.insets = new java.awt.Insets(0, 0, 0, 30);
	     preProcessingPanel.add(secondPreProcPanel, gbc);
	     

	     
	     
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(6, 12, 6, 12);
        getContentPane().add(preProcessingPanel, gbc);
        
        return;
    }
    
    void unwrappingPanel() {
        
        javax.swing.JPanel unwrappingPanel = new javax.swing.JPanel();
        unwrappingPanel.setLayout(new java.awt.GridBagLayout());
        unwrappingPanel.setBorder(new javax.swing.border.TitledBorder("Unwrapping Methods"));
        

        javax.swing.JButton laplacianButton = new javax.swing.JButton();
        laplacianButton.setText("Laplacian-Based Estimate");
        laplacianButton.setPreferredSize(new java.awt.Dimension(250, 30));
	        laplacianButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                laplacianUnwrap();
	            }
	        });

	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 0;
	        gbc.gridy = 0;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 2;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(3, 6, 6, 3);
        unwrappingPanel.add(laplacianButton, gbc);
        
        javax.swing.JButton regionGrowingButton = new javax.swing.JButton();
        regionGrowingButton.setText("Region Growing Unwrap");
        regionGrowingButton.setPreferredSize(new java.awt.Dimension(250, 30));
	        regionGrowingButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                regionGrowingUnwrap();
	            }
	        });

	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 2;
	        gbc.gridy = 0;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 2;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(3, 6, 6, 3);
        unwrappingPanel.add(regionGrowingButton, gbc); 
        
        javax.swing.JButton leastPhaseButton = new javax.swing.JButton();
        leastPhaseButton.setText("Dilate-Erode-Propagate");
        leastPhaseButton.setPreferredSize(new java.awt.Dimension(250, 30));
	        leastPhaseButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                propagationErosionUnwrap();
	            }
	        });

	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 4;
	        gbc.gridy = 0;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 2;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(3, 6, 6, 3);
        unwrappingPanel.add(leastPhaseButton, gbc);
        
	    javax.swing.JLabel maxWindowLabel = new javax.swing.JLabel();
        maxWindowLabel.setText("Max Window: ");
        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 4;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.EAST;
	        gbc.insets = new java.awt.Insets(3, 6, 18, 3);
        unwrappingPanel.add(maxWindowLabel, gbc);
        
        maxWindowTextField = new javax.swing.JTextField();
        //maxWindowTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        maxWindowTextField.setPreferredSize(new java.awt.Dimension(30, 20));
        maxWindowTextField.setText(Integer.toString(maxWindow));
	        maxWindowTextField.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	maxWindow = getInteger(maxWindowTextField);
	            }
	        });
	        maxWindowTextField.addFocusListener(new java.awt.event.FocusAdapter() {
	            public void focusLost(java.awt.event.FocusEvent evt) {
	            	maxWindow = getInteger(maxWindowTextField);
	            }
	        });
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 5;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(3, 6, 18, 3);
	    unwrappingPanel.add(maxWindowTextField, gbc);
	    
	    /**
	    javax.swing.JLabel thresholdLabel = new javax.swing.JLabel();
        thresholdLabel.setText("Gradient Threshold: ");
        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 4;
	        gbc.gridy = 2;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.EAST;
	        gbc.insets = new java.awt.Insets(3, 6, 18, 3);
        unwrappingPanel.add(thresholdLabel, gbc);
        
        
        thresholdTextField = new javax.swing.JTextField();
        //maxWindowTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        thresholdTextField.setPreferredSize(new java.awt.Dimension(30, 20));
        thresholdTextField.setText(Double.toString(erodeThreshold));
	        thresholdTextField.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	erodeThreshold = getDouble(thresholdTextField);
	            }
	        });
	        thresholdTextField.addFocusListener(new java.awt.event.FocusAdapter() {
	            public void focusLost(java.awt.event.FocusEvent evt) {
	            	erodeThreshold = getDouble(thresholdTextField);
	            }
	        });
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 5;
	        gbc.gridy = 2;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(3, 6, 18, 3);
	    unwrappingPanel.add(thresholdTextField, gbc);
		*/
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(6, 12, 6, 12);
        getContentPane().add(unwrappingPanel, gbc);
        
        return;
    }
    
    void utilitiesPanel() {
        
        javax.swing.JPanel utilitiesPanel = new javax.swing.JPanel();
        utilitiesPanel.setLayout(new java.awt.GridBagLayout());
        utilitiesPanel.setBorder(new javax.swing.border.TitledBorder("Selection-Based Utilities"));
        
        javax.swing.JButton addTwoPiButton = new javax.swing.JButton();
        addTwoPiButton.setText("Add 2 \u03C0");
        addTwoPiButton.setPreferredSize(new java.awt.Dimension(200, 25));
        addTwoPiButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                addTwoPi();
	            }
	        });

	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 0;
	        gbc.gridy = 0;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(3, 6, 6, 3);
        utilitiesPanel.add(addTwoPiButton, gbc);
        
        javax.swing.JButton subtractTwoPiButton = new javax.swing.JButton();
        subtractTwoPiButton.setText("Subtract 2 \u03C0");
        subtractTwoPiButton.setPreferredSize(new java.awt.Dimension(200, 25));
        subtractTwoPiButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                subtractTwoPi();
	            }
	        });

	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 1;
	        gbc.gridy = 0;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(3, 6, 6, 3);
        utilitiesPanel.add(subtractTwoPiButton, gbc);
        
        javax.swing.JButton zeroToNaNButton = new javax.swing.JButton();
        zeroToNaNButton.setText("Zero to NaN");
        zeroToNaNButton.setPreferredSize(new java.awt.Dimension(200, 25));
        zeroToNaNButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                zeroToNaN();
	            }
	        });
	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 0;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(3, 6, 6, 3);
        utilitiesPanel.add(zeroToNaNButton, gbc);
        
        javax.swing.JButton naNToZero = new javax.swing.JButton();
        naNToZero.setText("NaN to Zero");
        naNToZero.setPreferredSize(new java.awt.Dimension(200, 25));
        naNToZero.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                naNToZero();
	            }
	        });

	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 1;
	        gbc.gridy = 1;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(3, 6, 6, 3);
        utilitiesPanel.add(naNToZero, gbc);
        
        javax.swing.JButton fillWithNaNButton = new javax.swing.JButton();
        fillWithNaNButton.setText("Fill with NaN");
        fillWithNaNButton.setPreferredSize(new java.awt.Dimension(200, 25));
        fillWithNaNButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                fillWithNaN();
	            }
	        });

	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 0;
	        gbc.gridy = 2;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(3, 6, 6, 3);
        utilitiesPanel.add(fillWithNaNButton, gbc);
        
        javax.swing.JButton fillOutsideNaNButton = new javax.swing.JButton();
        fillOutsideNaNButton.setText("Fill Outside NaN");
        fillOutsideNaNButton.setPreferredSize(new java.awt.Dimension(200, 25));
        fillOutsideNaNButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                fillOutsideNaN();
	            }
	        });

	        gbc = new java.awt.GridBagConstraints();
	        gbc.gridx = 1;
	        gbc.gridy = 2;
	        gbc.weightx = 1;
	        gbc.weighty = 1;
	        gbc.gridwidth = 1;
	        gbc.gridheight = 1;
	        gbc.anchor = java.awt.GridBagConstraints.WEST;
	        gbc.insets = new java.awt.Insets(3, 6, 6, 3);
        utilitiesPanel.add(fillOutsideNaNButton, gbc);

        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        //gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(6, 12, 6, 12);
        getContentPane().add(utilitiesPanel, gbc);
        
        pack();
        
    }
    
    private void automaskAndNormalize(java.awt.event.ActionEvent evt, javax.swing.JComboBox cb) {
    	if ( imageSelectError() || volumeError() ) return;
    	IJ.showStatus("Automask and Normalize");
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
    	ImagePlus maskImages = WindowManager.getImage( (String)maskList.getSelectedItem().toString() );
    	Automasker automasker = new Automasker();
    	try {
    		ImagePlus binaryMask = automasker.getBinaryMask(maskImages, phaseImages, mask, exclusion, volumeDepth);
    		ImagePlus result = automasker.applyBinaryMask(phaseImages, binaryMask);
			binaryMask.setTitle("Binary Mask");
			binaryMask.show();
			result.setTitle("Masked Image");
			result.show();
	    	StackNormalizer stackNormalizer = new StackNormalizer();
	    	ImagePlus normalizedImages = stackNormalizer.normalizeStack(result);
	    	normalizedImages.setTitle("Normalized Image");
	    	normalizedImages.setDisplayRange(0, 2*Math.PI);
	    	normalizedImages.show();
    	} catch (NullPointerException e) {
    		IJ.error("Please select a mask");
    		return;
    	}

    	resetIndices();
    	IJ.run("Tile");
    }
    
    private void justAutomask(java.awt.event.ActionEvent evt, javax.swing.JComboBox cb) {
    	if ( imageSelectError() || volumeError() ) return;
    	IJ.showStatus("Automask");
    	
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
    	ImagePlus maskImages = WindowManager.getImage( (String)maskList.getSelectedItem().toString() );
    	Automasker automasker = new Automasker();
    	try {
	    	ImagePlus binaryMask = automasker.getBinaryMask(maskImages, phaseImages, mask, exclusion, volumeDepth);
			ImagePlus result = automasker.applyBinaryMask(phaseImages, binaryMask);
			binaryMask.setTitle("Binary Mask");
			binaryMask.show();
			result.setTitle("Masked Image");
			result.show();
	    } catch (NullPointerException e) {
			IJ.error("Please select a mask");
			return;
		}
		resetIndices();
    	IJ.run("Tile");
    }
    
    private void justNormalize(java.awt.event.ActionEvent evt, JComboBox cb) {
    	if ( imageSelectError() || volumeError() ) return;
    	IJ.showStatus("Normalize");
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
    	StackNormalizer stackNormalizer = new StackNormalizer();
    	ImagePlus normalizedImages = stackNormalizer.normalizeStack(phaseImages);
    	normalizedImages.setTitle("Normalized Image");
    	normalizedImages.setDisplayRange(0, 2*Math.PI);
    	normalizedImages.show();
    	resetIndices();
    	IJ.run("Tile");
    }
    
    private void laplacianUnwrap() {
    	long startingTime = System.currentTimeMillis();
    	if ( imageSelectError() || volumeError() ) return;
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
		LaplacianUnwrapper4D lbe = new LaplacianUnwrapper4D();
		ImagePlus unwrappedImages = lbe.unwrapImage(phaseImages, volumeDepth, timeSteps, parallelImages);
		unwrappedImages.show();
		resetIndices();
    	IJ.run("Tile");
    	double elapsedTime =  ( System.currentTimeMillis() - startingTime )/1000.0;
    	System.out.println("Elapsed time - LBE Unwrap: "+elapsedTime);
    }
    
    
    private void regionGrowingUnwrap() {
    	
    	long startingTime = System.currentTimeMillis();
    	if ( imageSelectError() || volumeError() ) return;
    	IJ.showStatus("Region Growing Unwrap");
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
    	RG4D rga = new RG4D(); // was RGA4D2
    	// ImagePlus unwrappedImages = rga.unwrapImage(phaseImages, volumeDepth, timeSteps, parallelImages, rgaThreshold);
		// unwrappedImages.show();	
    	
    	resetIndices();
    	IJ.run("Tile");
    	double elapsedTime =  ( System.currentTimeMillis() - startingTime )/1000.0;
    	System.out.println("Elapsed time - RG Unwrap: "+elapsedTime);
    	
    }

    private void propagationErosionUnwrap() {
    	 
    	if ( imageSelectError() || volumeError() ) return;
    	IJ.showStatus("Dilate-Erode-Propagate Unwrap");
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
    	DilateErode propagationErosion = new DilateErode();
    	
    	boolean[] mask = new boolean[phaseImages.getWidth()*phaseImages.getHeight()];
    	for (int i = 0; i < mask.length; i++) mask[i] = true;
    	Roi ROI = phaseImages.getRoi();
    	if (ROI != null) {
    		for (int row = 0; row < phaseImages.getHeight(); row++) {
				for (int column = 0; column < phaseImages.getWidth(); column++) {
					if (!ROI.contains(column, row) ) {
						mask[column + row*phaseImages.getWidth()] = false;
					}
				}
    		}
    	}
    	
    	long startingTime = System.currentTimeMillis();
    	ImagePlus unwrappedImages = propagationErosion.unwrapImage(phaseImages, volumeDepth, timeSteps, parallelImages, 
    			maxWindow, erodeThreshold, mask);
    	unwrappedImages.show();
    	resetIndices();
    	IJ.run("Tile");
    	double elapsedTime =  ( System.currentTimeMillis() - startingTime )/1000.0;
    	System.out.println("Elapsed time - DE Unwrap: "+elapsedTime);
    	
    }
    
    private void addTwoPi() {
    	
    	if ( imageSelectError() ) return;
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
    	Roi ROI = phaseImages.getRoi();
    	try {
			for (int row = 0; row < phaseImages.getHeight(); row++) {
				for (int column = 0; column < phaseImages.getWidth(); column++) {
					if (ROI.contains(column, row) ) {
						float value = phaseImages.getProcessor().getPixelValue(column, row);
						float valuePlusTwoPi = (float)( value+Math.PI*2 );
						phaseImages.getProcessor().putPixelValue(column, row, valuePlusTwoPi);
					} //if contains
				} // for x
			} //for y
			phaseImages.updateAndDraw();
    	} catch (NullPointerException e) {
	    	for (int row = 0; row < phaseImages.getHeight(); row++) {
				for (int column = 0; column < phaseImages.getWidth(); column++) {
					float value = phaseImages.getProcessor().getPixelValue(column, row);
					float valuePlusTwoPi = (float)( value+Math.PI*2 );
					phaseImages.getProcessor().putPixelValue(column, row, valuePlusTwoPi);
				}
	    	}
		}
    	
    }
    
    private void subtractTwoPi() {
    	
    	if ( imageSelectError() ) return;
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
    	Roi ROI = phaseImages.getRoi();
    	try {
			for (int row = 0; row < phaseImages.getHeight(); row++) {
				for (int column = 0; column < phaseImages.getWidth(); column++) {
					if (ROI.contains(column, row) ) {
						float value = phaseImages.getProcessor().getPixelValue(column, row);
						float valueMinusTwoPi = (float)( value-Math.PI*2 );
						phaseImages.getProcessor().putPixelValue(column, row, valueMinusTwoPi);
					} //if contains
				} // for x
			} //for y
			phaseImages.updateAndDraw();
	    } catch (NullPointerException e) {
	    	for (int row = 0; row < phaseImages.getHeight(); row++) {
				for (int column = 0; column < phaseImages.getWidth(); column++) {
					float value = phaseImages.getProcessor().getPixelValue(column, row);
					float valueMinusTwoPi = (float)( value-Math.PI*2 );
					phaseImages.getProcessor().putPixelValue(column, row, valueMinusTwoPi);
				}
	    	}
		}
    	
    }
    
    private void zeroToNaN() {
    	
    	if ( imageSelectError() ) return;
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
    	for (int slice = 1; slice <= phaseImages.getImageStackSize(); slice++) {
			for (int row = 0; row < phaseImages.getProcessor().getHeight(); row++) {
				for (int column = 0; column < phaseImages.getProcessor().getWidth(); column++) {
					if ( phaseImages.getStack().getProcessor(slice).getPixelValue(column, row) == 0) {
						phaseImages.getStack().getProcessor(slice).putPixelValue(column, row, Float.NaN);
					}
				}
			}
		}
    	
    }
    
    private void naNToZero() {    	
    	
    	if ( imageSelectError() ) return;
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
    	for (int slice = 1; slice <= phaseImages.getImageStackSize(); slice++) {
			for (int row = 0; row < phaseImages.getProcessor().getHeight(); row++) {
				for (int column = 0; column < phaseImages.getProcessor().getWidth(); column++) {
					if (  Float.isNaN( phaseImages.getStack().getProcessor(slice).getPixelValue(column, row) )  ) {
						phaseImages.getStack().getProcessor(slice).putPixelValue(column, row, 0);
					}
				}
			}
		}
    	
    }
    
    private void fillWithNaN() {
    	
    	if ( imageSelectError() ) return;
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
    	Roi ROI = phaseImages.getRoi();
    	try {
    		for (int slice = 1; slice <= phaseImages.getImageStackSize(); slice++) {
				for (int row = 0; row < phaseImages.getHeight(); row++) {
					for (int column = 0; column < phaseImages.getWidth(); column++) {
						if (ROI.contains(column, row) ) {
							phaseImages.getStack().getProcessor(slice).putPixelValue(column, row, Float.NaN);
						} //if contains
					} // for x
				} //for y
    		}
			phaseImages.updateAndRepaintWindow();
	    } catch (NullPointerException e) {
			IJ.error("No ROI Selected");
			return;
		}
    	
    }
    
    private void fillOutsideNaN() {
    	
    	if ( imageSelectError() ) return;
    	ImagePlus phaseImages =  WindowManager.getImage( (String)imageList.getSelectedItem().toString() );
    	Roi ROI = phaseImages.getRoi();
    	try {
    		for (int slice = 1; slice <= phaseImages.getImageStackSize(); slice++) {
    			for (int row = 0; row < phaseImages.getHeight(); row++) {
					for (int column = 0; column < phaseImages.getWidth(); column++) {
						if (!ROI.contains(column, row) ) {
							phaseImages.getStack().getProcessor(slice).putPixelValue(column, row, Float.NaN);
						} //if contains
					} // for x
    			} //for y
    		}
			phaseImages.updateAndDraw();
    	} catch (NullPointerException e) {
    		IJ.error("No ROI Selected");
    		return;
    	}
    	
    }
    	
    private boolean volumeError() {
    	if (volumeDepth == 0) {
        	IJ.error("Please set a volume depth");
    		return true;
    	} else return false;
    }
    
    private boolean imageSelectError() {
    	if ( imageList.getSelectedIndex() == 0) {
    		IJ.error("Please select an image");
    		return true;
    	} else return false;
    }
    
    private void resetIndices() {
    	maskList.setSelectedIndex(0);
    	imageList.setSelectedIndex(0);
    }
    
    private int getInteger(JTextField textField){
        int val=(  new Integer( textField.getText() )  ).intValue();
        return val;      
    }
    
    private double getDouble(JTextField textField){
        double val=(  new Double( textField.getText() )  ).doubleValue();
        return val;      
    }
  	

    
}

