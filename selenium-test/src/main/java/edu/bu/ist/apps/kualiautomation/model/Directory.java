package edu.bu.ist.apps.kualiautomation.model;

import java.io.File;

public class Directory {

	private String filepath;
	private String error;
	
	private File f;
	
	public Directory(File f) {
		this.f = f;
	}
	public Directory() { }
	public String getFilepath() {
		if(filepath == null)
			return f == null ? "CANCELLED" : f.getAbsolutePath();
		return filepath;
	}
	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	
}