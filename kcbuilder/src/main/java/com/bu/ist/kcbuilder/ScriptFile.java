package com.bu.ist.kcbuilder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import com.bu.ist.kcbuilder.config.Module;

public class ScriptFile {

	private static final OperatingSystem os = OperatingSystem.getThisOperatingSystem();
	private PomFile pom;
	private File scriptFile;
	
	@SuppressWarnings("unused")
	private ScriptFile() { /* Restrict private constructor */ }
	
	public ScriptFile(PomFile pom) {
		this.pom = pom;
	}

	public void saveAs(String scriptFileDirPath) throws FileNotFoundException {
		// Set os and workspace
		File scriptFileDir = new File(scriptFileDirPath);
		if(!scriptFileDir.isDirectory()) {
			throw new FileNotFoundException("Script file directory: \"" + scriptFileDirPath + "\" does not exist!");
		}
		
		// Clear out any existing script file.
		scriptFile = new File(scriptFileDir, ("build" + os.getFileExtension()));
		if(scriptFile.isFile()) {
			scriptFile.delete();
		}
		
		// Instantiate io objects
		FileOutputStream fs = new FileOutputStream(scriptFile);
		BufferedOutputStream bs = new BufferedOutputStream(fs);
		PrintWriter pw = null;
		
		// Write out the script file
		try {
			pw = new PrintWriter(bs);
			writeContent(pw);
			System.out.println("Created script file: " + scriptFile.getAbsolutePath());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if(pw != null) {
				pw.close();
			}
		}
	}
	
	public void writeContent(PrintWriter pw) {
		// cd into the workspace directory
		pw.println("cd " + pom.getConfig().getWorkspacePath());
		pw.println();
		
		// cd into the submodule directory, checkout from git, run maven build.
		Map<Module, String> versions = pom.getSubModuleVersions();
		boolean firstLoop = true;
		boolean inModuleDir = false;
		for (Iterator<Module> iterator = versions.keySet().iterator(); iterator.hasNext();) {
			Module module = iterator.next();
			
			if(module.isRunInstall()) {
				String subModuleVersion = versions.get(module);
				
				if(hasGitRepo(module.getProjectDirectoryName())) {					
					if(firstLoop) {
						pw.println("cd " + module.getProjectDirectoryName());
					}
					else {
						pw.println("cd ../" + module.getProjectDirectoryName());
					}
					inModuleDir = true;					
					pw.println("git checkout tags/" + subModuleVersion);
				}
				else {
					if(inModuleDir) {
						pw.println("cd ..");
						inModuleDir = false;
					}
					pw.println("git clone " + module.getGitUrl());
					pw.println("cd " + module.getProjectDirectoryName());
					inModuleDir = true;					
					if(iterator.hasNext()) {
						// Not the parent project so git checkout necessary because clone gets HEAD of branch.
						pw.println("git checkout tags/" + subModuleVersion);
					}
				}
				
				pw.println("mvn clean compile source:jar javadoc:jar install -Dgrm.off=true");
				pw.println();
				
				firstLoop = false;
			}
			else {
				pw.println("# " + module.getProjectDirectoryName() + " configured to be skipped.");
				pw.println();
			}
		}
		pw.flush();
	}

	/**
	 * Determine if a git directory is established in the specified project directory of the workspace.
	 * 
	 * @param projDirName
	 * @return
	 */
	private boolean hasGitRepo(String projDirName) {
		File workspaceDir = new File(pom.getConfig().getWorkspacePath());
		if(workspaceDir.isDirectory()) {
			File projDir = new File(workspaceDir, projDirName);
			if(projDir.isDirectory()) {
				File gitDir = new File(projDir, ".git");
				return gitDir.isDirectory();
			}
		}
		return false;
	}

	public OperatingSystem getOperatingSystem() {
		return os;
	}
	public File getFile() {
		return scriptFile;
	}
	public PomFile getPom() {
		return pom;
	}
}
