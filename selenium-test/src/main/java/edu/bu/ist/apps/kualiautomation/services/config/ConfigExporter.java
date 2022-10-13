package edu.bu.ist.apps.kualiautomation.services.config;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import javax.swing.JFileChooser;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.bu.ist.apps.kualiautomation.entity.Config;
import edu.bu.ist.apps.kualiautomation.util.Utils;
import edu.bu.ist.apps.kualiautomation.model.Directory;

/**
 * @author wrh
 *
 */
public class ConfigExporter {

	public static final String CFG_NAME = "kualiautomation.cfg";
	
	/**
	 * The content of the configuration file is all json which can be reverse mapped back to a Config instance.
	 * @return The reverse mapped instance
	 * @throws Exception
	 */
	public Config getConfig() throws Exception {
		return getConfigInstance(getConfigFile());
	}

	/**
	 * The configuration file is located in a config directory next to the jar file for this application.
	 * Create it (with default values) if it does not exist and return it.
	 * 
	 * @return
	 * @throws Exception
	 */
	private File getConfigFile() throws Exception {
		File rootdir = Utils.getRootDirectory();
		File cfgfile = null;
		if(rootdir != null && rootdir.isDirectory()) {
			File configdir = new File(rootdir, "config");
			if(!configdir.isDirectory()) {
				configdir.mkdir();
			}
			
			cfgfile = new File(configdir, CFG_NAME);
			if(!cfgfile.isFile()) {
				createDefaultConfigFile(cfgfile);
			}
		}
		return cfgfile;
	}
	
	/**
	 * Read json from a file (entire file content) and convert to a Config instance.
	 * 
	 * @param cfgfile
	 * @return
	 * @throws Exception
	 */
	private Config getConfigInstance(File cfgfile) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Config cfg = mapper.readValue(cfgfile, Config.class);
		return cfg;
	}

	/**
	 * Convert a Config instance to json and save that json to a file.
	 * 
	 * @param cfgfile
	 * @throws Exception
	 */
	private void createDefaultConfigFile(File cfgfile) throws Exception {
		Config cfg = new Config();			
		ConfigDefaults.populate(cfg);
		saveConfigFile(cfg, cfgfile);		
	}
	
	/**
	 * Save a the Config instance as json to the specified file.
	 * 
	 * @param cfg
	 * @param cfgfile
	 * @throws Exception
	 */
	private void saveConfigFile(Config cfg, File cfgfile) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(cfgfile);
			mapper.writerWithDefaultPrettyPrinter().writeValue(out, cfg);
			out.close();
		} 
		finally {
			if(out != null) {
				out.close();
			}
		}		
	}
	
	public void saveConfig(Config cfg, String filepath) throws Exception {
		//cfg.setLastUpdated(new Date(System.currentTimeMillis()).toString());
		saveConfigFile(cfg, getConfigFile());
	}

	/**
	 * Throw up a file chooser to get the user to pick a directory on their file system.
	 * Once selected, the configuration for the app is updated with the path of this directory and returned.
	 * 
	 * @return
	 * @throws Exception
	 */
	public Config setOutputDirectory() throws Exception {
		Directory dir = null;
		final JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new java.io.File("."));
		fc.setDialogTitle("Where should you output files go?");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		int returnVal = fc.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
		    File file = fc.getSelectedFile();
		    dir = new Directory(file);
			Config cfg = getConfig();
			saveConfig(cfg, dir.getFilepath());
			return cfg;
		} 
		else {
		    return null;
		}
	}

	public static void main(String[] args) throws Exception {
		ConfigExporter svc = new ConfigExporter();
		Config cfg = svc.getConfig();
		System.out.println(cfg);
	}
}
