package com.bu.ist.kcbuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ScriptProcess {
	
	private ScriptFile scriptFile;
	
	
	public ScriptProcess(ScriptFile scriptFile) {
		this.scriptFile = scriptFile;
	}

	public boolean run() throws InterruptedException {
		Process p = null;
		try {
			String scriptFilePath = scriptFile.getFile().getAbsolutePath();
			String workspacePath = scriptFile.getPom().getConfig().getWorkspacePath();
			p = Runtime.getRuntime().exec(scriptFilePath, null, new File(workspacePath));
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			
            String line = "";			
			while ((line = reader.readLine())!= null) {
				System.out.println(line);
				
				while ((line = errReader.readLine())!= null) {
					System.out.println(line);
				}
			}
			
			System.out.println("Waiting for process to exit...");
			p.waitFor();
			System.out.println("Exit value for process = " + String.valueOf(p.exitValue()));
			reader.close();
			p.destroy();
		} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		finally {
			if(p != null && p.isAlive()) {
				p.destroy();
			}
		}
		
		return true;
	}

	public static void main(String[] args) throws IOException {

		StringBuffer output = new StringBuffer();

		Process p = null;
		String mavenDirPath = "C:/whennemuth/mytechnicalstuff/downloads/maven/apache-maven-3.3.9/bin";
		File desktop = new File("C:/Users/hennemuthw/Desktop");
		File workdir = new File("C:/Users/hennemuthw/Desktop/mytest");
		String[] command = {mavenDirPath + "/mvn.cmd", "-h"};

		try {
			p = Runtime.getRuntime().exec("C:/Users/hennemuthw/Desktop/test.bat", null, workdir);
		
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";			
			while ((line = reader.readLine())!= null) {
				System.out.println(line);
				output.append(line + "\n");
			}
			reader.close();
			p.destroy();
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if(p != null && p.isAlive()) {
				p.destroy();
			}
		}
	}
}
