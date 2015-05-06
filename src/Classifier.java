import java.util.LinkedList;

/* Class for storing all info about a classifier
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

public class Classifier {
	private String name; //Classifier name
	private LinkedList<Parameter> paramList; //List of parameters
	private String dirName; //Directory name
	
	//Constructors
	
	public Classifier() {
		name = null;
		paramList = new LinkedList<Parameter>();
	}
	
	public Classifier(Classifier temp) {
		name = temp.name;
		paramList = new LinkedList<Parameter>();
		for(int i = 0; i < temp.getParamList().size(); i++) {
			paramList.add(new Parameter(temp.getParameter(i)));
		}
	}
	
	public Classifier(String name) {
		this.name = name;
		paramList = new LinkedList<Parameter>();
	}
	
	public Classifier(String name, LinkedList<Parameter> paramList) {
		this.name = name;
		this.paramList = paramList;
	}

	//Get & set methods
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LinkedList<Parameter> getParamList() {
		return paramList;
	}

	public void setParamList(LinkedList<Parameter> paramList) {
		this.paramList = paramList;
	}
	
	public Parameter getParameter(int index) {
		return paramList.get(index);
	}
	
	public int getNumParams() {
		return paramList.size();
	}

	public String getDirName() {
		return dirName;
	}

	public void setDirName(String dirName) {
		this.dirName = dirName;
	}
	
	
}
