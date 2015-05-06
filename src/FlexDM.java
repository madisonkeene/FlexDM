import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

/* Main class for running FlexDM project
 * Parses XML, creates appropriate directories for file storage, 
 * runs each classifier as a separate thread.
 * All results stored in Results folder.
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

public class FlexDM {
	//Semaphore for writing to summary file
	public static Semaphore writeFile = new Semaphore(1);


	public static void main(String args[]) {
		//Check that weka.jar is linked to project properly
		try {
			Class.forName("weka.core.Instances");
		}
		catch(ClassNotFoundException e) {
			System.err.println("The file weka.jar is missing");
			System.exit(1);
		}

		//Check that weka.jar is linked to project properly
		File f = new File("flexdm.dtd");
		if(!f.exists()) {
			System.err.println("The file flexdm.dtd is missing");
			System.exit(1);
		}

		//Create parser
		XMLParser p = new XMLParser();
		LinkedList<Dataset> myData = null;
		
		//Get name of XML file and parse the file
		if(args.length != 0) {
			myData = p.parseDocument(args[0]);
		}
		else { //XML file not specified
			System.err.println("The XML file has not been specified.");
			System.exit(1);
		}
		
		//If the XML parser errored and somehow made it back here, exit
		if(myData == null) {
			System.exit(1);
		}
		
		//Process the ranges in the parameter elements
		RangeProcessor x = new RangeProcessor();
		x.processRanges(myData);
		
		//Run the experiments
		runExperiments(myData);
	}

	/* Method to train/evaluate each classifier on data
	 * Inputs: a linked list of dataset objects
	 * Outputs: none
	 */
	private static void runExperiments(LinkedList<Dataset> dlist) {
		int countNum = 1;
		
		//Semaphore to control number of active experiments
		Semaphore s = new Semaphore(Math.max(1, Runtime.getRuntime().availableProcessors()-1));

		//Set up parent results directory
		String parentDir = "Results";
		File theDir = new File(parentDir);
		
		//If the directory doesnt exist, create it
		if(!theDir.exists()) { 
			theDir.mkdir();
		}
		else { //Directory already exists- create new one with number.
			int temp = 1;
			parentDir = "Results" + "(" + temp + ")";
			theDir = new File(parentDir);
			
			//Keep trying to create directory with new number until successful
			while(theDir.exists()) { 
				temp++;
				parentDir = "Results" + "(" + temp + ")";
				theDir = new File(parentDir);
			}

			theDir.mkdir();
		}

		//create summary file (CSV)
		File file = new File(parentDir + "/results_summary.txt");
		PrintWriter p;
		try {
			p = new PrintWriter(file);
			p.write("dataset, classifier, parameters, accuracy(% correct)\r\n");
			p.close();
		} catch (FileNotFoundException e1) {
			System.err.println("FATAL ERROR OCCURRED: Could not create summary file");
			System.exit(1);
		}

		//For each dataset
		for(int i = 0; i < dlist.size(); i++) {	
			//Get the dataset, get the classifiers to run on that data
			Dataset dtemp = dlist.get(i);
			LinkedList<Classifier> clist = dtemp.getClassList();
			
			//Check the file exists
			File check = new File(dtemp.getName());
			boolean fileExists = check.exists();
			
			//Get and check test method for output purposes
			String testOpt = dtemp.getTest();
			if(dtemp.getTest().contains("testset")) {
				testOpt = "testset " + dtemp.getTest().substring(dtemp.getTest().lastIndexOf("\\") + 1);
			}
			
			//Dataset exists
			if(fileExists) {
				//Create new results directory for this dataset & test combo
				try { 
					theDir = new File(parentDir + "/Results--" + dtemp.getName().substring(dtemp.getName().lastIndexOf("\\") + 1) + "--" + testOpt);
				}
				catch (Exception e){
					theDir = new File(parentDir + "/Results--" + dtemp.getName() + "--" + testOpt);
				}
				
				//if directory doesnt already exist, create it
				if(!theDir.exists()) {
					theDir.mkdir();
				}
				else { //directory already exists
					//keep trying to create numbered directory until successful
					int temp = 1;
					try {
						theDir = new File(parentDir + "/Results--" + dtemp.getName().substring(dtemp.getName().lastIndexOf("\\") + 1) + "--" + testOpt + "(" + temp + ")");
					}
					catch (Exception e){
						theDir = new File(parentDir + "/Results--" + dtemp.getName() + "--" + testOpt + "(" + temp + ")");
					}
					while(theDir.exists()) {
						temp++;
						try {
							theDir = new File(parentDir + "/Results--" + dtemp.getName().substring(dtemp.getName().lastIndexOf("\\") + 1) + "--" + testOpt + "(" + temp + ")");
						}
						catch (Exception e){
							theDir = new File(parentDir + "/Results--" + dtemp.getName() + "--" + testOpt + "(" + temp + ")");
						}
					}

					theDir.mkdir();
				}

				//set directory of the dataset
				dtemp.setDir(theDir.getAbsolutePath());

				//output that the experiment is starting
				String dataname[] = {theDir.getName().substring(0,theDir.getName().lastIndexOf("--")), theDir.getName().substring(theDir.getName().lastIndexOf("--")+2)};
				if(dataname.length == 3) {
					int len = ("STARTING EXPERIMENT ON DATASET " + dataname[0].substring(9).trim() + " WITH TEST " + dataname[1].trim() + " " + dataname[2].trim() +" USING " + s.availablePermits() + " CPU CORES").length();
					String headFoot = ""; //create line of ----- for pretty output
					for (int k = 0; k < len; k++) {
						headFoot += "-";
					}
					System.out.println(headFoot);
					System.out.println("STARTING EXPERIMENT ON DATASET " + dataname[0].substring(9).trim() + " WITH TEST " + dataname[1].trim() + " " + dataname[2].trim() +" USING " + s.availablePermits() + " CPU CORES");
					System.out.println(headFoot);
				}
				else {
					if(dataname[1].contains("(")) {
						int len = ("STARTING EXPERIMENT ON DATASET " + dataname[0].substring(9).trim() + " WITH TEST " + dataname[1].substring(0, dataname[1].indexOf("(")).trim() + " USING " + s.availablePermits() + " CPU CORES").length();
						String headFoot = ""; //create line of ----- for pretty output
						for (int k = 0; k < len; k++) {
							headFoot += "-";
						}
						System.out.println(headFoot);
						System.out.println("\nSTARTING EXPERIMENT ON DATASET " + dataname[0].substring(9).trim() + " WITH TEST " + dataname[1].substring(0, dataname[1].indexOf("(")).trim() + " USING " + s.availablePermits() + " CPU CORES\n");
						System.out.println(headFoot);
					}
					else {
						int len = ("STARTING EXPERIMENT ON DATASET " + dataname[0].substring(9).trim() + " WITH TEST " + dataname[1].trim() + " USING " + s.availablePermits() + " CPU CORES").length();
						String headFoot = ""; //create line of ----- for pretty output
						for (int k = 0; k < len; k++) {
							headFoot += "-";
						}
						System.out.println(headFoot);
						System.out.println("STARTING EXPERIMENT ON DATASET " + dataname[0].substring(9).trim() + " WITH TEST " + dataname[1].trim() + " USING " + s.availablePermits() + " CPU CORES");
						System.out.println(headFoot + "\n");
					}
				}

				//for each classifier
				for(int j = 0; j < clist.size(); j++) {

					try { //get a permit
						s.acquire();
					} 
					catch (InterruptedException e) { //bad things happened
						System.err.println("FATAL ERROR OCCURRED. PROGRAM WILL NOW EXIT.");
						System.exit(1);
					}

					//get classifier, create thread, directory and run
					Classifier ctemp = clist.get(j);
					FlexDMThread mythread = new FlexDMThread(dtemp, ctemp, s, countNum, file);

					File newDir = new File(dtemp.getDir() + "/" + ctemp.getName());
					ctemp.setDirName(newDir.getName());
					if(!newDir.exists()) {
						newDir.mkdir();
					}
					countNum++;
					mythread.start();
				}
			}
			else {
				System.err.println("Dataset " + dtemp.getName() + " cannot be found.");
				System.exit(1);
			}
		}

		//while experiments are running, just wait.
		while(s.availablePermits() != Math.max(1, Runtime.getRuntime().availableProcessors()-1)) {}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			//this is never going to happen
		}
		
		//All done
		System.out.println("\n-------------------");
		System.out.println("FINISHED EXPERIMENT");
		System.out.println("-------------------");
	}

}
