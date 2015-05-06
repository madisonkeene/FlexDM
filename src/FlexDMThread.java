import java.util.Random;
import java.util.concurrent.Semaphore;
import java.io.*;
import weka.core.Range;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

/* Class for each classifier's thread.
 * Will create, train and evaluate classifier, writing results to text file 
 * and updating the summary file.
 * 
 * Copyright (C) 2014  Madison Flannery
 * madison.flannery@uon.edu.au
 * 
 * This file is part of FlexDM.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

public class FlexDMThread extends Thread{
	private Dataset dataset; //Dataset
	private Classifier classifier; //Classifier
	private Semaphore s; //To control threads executing
	private int cNum; //Classifier number, for output
	private File summary; //Summary file to write to
	
	//Constructor
	public FlexDMThread(Dataset dtemp, Classifier ctemp, Semaphore s, int cNum, File file) {
		dataset = dtemp;
		classifier = ctemp;
		this.s = s;
		this.cNum = cNum;
		this.summary = file;
	}

	//Run the thread
	public void run () {
		try{
			//Get the data from the source
			DataSource source = new DataSource(dataset.getName());
			Instances data = source.getDataSet();
			
			//Set class attribute if undefined
			if (data.classIndex() == -1) {
				data.setClassIndex(data.numAttributes() - 1);
			}

			//Process hyperparameters for classifier
			String temp = "";
			for (int i = 0; i < classifier.getNumParams(); i++) {
				temp += classifier.getParameter(i).getName();
				temp+= " ";
				if(classifier.getParameter(i).getValue() != null) {
					temp += classifier.getParameter(i).getValue();
					temp+= " ";
				}
			}
			
			String[] options = weka.core.Utils.splitOptions(temp);
			
			//Print to console- experiment is starting
			if(temp.equals("")) { //no parameters
				temp = "results_no_parameters";
				try{
					System.out.println("STARTING CLASSIFIER " + cNum + " - " + classifier.getName() + " on dataset " + dataset.getName().substring(dataset.getName().lastIndexOf("\\")+1) + " with no parameters");
				}
				catch (Exception e) {
					System.out.println("STARTING CLASSIFIER " + cNum + " - " + classifier.getName() + " on dataset " + dataset.getName() + " with no parameters");
				}
			}
			else { //parameters
				try {
					System.out.println("STARTING CLASSIFIER " + cNum + " - " + classifier.getName() + " on dataset " + dataset.getName().substring(dataset.getName().lastIndexOf("\\")+1) + " with parameters " + temp);
				}
				catch (Exception e){
					System.out.println("STARTING CLASSIFIER " + cNum + " - " + classifier.getName() + " on dataset " + dataset.getName() + " with parameters " + temp);
				}
			}
			
			//Create classifier, setting parameters
			weka.classifiers.Classifier x = createObject(classifier.getName());
			x.setOptions(options);
			x.buildClassifier(data);
			
			//Process the test selection
			String[] tempTest = dataset.getTest().split("\\s");
			
			//Create evaluation object for training and testing classifiers
			Evaluation eval = new Evaluation(data);
			StringBuffer predictions = new StringBuffer();
			
			//Train and evaluate classifier
			if(tempTest[0].equals("testset")) { //specified test file
				//Build classifier
				x.buildClassifier(data);
				
				//Open test file, load data
				DataSource testFile = new DataSource(dataset.getTest().substring(7).trim());
				Instances testSet = testFile.getDataSet();
				
				//Set class attribute if undefined
				if (testSet.classIndex() == -1) {
					testSet.setClassIndex(testSet.numAttributes() - 1);
				}
				
				//Evaluate model
				Object[] array = {predictions, new Range(), new Boolean(true)};
				eval.evaluateModel(x, testSet, array);
			}
			else if(tempTest[0].equals("xval")) { //Cross validation
				//Build classifier
				x.buildClassifier(data);
				
				//Cross validate
				eval.crossValidateModel(x, data, Integer.parseInt(tempTest[1]), new Random(1), predictions, new Range(), true);
			}
			else if(tempTest[0].equals("leavexval")) { //Leave one out cross validation
				//Build classifier
				x.buildClassifier(data);
				
				//Cross validate
				eval.crossValidateModel(x, data, data.numInstances()-1, new Random(1), predictions, new Range(), true);
			}
			else if(tempTest[0].equals("percent")) { //Percentage split of single data set
				//Set training and test sizes from percentage
				int trainSize = (int) Math.round(data.numInstances() * Double.parseDouble(tempTest[1])); 
				int testSize = data.numInstances() - trainSize;
				
				//Load specified data
				Instances train = new Instances(data, 0, trainSize);
				Instances testSet = new Instances(data, trainSize, testSize);
				
				//Build classifier
				x.buildClassifier(train);
				
				//Train and evaluate model
				Object[] array = {predictions, new Range(), new Boolean(true)};
				eval.evaluateModel(x, testSet, array);
			}
			else { //Evaluate on training data
				//Test and evaluate model
				Object[] array = {predictions, new Range(), new Boolean(true)};
				eval.evaluateModel(x, data, array);
			}
			
			//create datafile for results
			String filename = dataset.getDir() + "/" + classifier.getDirName() + "/" + temp + ".txt";		
			PrintWriter writer = new PrintWriter(filename, "UTF-8");
			
			//Print classifier, dataset, parameters info to file
			try {
				writer.println("CLASSIFIER: " + classifier.getName() + "\n DATASET: " + dataset.getName() + "\n PARAMETERS: " + temp);
			}
			catch (Exception e) {
				writer.println("CLASSIFIER: " + classifier.getName() + "\n DATASET: " + dataset.getName() + "\n PARAMETERS: " + temp);
			}
			
			//Add evaluation string to file
			writer.println(eval.toSummaryString());
			
			//Process result options
			if(checkResults("stats")) { //Classifier statistics
				writer.println(eval.toClassDetailsString());
			}
			if(checkResults("model")) { //The model
				writer.println(x.toString());
			}
			if(checkResults("matrix")) { //Confusion matrix
				writer.println(eval.toMatrixString());
			}
			if(checkResults("entropy")) { //Entropy statistics
				//Set options req'd to get the entropy stats
				String[] opt = new String[4];
				opt[0] = "-t";
				opt[1] = dataset.getName(); 
				opt[2] = "-k";
				opt[3] = "-v";
				
				//Evaluate model
				String entropy = Evaluation.evaluateModel(x, opt);
				
				//Grab the relevant info from the results, print to file
				entropy = entropy.substring(entropy.indexOf("=== Stratified cross-validation ===") + 35, entropy.indexOf("=== Confusion Matrix ==="));
				writer.println("=== Entropy Statistics ===");
				writer.println(entropy);
			}
			if(checkResults("predictions")) { //The models predictions
				writer.println("=== Predictions ===\n");
				if(!dataset.getTest().contains("xval")) { //print header of predictions table if req'd
					writer.println(" inst#     actual  predicted error distribution ()"); 
				}
				writer.println(predictions.toString()); //print predictions to file
			}
			
			writer.close();
			
			//Summary file is semaphore controlled to ensure quality
			try { //get a permit
				//grab the summary file, write the classifiers details to it
				FlexDM.writeFile.acquire();
				PrintWriter p = new PrintWriter(new FileWriter(summary, true));
				if(temp.equals("results_no_parameters")) { //change output based on parameters
					temp = temp.substring(8);
				}
				
				//write percent correct, classifier name, dataset name to summary file
				p.write(dataset.getName() + ", " + classifier.getName() + ", "  + temp + ", " + eval.pctCorrect() + "\r\n");
				p.close();
				
				//release semaphore
				FlexDM.writeFile.release();
			} 
			catch (InterruptedException e) { //bad things happened
				System.err.println("FATAL ERROR OCCURRED: Classifier: " + cNum + " - " + classifier.getName() + " on dataset " + dataset.getName());
			}
			
			//output we have successfully finished processing classifier
			if(temp.equals("no_parameters")) { //no parameters
				try {
					System.out.println("FINISHED CLASSIFIER " + cNum + " - " + classifier.getName() + " on dataset " + dataset.getName().substring(dataset.getName().lastIndexOf("\\")+1) + " with no parameters");
				}
				catch (Exception e) {
					System.out.println("FINISHED CLASSIFIER " + cNum + " - " + classifier.getName() + " on dataset " + dataset.getName() + " with no parameters");
				}
			}
			else { //with parameters
				try {
					System.out.println("FINISHED CLASSIFIER " + cNum + " - " + classifier.getName() + " on dataset " + dataset.getName().substring(dataset.getName().lastIndexOf("\\")+1) + " with parameters " + temp);
				}
				catch (Exception e) {
					System.out.println("FINISHED CLASSIFIER " + cNum + " - " + classifier.getName() + " on dataset " + dataset.getName() + " with parameters " + temp);
				}
			}
		
			s.release();
			
		}
		catch(Exception e) {
			//an error occurred
			System.err.println("FATAL ERROR OCCURRED: " + e.toString() + "\nClassifier: " + cNum + " - " + classifier.getName() + " on dataset " + dataset.getName());
			s.release();
		}
		

	}

	/* Method for creating classifier objects based on specified name
	 * Using java reflection
	 * Author: Madison Flannery, madison.flannery@uon.edu.au
	 */
	public static weka.classifiers.Classifier createObject(String className) {
		try {
			Class c = Class.forName(className);
			weka.classifiers.Classifier newObject = (weka.classifiers.Classifier)c.newInstance();
			return newObject;
		} 
		catch (Exception e) {
			//this will be caught in thread where method is called
		}
		return null;
	}
	
	/* Check if the result string is in the XML for this classifier
	 * input: results string
	 * output: true if the results string appears in XML for classifier, false otherwise
	 */
	public boolean checkResults(String comp) {
		for (int i = 0; i < dataset.getResults().length; i++) {
			if(dataset.getResults()[i].equalsIgnoreCase(comp)) {
				return true;
			}
		}
		return false;
	}
	
}
