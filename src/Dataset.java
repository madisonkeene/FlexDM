import java.util.LinkedList;

/* Class for storing all info about a dataset
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

public class Dataset {
	private String name; //Dataset name
	private LinkedList<Classifier> classList; //Classifiers to be run on this dataset
	private String test; //Test method
	private String[] results; //Results requested
	private String dir; //Directory name
	
	//Constructors
	
	public Dataset() {
		name = null;
		classList = new LinkedList<Classifier>();
		setResults(new String[5]);
	}
	
	public Dataset(String name, String test, String results) {
		this.name = name;
		this.setTest(test);
		classList = new LinkedList<Classifier>();
		results.replaceAll("\\s+","");
		this.setResults(results.split(","));
	}
	
	public Dataset(String name, LinkedList<Classifier> classList) {
		this.name = name;
		this.classList = classList;
		setResults(new String[5]);
	}

	//Get and set methods 
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LinkedList<Classifier> getClassList() {
		return classList;
	}

	public void setClassList(LinkedList<Classifier> classList) {
		this.classList = classList;
	}
	
	public Classifier getClassifier(int index) {
		return classList.get(index);
	}
	
	public int getNumClassifiers() {
		return classList.size();
	}

	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		this.test = test;
	}

	public void setDir(String absolutePath) {
		dir = absolutePath;	
	}

	public String getDir() {
		// TODO Auto-generated method stub
		return dir;
	}

	public String[] getResults() {
		return results;
	}

	public void setResults(String[] results) {
		this.results = results;
	}
	
	
}
