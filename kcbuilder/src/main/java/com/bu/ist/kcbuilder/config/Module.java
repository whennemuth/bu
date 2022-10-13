package com.bu.ist.kcbuilder.config;

/**
 * This class provides the module information necessary to build out the projects of
 * the workspace that correspond to the maven submodules that will be present in the main
 * pom file. It is part a collection in the Config class and is populated with the 
 * jackson ObjectMapper when the Config class is populated.
 * 
 * @author hennemuthw
 *
 */
public class Module {
	String gitUrl;
	String tagPrefix;
	String versionElement;
	Integer buildOrder;
	boolean runInstall;
	
	public String getGitUrl() {
		return gitUrl;
	}
	public void setGitUrl(String gitUrl) {
		this.gitUrl = gitUrl;
	}
	public String getTagPrefix() {
		return tagPrefix;
	}
	public void setTagPrefix(String tagPrefix) {
		this.tagPrefix = tagPrefix;
	}
	public String getVersionElement() {
		return versionElement;
	}
	public void setVersionElement(String versionElement) {
		this.versionElement = versionElement;
	}
	public String getProjectDirectoryName() {
		if(gitUrl == null)
			return null;
		String[] parts = gitUrl.split("[\\\\/]");
		String gitDBName = parts[parts.length - 1];
		gitDBName = gitDBName.replaceAll("\\.git", "");
		return gitDBName;
	}
	public Integer getBuildOrder() {
		return buildOrder;
	}
	public void setBuildOrder(Integer buildOrder) {
		this.buildOrder = buildOrder;
	}
	public boolean isRunInstall() {
		return runInstall;
	}
	public void setRunInstall(boolean runInstall) {
		this.runInstall = runInstall;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((buildOrder == null) ? 0 : buildOrder.hashCode());
		result = prime * result + ((tagPrefix == null) ? 0 : tagPrefix.hashCode());
		result = prime * result + ((versionElement == null) ? 0 : versionElement.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Module other = (Module) obj;
		if (buildOrder == null) {
			if (other.buildOrder != null)
				return false;
		} else if (!buildOrder.equals(other.buildOrder))
			return false;
		if (tagPrefix == null) {
			if (other.tagPrefix != null)
				return false;
		} else if (!tagPrefix.equals(other.tagPrefix))
			return false;
		if (versionElement == null) {
			if (other.versionElement != null)
				return false;
		} else if (!versionElement.equals(other.versionElement))
			return false;
		return true;
	}
}