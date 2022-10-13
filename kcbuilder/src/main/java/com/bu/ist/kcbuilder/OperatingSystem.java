package com.bu.ist.kcbuilder;

/**
 * This enum is for different operating system types with information specific
 * to each with regards to scripting.
 * 
 * @author hennemuthw
 *
 */
public enum OperatingSystem { 
	
	WINDOWS(".bat", "call"), 
	MAC(".sh", "source"),
	UNIX(".sh", "source"),
	SOLARIS(".unknown", "unknown");
	
	private String fileExtension;
	private String callCommand;
	private static final String OS = System.getProperty("os.name").toLowerCase();
	
	private OperatingSystem(String fileExtension, String callCommand) {
		this.fileExtension = fileExtension;
		this.callCommand = callCommand;
	}
	public String getFileExtension() {
		return fileExtension;
	}
	public String getCallCommand() {
		return callCommand;
	}
	public static OperatingSystem getThisOperatingSystem() {
		if(OS.indexOf("win") >= 0)
			return WINDOWS;
		else if(OS.indexOf("win") >= 0)
			return MAC;
		else if(OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0)
			return UNIX;
		else if(OS.indexOf("sunos") >= 0)
			return SOLARIS;
		return WINDOWS;	// Arbitrary default if non-recognized or missing system propety.
	}
}