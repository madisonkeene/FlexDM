import java.io.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;
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
    public static Semaphore writeLog = new Semaphore(1);
    public static Semaphore getMainData = new Semaphore(1);
    public static Semaphore getTestData = new Semaphore(1);
    public static final String LOG_NAME = "log.txt";

	public static void main(String args[]) {
		int numcores = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        String customResults = "Results";
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
		if(args.length == 1) {
			myData = p.parseDocument(args[0]);
		}
		else if(args.length == 2) {
			myData = p.parseDocument(args[0]);
            try {
                numcores = Integer.parseInt(args[1]);
            }
            catch(NumberFormatException e) {
                customResults = args[1];
            }
		}
        else if (args.length == 3) {
            myData = p.parseDocument(args[0]);
            try {
                numcores = Integer.parseInt(args[1]);
                customResults = args[2];
            }
            catch(NumberFormatException e) {
                try {
                    numcores = Integer.parseInt(args[2]);
                    customResults = args[1];
                }
                catch(NumberFormatException e1) {
                    System.err.println("Invalid command line arguments.");
                    System.exit(1);
                }
            }

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

        //check if log file exists
        File logFile = new File(LOG_NAME);
        LogFile logFile_obj = null;
        boolean fileExists = logFile.exists();
        boolean cont = false;
        if(fileExists) {
            //if exists, load, process, open in append mode
            logFile_obj = new LogFile();
            logFile_obj.processFile(logFile);
            File folder = new File(logFile_obj.getResultsFolder());
            boolean folder_exists = folder.exists();
            if(folder_exists) {
                Scanner in = new Scanner(System.in);
                if (logFile_obj.getEntries().size() != 0) {
                    int len = logFile_obj.getEntries().size();
                    System.out.println("Previous experiment found: Last run: " + logFile_obj.getEntries().get(len - 1).getDatetime());
                } else {
                    System.out.println("Previous experiment found: Last run: " + logFile_obj.getInitDatetime());
                }
                System.out.println("XML file: " + logFile_obj.getXmlFileName() + " Results folder: " + logFile_obj.getResultsFolder());
                System.out.println("Would you like to continue previous experiment? (enter y to continue, any key otherwise): ");
                String s = in.nextLine();
                if (s.trim().equalsIgnoreCase("y")) {
                    cont = true;
                } else {
                    logFile_obj = null;
                    deleteFile(LOG_NAME);
                }
            }
            else {
                fileExists = false;
                deleteFile(LOG_NAME);
                logFile_obj = null;
            }
        }
        if(!fileExists || !cont){
            //if doesnt exist, create new file & open & write info
            PrintWriter pw;
            try {
                pw = new PrintWriter(logFile);
                pw.write(args[0] + "\n");
                pw.close();
            } catch (FileNotFoundException e1) {
                System.err.println("FATAL ERROR OCCURRED: Could not create log file");
                System.exit(1);
            }
        }

		//Run the experiments
		runExperiments(myData, numcores, logFile, logFile_obj, customResults);
	}

	/* Method to train/evaluate each classifier on data
	 * Inputs: a linked list of dataset objects
	 * Outputs: none
	 */
	private static void runExperiments(LinkedList<Dataset> dlist, int numcores, File logFile, LogFile logFile_obj, String customResults) {
		int countNum = 1;

        // Instantiate a Date object
        Date date = new Date();
        Format formatter = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss");
        //formatter.format(date)

		//Semaphore to control number of active experiments
		Semaphore s = new Semaphore(Math.min(numcores, Math.max(1, Runtime.getRuntime().availableProcessors() - 1)));
        String parentDir;
        File summaryFile;
        if(logFile_obj == null) {
            parentDir = createResultsDir(customResults);
            summaryFile = createSummaryFile(parentDir);
            try {
                PrintWriter p = new PrintWriter(new FileWriter(logFile, true));
                p.write(parentDir + "\n");
                p.write(formatter.format(date) + "\n");
                p.close();
            } catch (IOException e) {
                System.err.println("FATAL ERROR OCCURRED: Could not write to log file");
                System.exit(1);
            }
        }
        else {
            parentDir = logFile_obj.getResultsFolder();
            summaryFile = new File(parentDir + "/results_summary.txt");
        }

        File resultsDir;


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
                //open data files
                dtemp.openDataFile();
                dtemp.openTestFile();

                //create results folder for dataset and test options
                resultsDir = createDatasetResultsFolder(parentDir, dtemp, testOpt);

                //set directory of the dataset
				dtemp.setDir(resultsDir.getAbsolutePath());

                //TODO: REFACTOR THIS CODE!
				//output that the experiment is starting
				String dataname[] = {resultsDir.getName().substring(0,resultsDir.getName().lastIndexOf("--")), resultsDir.getName().substring(resultsDir.getName().lastIndexOf("--")+2)};
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
                    //check things here
                    if(logFile_obj == null || !classifierDone(logFile_obj.getEntries(), dtemp, ctemp)) {
                        FlexDMThread mythread = new FlexDMThread(dtemp, ctemp, s, countNum, summaryFile, logFile);

                        File newDir = new File(dtemp.getDir() + "/" + ctemp.getName());
                        ctemp.setDirName(newDir.getName());
                        if(!newDir.exists()) {
                            newDir.mkdir();
                        }
                        countNum++;
                        mythread.start();
                    }
                    else {
                        s.release();
                    }
				}
			}
			else {
				System.err.println("Dataset " + dtemp.getName() + " cannot be found.");
				System.exit(1);
			}
		}

		//while experiments are running, just wait.
		while(s.availablePermits() != Math.min(numcores, Math.max(1,Runtime.getRuntime().availableProcessors() - 1))) {}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			//this is never going to happen
		}
		
		//All done
		System.out.println("\n-------------------");
		System.out.println("FINISHED EXPERIMENT");
		System.out.println("-------------------");

        deleteFile(LOG_NAME);
	}

    private static boolean classifierDone(ArrayList<LogEntry> entries, Dataset dataset, Classifier classifier) {
        //Process hyperparameters for classifier
        String param_string = "";
        for (int i = 0; i < classifier.getNumParams(); i++) {
            param_string += classifier.getParameter(i).getName();
            param_string+= " ";
            if(classifier.getParameter(i).getValue() != null) {
                param_string += classifier.getParameter(i).getValue();
                param_string+= " ";
            }
        }
        if(param_string.equals("")) { //no parameters
            param_string = "no_parameters";
        }

        for(int i = 0; i < entries.size(); i++) {
            LogEntry ent = entries.get(i);
            if(dataset.getName().equals(ent.getDataset())){
                if(dataset.getTest().equals(ent.getTestopt())) {
                    if(dataset.getResult_string().equals(ent.getResultopt())) {
                        if(classifier.getName().equals(ent.getClassifier())) {
                            if(param_string.trim().equals(ent.getParameters().trim())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Deletes an individual file if it exists
     * @param filename The name of the file to delete
     */
    private static void deleteFile(String filename) {
        //check file exists
        File file = new File(filename);
        boolean exists = file.exists();

        //delete file
        if(exists) {
            file.delete();
        }
    }

    private static File createDatasetResultsFolder(String parentDir, Dataset dtemp, String testOpt) {
        File theDir;//Create new results directory for this dataset & test combo
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
//        else { //directory already exists
//            //keep trying to create numbered directory until successful
//            int temp = 1;
//            try {
//                theDir = new File(parentDir + "/Results--" + dtemp.getName().substring(dtemp.getName().lastIndexOf("\\") + 1) + "--" + testOpt + "(" + temp + ")");
//            }
//            catch (Exception e){
//                theDir = new File(parentDir + "/Results--" + dtemp.getName() + "--" + testOpt + "(" + temp + ")");
//            }
//            while(theDir.exists()) {
//                temp++;
//                try {
//                    theDir = new File(parentDir + "/Results--" + dtemp.getName().substring(dtemp.getName().lastIndexOf("\\") + 1) + "--" + testOpt + "(" + temp + ")");
//                }
//                catch (Exception e){
//                    theDir = new File(parentDir + "/Results--" + dtemp.getName() + "--" + testOpt + "(" + temp + ")");
//                }
//            }
//
//            theDir.mkdir();
//        }
        return theDir;
    }

    private static File createSummaryFile(String parentDir) {
        //create summary file (CSV)
        File file = new File(parentDir + "/results_summary.csv");
        PrintWriter p;
        try {
            p = new PrintWriter(file);
            p.write("Key_Dataset, Key_Scheme, Key_Scheme_options, Number_correct, " +
                    "Number_incorrect, Number_unclassified, Percent_correct, Percent_incorrect, " +
                    "Percent_unclassified, Kappa_statistic, Mean_absolute_error, Root_mean_squared_error, " +
                    "Relative_absolute_error, Root_relative_squared_error, SF_prior_entropy, SF_scheme_entropy, " +
                    "SF_entropy_gain, SF_mean_prior_entropy, SF_mean_scheme_entropy, SF_mean_entropy_gain, " +
                    "KB_information, KB_mean_information, KB_relative_information, Weighted_avg_true_positive_rate, " +
                    "Weighted_avg_false_positive_rate, Weighted_avg_true_negative_rate, Weighted_avg_false_negative_rate, " +
                    "Weighted_avg_IR_precision, Weighted_avg_IR_recall, Weighted_avg_F_measure, Weighted_avg_area_under_ROC\n");
            p.close();
        } catch (FileNotFoundException e1) {
            System.err.println("FATAL ERROR OCCURRED: Could not create summary file");
            System.exit(1);
        }
        return file;
    }

    private static String createResultsDir(String parentDir) {
        //Set up parent results directory

        File theDir = new File(parentDir);
        String parentDir_temp = parentDir;
        //If the directory doesnt exist, create it
        if(!theDir.exists()) {
            theDir.mkdir();
        }
        else { //Directory already exists- create new one with number.
            int temp = 1;
            parentDir_temp = parentDir + "(" + temp + ")";
            theDir = new File(parentDir_temp);

            //Keep trying to create directory with new number until successful
            while(theDir.exists()) {
                temp++;
                parentDir_temp = parentDir + "(" + temp + ")";
                theDir = new File(parentDir_temp);
            }

            theDir.mkdir();
        }
        return parentDir_temp;
    }

}
