package edu.bu.ist.ci.jenkins;

import java.util.HashMap;
import java.util.Map;

import edu.bu.ist.ci.BasicEnvironment;
import edu.bu.ist.ci.Environment;

public class JenkinsJobEnvironment implements Environment {

	BasicEnvironment basic;
	
	static enum JENKINS_VARS {
		JOB_URL,
		BUILD_ID,
		JOB_NAME,
		JENKINS_SERVER_COOKIE,
		LOGNAME,
		WORKSPACE,
		PWD,
		JENKINS_URL,
		RUNLEVEL,
		BUILD_TAG,
		EXECUTOR_NUMBER,
		JOB_BASE_NAME,
		USER,
		JENKINS_HOME,
		BUILD_NUMBER,
		BUILD_DISPLAY_NAME,
		BUILD_URL;
		
		public String getValue(Environment env) {
			return env.getVariable(this.name());
		}
		
		public boolean isAlternateName(String name) {
			if(name.trim().equalsIgnoreCase(this.name()))
				return true;
			if(name.trim().equalsIgnoreCase(this.name().replaceAll("_", "")))
				return true;
			return false;
		}
		
		public static Map<String, String> asMap(Environment env) {
			Map<String, String> map = new HashMap<String, String>();
			for(JENKINS_VARS jenkVar : JENKINS_VARS.values()) {
				map.put(jenkVar.name(), jenkVar.getValue(env));
			}
			return map;
		}
		
		public static String findValue(Environment env, String name) {
			for(JENKINS_VARS jenkVar : JENKINS_VARS.values()) {
				if(jenkVar.isAlternateName(name)) {
					return jenkVar.getValue(env);
				}
			}
			return null;
		}
	};

	public JenkinsJobEnvironment() {	
		this(new BasicEnvironment());
	}

	public JenkinsJobEnvironment(BasicEnvironment basic) {	
		this.basic = basic;
	}

	@Override
	public Map<String, String> getVariables() {
		return JENKINS_VARS.asMap(basic);
	}

	@Override
	public String getVariable(String name) {
		return JENKINS_VARS.findValue(basic, name);
	}

	public String getParameter(String parmName) {		
		return basic.getVariable(parmName);
	}
	
	@Override
	public String toString() {
		return "JenkinsEnvironment [getJobUrl()=" + getJobUrl() + ", getBuildId()=" + getBuildId() + ", getJobName()="
				+ getJobName() + ", getJenkinsServerCookie()=" + getJenkinsServerCookie() + ", getLogName()="
				+ getLogName() + ", getWorkspace()=" + getWorkspace() + ", getPwd()=" + getPwd() + ", getJenkinsUrl()="
				+ getJenkinsUrl() + ", getRunLevel()=" + getRunLevel() + ", getBuildTag()=" + getBuildTag()
				+ ", getExecutorNumber()=" + getExecutorNumber() + ", getJobBaseName()=" + getJobBaseName()
				+ ", getUser()=" + getUser() + ", getJenkinsHome()=" + getJenkinsHome() + ", getBuildNumber()="
				+ getBuildNumber() + ", getBuildDisplayName()=" + getBuildDisplayName() + ", getBuildUrl()="
				+ getBuildUrl() + "]";
	}

	public String getJobUrl() {
		return JENKINS_VARS.JOB_URL.getValue(basic);
	}

	public String getBuildId() {
		return JENKINS_VARS.BUILD_ID.getValue(basic);
	}

	public String getJobName() {
		return JENKINS_VARS.JOB_NAME.getValue(basic);
	}

	public String getJenkinsServerCookie() {
		return JENKINS_VARS.JENKINS_SERVER_COOKIE.getValue(basic);
	}

	public String getLogName() {
		return JENKINS_VARS.LOGNAME.getValue(basic);
	}

	public String getWorkspace() {
		return JENKINS_VARS.WORKSPACE.getValue(basic);
	}

	public String getPwd() {
		return JENKINS_VARS.PWD.getValue(basic);
	}

	public String getJenkinsUrl() {
		return JENKINS_VARS.JENKINS_URL.getValue(basic);
	}

	public String getRunLevel() {
		return JENKINS_VARS.RUNLEVEL.getValue(basic);
	}

	public String getBuildTag() {
		return JENKINS_VARS.BUILD_TAG.getValue(basic);
	}

	public String getExecutorNumber() {
		return JENKINS_VARS.EXECUTOR_NUMBER.getValue(basic);
	}

	public String getJobBaseName() {
		return JENKINS_VARS.JOB_BASE_NAME.getValue(basic);
	}

	public String getUser() {
		return JENKINS_VARS.USER.getValue(basic);
	}

	public String getJenkinsHome() {
		return JENKINS_VARS.JENKINS_HOME.getValue(basic);
	}

	public String getBuildNumber() {
		return JENKINS_VARS.BUILD_NUMBER.getValue(basic);
	}

	public String getBuildDisplayName() {
		return JENKINS_VARS.BUILD_DISPLAY_NAME.getValue(basic);
	}

	public String getBuildUrl() {
		return JENKINS_VARS.BUILD_URL.getValue(basic);
	}

}
