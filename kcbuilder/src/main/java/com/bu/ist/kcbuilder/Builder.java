package com.bu.ist.kcbuilder;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.bu.ist.kcbuilder.config.Config;

/**
 * This is the main class for this application (main function is located here).
 * @author hennemuthw
 *
 */
public class Builder {

	private Config config;
	
	public Builder(Config config) {
		this.config = config;
	}
	
	public boolean build() throws FileNotFoundException {
		PomFile pom = new PomFile(config);
		if(pom.isLoaded()) {
			Workspace workspace = new Workspace(pom);
			if(workspace.create()) {
				ScriptFile sf = new ScriptFile(pom);
				sf.saveAs(config.getWorkspacePath());
				if(config.isRunScript()) {
					ScriptProcess process = new ScriptProcess(sf);
					try {
						if(process.run()) {
							return true;
						}
					} 
					catch (InterruptedException e) {
						e.printStackTrace();
						return false;
					}
				}
				else {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * The configuration file is a .json file and should be indicated by path as the single command line argument
	 * or, if no argument, the json file should exist in the same directory as the jar itself.
	 * 
	 * @param args
	 * @return
	 */
	private static InputStream getConfigInputStream(String[] args) {
		InputStream in = null;
		
		if(args.length == 0) {
			try {
				File jarDir = Util.getJarDirectory();
				File[] jsonFiles = jarDir.listFiles(new FileFilter(){
					@Override
					public boolean accept(File f) {
						return f.isFile() && f.getName().endsWith(".json");
					}});
				System.out.println("No json file argument specified, checking " + jarDir.getAbsolutePath() + "...");
				if(jsonFiles.length > 1) {
					System.out.println("More than one \".json\" file at: " + jarDir.getAbsolutePath());
					System.out.println("Which one is the config file?");
					System.out.println("Cancelling build");
				}
				else if(jsonFiles.length == 0) {
					System.out.println("Expecting a \".json\" file at: " + jarDir.getAbsolutePath());
					System.out.println("None found, cancelling build");
				}
				else {
					in = new FileInputStream(jsonFiles[0]);
				}
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				in = new FileInputStream(args[0]);
			} 
			catch (FileNotFoundException e) {
				System.out.println("No such configuration file: " + args[0]);
			}
		}
		
		return in;
	}
	
	public static void main(String[] args) {

		try {
			InputStream in = getConfigInputStream(args);
			if(in != null) {
				Config cfg = Config.getInstance(in);
				Builder bdr = new Builder(cfg);
				if(!bdr.build()) {
					System.out.println("BUILD FAILED!");
					return;
				}
				System.out.println("BUILD SUCCEEDED!");
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("BUILD FAILED!");
		}
	}
}
