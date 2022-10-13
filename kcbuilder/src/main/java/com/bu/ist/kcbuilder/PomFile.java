package com.bu.ist.kcbuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bu.ist.kcbuilder.config.Config;
import com.bu.ist.kcbuilder.config.Module;

public class PomFile {

	private String content;
	private Config config;
	private boolean badUrl;	
	private Map<Module, String> subModuleVersions = new TreeMap<Module, String>(new Comparator<Module>(){
		@Override public int compare(Module m1, Module m2) {
			return m1.getBuildOrder().compareTo(m2.getBuildOrder());
		}});
	
	/**
	 * Obtain the content of a pom file located at a URL indicated by the config object.
	 * 
	 * @param config
	 */
	public PomFile(Config config) {
		if(config == null)
			return;
		this.config = config;
		URL url = null;
		InputStream in = null;
		if(this.content == null) {
			try {
				url = new URL(config.getPomUrl());
				in = url.openStream();
			} 
			catch (Exception e) {
				e.printStackTrace();
				badUrl = true;
			}
			
			loadPom(in);
		}
	}
	
	/**
	 * Skipping the aquisition of the pom file from a remote location based on a URL.
	 * Here the inputstream is the pom file content and the URL contained in the config 
	 * will be ignored.
	 * 
	 * @param config
	 * @param in
	 */
	public PomFile(Config config, InputStream in) {
		this.config = config;
		loadPom(in);		
	}

	/**
	 * Load the content of an inputstream to a private string field. This content is the pom file.
	 * @param in
	 */
	private void loadPom(InputStream in) {
		content = Util.getFileContent(in);
	}
	
	public boolean isLoaded() {
		return content != null && content.isEmpty() == false;
	}

	public boolean isBadUrl() {
		return badUrl;
	}
	
	public String getContent() {
		return content;
	}

	public Config getConfig() {
		return config;
	}

	/**
	 * Get a map of all module versions as found in the pom file, keyed by the enclosing element start tag
	 * @return
	 */
	public Map<Module, String> getSubModuleVersions() {
		if(subModuleVersions.isEmpty()) {
			for(int i=0; i<config.getModules().length; i++) {
				Module module = config.getModules()[i];
				if(module.getVersionElement() != null) {
					String regex = getVersionRegex(module.getVersionElement());
					String version = getVersion(content, regex);
					version = module.getTagPrefix() + version;
					subModuleVersions.put(module, version);
				}
			}
		}
		return subModuleVersions;
	}
	
	public String getVersion(String content, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		if(matcher.find()) {
			return matcher.group(1).trim();
		}
		return null;
	}
	
	public String getVersionRegex(String element) {
		String openTagRegex = element.replaceAll("\\.", "\\\\.").replaceAll("\\-",  "\\\\-");
		String innerHtmlRegex = "([^<>]+)";
		String closeTagRegex = openTagRegex.replaceFirst("<", "</");
		return openTagRegex + innerHtmlRegex + closeTagRegex;
	}
}
