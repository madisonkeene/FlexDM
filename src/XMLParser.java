import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/* Class for parsing the XML file and storing data
 * as a number of Dataset, Classifier and Parameter objects
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


public class XMLParser extends DefaultHandler{
	LinkedList<Dataset> datasets; //The list of datasets to return
	int numDatasets; //Number of datasets counted
	
	//Temp variables for parsing each XML tag
	Dataset tempDataset; 
	Classifier tempClassifier;
	
	//Constructor
	public XMLParser() {
		numDatasets = 0;
		datasets = new LinkedList<Dataset>(); 
		tempDataset = null;
		tempClassifier = null;
	}
	
	/* Main parse method for the XML document
	 * Input: file name as a string
	 * Output: A linked list containing dataset objects
	 */
	public LinkedList<Dataset> parseDocument(String file) {
		//Create factory
		SAXParserFactory factory = SAXParserFactory.newInstance();
		
		//Enable validation
		factory.setValidating(true);
		try {
			//Create parser and error handler
			SAXParser parser = factory.newSAXParser();
			DefaultHandler h = new ErrorHandler();
			
			//check XML is valid
			parser.parse(file, h);
			
			//XML is valid- turn off validation and parse the file
			factory.setValidating(false);
			parser = factory.newSAXParser();
			parser.parse(file, this);
			
			return datasets;
		} catch (ParserConfigurationException e) {
			System.err.println("FATAL ERROR: Incorrect parser configuration.");
		} catch (SAXException e) {
			System.err.println("FATAL ERROR : Invalid XML.");
		} catch (IOException e) {
			System.err.println("FATAL ERROR: IO error;");
		} catch(Exception e) {
			System.err.println("FATAL ERROR: Invalid XML.");
		}
		return null;
	}

	/* Method to run when parser has reached the start of the element */
	public void startElement(String s, String s1, String elementName, Attributes attributes) throws SAXException {
		if(elementName.equalsIgnoreCase("dataset")) { //Parser reached a dataset element
			numDatasets++;
			String testOpt = ""; //store processed test options
			
			//Split test string on whitespace
			String[] tempTest = attributes.getValue("test").split("\\s");
			
			//Check if test is correct, and format string for consistency
			if(tempTest.length > 2) { //test set option
				if (tempTest[0].equals("testset")) {
					//Check for the file's existence, error if not found
					boolean exists = new File(attributes.getValue("test").substring(7).trim()).isFile();
					if(!exists) { 
						System.err.println("ERROR: Test file " + attributes.getValue("test").substring(7).trim() + " could not be found.");
						System.exit(1);
					}
					else { //set processed test option
						testOpt = "testset " + attributes.getValue("test").substring(7).trim();
					}
				}
				else { //test option is invalid
					System.err.println("ERROR: Test option " + attributes.getValue("test") + " is invalid.");
					System.exit(1);
				}
			}
			else {
				if (tempTest.length == 1) { //leave one out cross validation, or training options.
					//remove extra whitespace
					tempTest[0] = tempTest[0].trim();
					
					//check for validity
					if((!tempTest[0].equals("leavexval")) && (!tempTest[0].equals("training"))) {
						System.err.println("ERROR: Test option " + attributes.getValue("test") + " is invalid.");
						System.exit(1);
					}
					testOpt = tempTest[0]; //set processed test option
				}
				else { //length == 2, xval or percent option
					//remove extra whitespace
					tempTest[0] = tempTest[0].trim();
					tempTest[1] = tempTest[1].trim();
					
					//cross validation
					if(tempTest[0].equals("xval")) {
						//check the number
						if(tempTest[1].matches("\\d+")) {
							//set processed test option
							testOpt = tempTest[0] + " " + tempTest[1];
						}
						else { //errored
							System.err.println("ERROR: Test option " + attributes.getValue("test") + " is invalid.");
							System.exit(1);
						}
					}
					//percentage split option
					else if (tempTest[0].equals("percent")) {
						//check the number
						if(tempTest[1].matches("0+.\\d+")) {
							//set processed test option
							testOpt = tempTest[0] + " " + tempTest[1];
						}
						else { //errored
							System.err.println("ERROR: Percentage in test option " + attributes.getValue("test") + " is invalid.");
							System.exit(1);
						}
					}
					else { //errored
						System.err.println("ERROR: Test option " + attributes.getValue("test") + " is invalid.");
						System.exit(1);
					}
				}
			}
			
			//check if results string is correct, and format for consistency
			//split on commas
			String[] resultSplit = attributes.getValue("results").split(",");
			
			//string to store processed results string
			String resultFin = "";
			
			//for each result option
			for (int i = 0; i < resultSplit.length; i++) {
				//remove whitespace, ensure lowercase
				resultSplit[i] = resultSplit[i].trim().toLowerCase();
				//check word is valid
				if (resultSplit[i].equals("stats") || resultSplit[i].equals("model") || resultSplit[i].equals("entropy") || resultSplit[i].equals("matrix") || resultSplit[i].equals("predictions")) {
					if (i != resultSplit.length - 1) { //if not the last one
						resultFin += resultSplit[i] + ","; //add comma
					}
					else { //last one
						resultFin += resultSplit[i]; //dont add comma 
					}
				}
				else { //errored
					System.err.println("ERROR: Result option " + resultSplit[i] + " is invalid.");
					System.exit(1);
				}
			}
			
			//results and test options now formatted, create temp dataset
			tempDataset = new Dataset(attributes.getValue("name"), testOpt, resultFin);
		}
		else if(elementName.equalsIgnoreCase("classifier")) { //parser reached a classifier element
			//create new classifier
			tempClassifier = new Classifier(attributes.getValue("name")); 
		}
		else if(elementName.equalsIgnoreCase("parameter")) { //parser reached a parameter element
			//create new parameter
			tempClassifier.getParamList().add(new Parameter(attributes.getValue("name"), attributes.getValue("value")));
		}
	}
	
	/* Method to run when parser has reached the end of an element */
	public void endElement(String s, String s1, String element) throws SAXException {
		if(element.equalsIgnoreCase("dataset")) { //end of dataset element
			//add dataset to list, reset tempDataset
			datasets.add(tempDataset);
			tempDataset = null;
		}
		else if(element.equalsIgnoreCase("classifier")) { //end of classifier element
			//add classifier to current dataset, reset tempClassifier
			tempDataset.getClassList().add(tempClassifier);
			tempClassifier = null;
		}
	}
	
	/* Class for handling the XML error checking
	 *   
	 * Author: Madison Flannery, madison.flannery@uon.edu.au
	 */
	private static class ErrorHandler extends DefaultHandler {
		/* method for an XML warning */
		public void warning(SAXParseException e) throws SAXException {
			System.out.println("XML Warning: "); 
			printErrorWarningInfo(e);
		}
		
		/* Method for an XML error */
		public void error(SAXParseException e) throws SAXException {
			System.out.print("XML Error: "); 
			printErrorWarningInfo(e);
			System.out.println("Please fix XML error(s) and try again");
			System.exit(0);
		}
		
		/*Method for printing information */
		private void printErrorWarningInfo(SAXParseException e) {
			System.out.println(e.getMessage() + " (Line: " + e.getLineNumber() + " Col: " + e.getColumnNumber() + ")");	         
		}
	}
}

