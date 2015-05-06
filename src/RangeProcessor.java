import java.util.LinkedList;

/* Class for processing ranges in parameters, e.g. [4:6]
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

public class RangeProcessor {
	
	/* Main method for processing ranges of parameters
	 * Input: Linked List of dataset objects
	 * Output: none
	 */
	public void processRanges(LinkedList<Dataset> dlist) {
		//for each dataset, process classifiers
		for(int i = 0; i < dlist.size(); i++) {
			processClassifiers(dlist.get(i));
		}
	}
	
	/* Detects the first range parameter in a set of parameters and
	 * expands it out.
	 * Input: a classifier object
	 * Output: array of string objects containing the parameter index and
	 * the expanded parameter values
	 */
	public String[] detectRange(Classifier classifier) {
		String[] results;
		
		//for each parameter in classifier, try and find one with a range
		for(int i = 0; i < classifier.getParamList().size(); i++) {
			//if we have a parameter that's value is of [num1:num2]
			if(classifier.getParamList().get(i).getValue().matches("[\\[][\\s]*[\\d\\.]+[\\s]*[:][\\s]*[\\d\\.]+[\\s]*[\\]]")) {
				//split on :
				 String[] splitted = classifier.getParamList().get(i).getValue().split(":");
				 
				 //remove brackets
				 splitted[0] = splitted[0].substring(1);
				 splitted[1] = splitted[1].substring(0, splitted[1].indexOf("]"));
				 
				 //remove excess whitespace
				 splitted[0] = splitted[0].trim();
				 splitted[1] = splitted[1].trim();
				 
				 //create array of floats
				 float[] temp = new float[3];
				 
				 //parse numbers, and set step size to 1
				 temp[0] = Float.parseFloat(splitted[0]);
				 temp[1] = 1;
				 temp[2] = Float.parseFloat(splitted[1]);
				 
				 //determine how big the array has to be
				 //by checking how many numbers we have
				 int counter = 0;
				 for (float j = temp[0]; j <= temp[2]; j+= temp[1]) {
					 counter++;
				 }
				 
				 //create results string 
				 results = new String[counter+1];
				 results[0] = Integer.toString(i); //parameter number
				 counter = 1;
				 for (float j = temp[0]; j <= temp[2]; j+= temp[1]) {
					 if(j % 1 == 0.0) { //if integer, parse to int
						 results[counter] = Integer.toString((int) j);
					 }
					 else { //if float, parse to float
						 results[counter] = Float.toString(j);
					 }
					 counter++;
				 }
				 return results;
			}
			//paramater range of form [num1:stepsize:num2]
			else if(classifier.getParamList().get(i).getValue().matches("[\\[][\\s]*[\\d\\.]+[\\s]*[:][\\s]*[\\d\\.]+[\\s]*[:][\\s]*[\\d\\.]+[\\s]*[\\]]")) {
				//split on :
				String[] splitted = classifier.getParamList().get(i).getValue().split(":");
				
				//remove brackets
				 splitted[0] = splitted[0].substring(1);
				 splitted[2] = splitted[2].substring(0, splitted[2].indexOf("]"));
				 
				 //remove whitespace
				 splitted[0] = splitted[0].trim();
				 splitted[1] = splitted[1].trim();
				 splitted[2] = splitted[2].trim();
				 
				 //parse all 3 numbers to floats
				 float[] temp = new float[3];
				 temp[0] = Float.parseFloat(splitted[0]);
				 temp[1] = Float.parseFloat(splitted[1]);
				 temp[2] = Float.parseFloat(splitted[2]);
				 
				 //determine how big array should be
				 int counter = 0;
				 for (float j = temp[0]; j <= temp[2]; j+= temp[1]) {
					 counter++;
				 }
				 
				 //create array
				 results = new String[counter+1];
				 results[0] = Integer.toString(i); //set param number
				 counter = 1;

				 for (float j = temp[0]; j <= temp[2]; j+= temp[1]) {
					 if(j % 1 == 0.0) { //if integer, parse to int
						 results[counter] = Integer.toString((int) j);
					 }
					 else { //if float, parse to float
						 results[counter] = Float.toString(j);
					 }
					 counter++;
				 }
				 return results;
			}
			//param of form [a,b,c,d] etc - comma sep values
			else if(classifier.getParamList().get(i).getValue().contains(",")) {
				//split on commas
				String[] splitted = classifier.getParamList().get(i).getValue().split(",");
				
				//remove brackets
				splitted[0] = splitted[0].substring(1);
				splitted[splitted.length-1] = splitted[splitted.length-1].substring(0, splitted[splitted.length-1].length()-1);
				
				//create array
				results = new String[splitted.length+1];
				results[0] = Integer.toString(i); //add param number
				for(int j = 0; j < splitted.length; j++) {
					results[j+1] = splitted[j];
				}
				return results;
			}
		}
		return null;
	}
	
	/* Process all the range parameters for each classifier in a dataset
	 * Input: a dataset object
	 * Output: none.
	 */
	public void processClassifiers(Dataset dataset) {
		int i = 0;
		
		//for each classifier
		while(i < dataset.getNumClassifiers()) {
			//remove the classifier from the dataset list
			Classifier tempClassifier = dataset.getClassList().remove(i);
			
			//check for ranges, and process them if exist
			String[] results = detectRange(tempClassifier);
			
			//if there were no ranges, re-add the classifier to the dataset list
			if(results == null) {
				dataset.getClassList().add(i, tempClassifier);
				i++;
			}
			else { //there were ranges
				//get param number
				int paramNumber = Integer.parseInt(results[0]);
				int additionNumber = 0; //to ensure classifiers stay in order
				
				//loop over all results entries
				for(int j = 1; j < results.length; j++) {
					//create a temp classifier
					Classifier t = new Classifier(tempClassifier);
					
					//get the parameter number and set its value to a single number
					t.getParamList().get(paramNumber).setValue(results[j]);
					
					//add classifier to dataset list
					if(i+additionNumber >= dataset.getNumClassifiers()) {
						dataset.getClassList().add(t);
					}
					else{
						dataset.getClassList().add(i+additionNumber, t);
					}
					additionNumber++;
				}
			}
				
			
		}
	}
	
	
	
	
	
	
	
/*
	public void processRanges(LinkedList<Dataset> dlist) {
		for(int i = 0; i < dlist.size(); i++) {
			int size = dlist.get(i).getClassList().size();
			LinkedList<Classifier> clist = dlist.get(i).getClassList();
			for(int j = 0; j < size; j++) {
				LinkedList<Parameter> plist = clist.get(j).getParamList();
				float[] details = findRangeParam(0, plist);
				if(details != null) {
					Classifier temp = clist.remove(j);
					iterateParam(details, temp, dlist, i);
				}
			}
		}
	}
	
	public void iterateParam(float[] details, Classifier classifier, LinkedList<Dataset> dlist, int datasetnum) {
		for (float i = details[1]; i <= details[3]; i+= details[2]) {
			Classifier t = new Classifier(classifier);
			t.getParamList().get((int)details[0]).setValue(Float.toString(i));
			float[] temp = findRangeParam((int)details[0]+1, classifier.getParamList());
			if(temp != null) {
				iterateParam(temp, t, dlist, datasetnum);
			}
			else {
				dlist.get(datasetnum).getClassList().add(t);
			}
		}
	}
	
	public float[] findRangeParam(int startIndex, LinkedList<Parameter> plist) {
		float[] results = new float[4];
		for(int i = startIndex; i < plist.size(); i++) {
			if(plist.get(i).getValue().matches("[\\[][\\s]*[\\d\\.]+[\\s]*[:][\\s]*[\\d\\.]+[\\s]*[\\]]")) {
				 String[] splitted = plist.get(i).getValue().split(":");
				 splitted[0] = splitted[0].substring(1);
				 splitted[1] = splitted[1].substring(0, splitted[1].indexOf("]"));
				 splitted[0] = splitted[0].trim();
				 splitted[1] = splitted[1].trim();
				 
				 results[0] = i;
				 results[1] = Float.parseFloat(splitted[0]);
				 results[2] = 1;
				 results[3] = Float.parseFloat(splitted[1]);
				 return results;
			}
			else if(plist.get(i).getValue().matches("[\\[][\\s]*[\\d\\.]+[\\s]*[:][\\s]*[\\d\\.]+[\\s]*[:][\\s]*[\\d\\.]+[\\s]*[\\]]")) {
				String[] splitted = plist.get(i).getValue().split(":");
				 splitted[0] = splitted[0].substring(1);
				 splitted[2] = splitted[2].substring(0, splitted[2].indexOf("]"));
				 splitted[0] = splitted[0].trim();
				 splitted[1] = splitted[1].trim();
				 splitted[2] = splitted[2].trim();
				 
				 results[0] = i;
				 results[1] = Float.parseFloat(splitted[0]);
				 results[2] = Float.parseFloat(splitted[1]);
				 results[3] = Float.parseFloat(splitted[2]);
				 return results;
			}
		}
		return null;
	}
*/
}
