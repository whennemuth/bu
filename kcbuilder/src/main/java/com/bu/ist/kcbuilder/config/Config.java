package com.bu.ist.kcbuilder.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the information necessary to build out the workspace and its
 * projects. It should be populated by the jackson ObjectMapper with a file containing
 * json that maps to this class.
 * 
 * @author hennemuthw
 *
 */
public class Config {

	private String pomUrl;
	private String gitExePath;
	private String mavenBinPath;
	private String workspacePath;
	private boolean runScript;
	private Set<Module> modules = new HashSet<Module>();
	
	private Config() { /* Restrict private constructor */ }
	
	public static Config getInstance(String configFilePath) {
		InputStream input;
		Config cfg = null;
		try {
			input = new FileInputStream(configFilePath);
			try {
				cfg = getInstance(input);
			} 
			catch (Exception e) {
				e.printStackTrace();
			} 
			finally {
				if(input != null) {
					try {
						input.close();
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return cfg;
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Config getInstance(InputStream input) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Config cfg = mapper.readValue(input, Config.class);
		return cfg;
	}
	
	/**
	 * This is the reverse to the normal process. We started with a populated instance
	 * of this class and output the corresponding json into a file.
	 * 
	 * @param filePath
	 * @throws JsonProcessingException
	 */
	public void saveAs(String filePath) throws JsonProcessingException {
		String json = (new ObjectMapper()).writerWithDefaultPrettyPrinter().writeValueAsString(this);
		FileWriter writer = null;
		try {
			File f = new File(filePath);
			if(f.isFile()) {
				f.delete();
			}
			writer = new FileWriter(filePath);
			writer.write(json);
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if(writer != null) {
				try {
					writer.close();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void addModule(Module module) {
		modules.add(module);
	}
	public void setModules(Module[] modules) {
		this.modules.clear();
		this.modules.addAll(Arrays.asList(modules));
	}
	public Module[] getModules() {
		// Add the modules to a treeset so they can be sorted by build order
		TreeSet<Module> sorted = new TreeSet<Module>(new Comparator<Module>(){
		@Override public int compare(Module m1, Module m2) {
			return m1.getBuildOrder().compareTo(m2.getBuildOrder());
		}});
		sorted.addAll(modules);
		
		/**
		 * Convert the set of modules to an array, but through a method that involves an iterator 
		 * so that the comparators ordering is observed - using the toArray() method of the Set 
		 * does not seem to produce an array that follow the ordering defined by the comparator.
		 */
		Module[] array = new Module[modules.size()];
		int i = 0;
		for (Iterator<Module> iterator = sorted.iterator(); iterator.hasNext(); i++) {
			array[i] = (Module) iterator.next();
		}
		return array;
	}
	public String getPomUrl() {
		return pomUrl;
	}
	public void setPomUrl(String pomUrl) {
		this.pomUrl = pomUrl;
	}
	public String getGitExePath() {
		return gitExePath;
	}
	public void setGitExePath(String gitExePath) {
		this.gitExePath = gitExePath;
	}
	public String getMavenBinPath() {
		return mavenBinPath;
	}
	public void setMavenBinPath(String mavenBinPath) {
		this.mavenBinPath = mavenBinPath;
	}
	public String getWorkspacePath() {
		return workspacePath;
	}
	public void setWorkspacePath(String workspacePath) {
		this.workspacePath = workspacePath;
	}	
	public boolean isRunScript() {
		return runScript;
	}
	public void setRunScript(boolean runScript) {
		this.runScript = runScript;
	}

	public static void main(String[] args) throws JsonProcessingException {
		Config cfg = new Config();
		cfg.pomUrl = "https://raw.githubusercontent.com/bu-ist/kuali_research/master/pom.xml";
		cfg.gitExePath = "C:/whennemuth/mytechnicalstuff/downloads/Git/bin";
		cfg.mavenBinPath = "C:/whennemuth/mytechnicalstuff/downloads/maven/apache-maven-3.3.9/bin";
		cfg.workspacePath = "C:/Users/hennemuthw/Desktop/kcbuilder_workspace";
		cfg.setRunScript(false);
		
		Module m = new Module();
		m.buildOrder = 2;
		m.gitUrl = "https://github.com/kuali/kc-rice.git";
		m.tagPrefix = "rice-";
		m.versionElement = "<rice.version>";
		cfg.addModule(m);
		
		m = new Module();
		m.buildOrder = 3;
		m.gitUrl = "https://github.com/kuali/kc-api.git";
		m.tagPrefix = "coeus-api-";
		m.versionElement = "<coeus-api-all.version>";
		cfg.addModule(m);

		m = new Module();
		m.buildOrder = 1;
		m.gitUrl = "https://github.com/kuali/schemaspy.git";
		m.tagPrefix = "schemaspy-";
		m.versionElement = "<schemaspy.version>";
		cfg.addModule(m);
		
		m = new Module();
		m.buildOrder = 4;
		m.gitUrl = "https://github.com/kuali/kc-s2sgen.git";
		m.tagPrefix = "coeus-s2sgen-";
		m.versionElement = "<coeus-s2sgen.version>";
		cfg.addModule(m);
		
		m = new Module();
		m.buildOrder = 5;
		m.gitUrl = "https://github.com/kuali/kc.git";
		m.tagPrefix = "";
		m.versionElement = "<version>";
		cfg.addModule(m);
		
		cfg.saveAs("C:/Users/hennemuthw/Desktop/config.json");
		System.out.println("Finished!");
	}
}
