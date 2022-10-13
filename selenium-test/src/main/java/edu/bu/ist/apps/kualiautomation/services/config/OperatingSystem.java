package edu.bu.ist.apps.kualiautomation.services.config;

public class OperatingSystem {

	public static enum OSType {
		WINDOWS("Windows operating system"),
		MAC("Mac operating system"),
		UNIX("Unix type operating system (linux, ");
		
		private String description;

		private OSType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}
	
	private static String os = System.getProperty("os.name").toLowerCase();
	
	public boolean isWindows() {
		return os.indexOf( "win" ) >= 0;
	}

	public boolean isMac() {
		return os.indexOf( "mac" ) >= 0;
	}

	public boolean isUnix() {
		return os.indexOf( "nix") >= 0 || os.indexOf( "nux") >= 0;
	}
	
	public OSType getType() {
		if(isWindows())
			return OSType.WINDOWS;
		if(isMac())
			return OSType.MAC;
		if(isUnix())
			return OSType.UNIX;
		return null;
	}
	
}
