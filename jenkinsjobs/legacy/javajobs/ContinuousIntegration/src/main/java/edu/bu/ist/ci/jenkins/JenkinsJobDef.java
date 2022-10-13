package edu.bu.ist.ci.jenkins;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import edu.bu.ist.ci.JobDef;
import edu.bu.ist.ci.JobParameterDef;
import edu.bu.ist.ci.Utils;

public class JenkinsJobDef implements JobDef {

	private String className;
	private Set<JobParameterDef> parmdefs = new LinkedHashSet<JobParameterDef>();
	
	public JenkinsJobDef() {}
	
	public JenkinsJobDef(String className) throws IllegalArgumentException {
		if(className == null || className.trim().isEmpty()) {
			throw new IllegalArgumentException("JenkinsJobDef constructor: className parameter cannot be null or empty");
		}
		this.className = className;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	@JsonDeserialize(contentAs=JenkinsJobParameterDef.class)
	public Set<JobParameterDef> getParameterDefs() {
		return parmdefs;
	}

	@Override
	public JenkinsJobDef addParameterDef(JobParameterDef jobParameterDef) {
		if(jobParameterDef != null) {
			JobParameterDef existing = findParameterDef(jobParameterDef.getName());
			if(existing != null) {
				parmdefs.remove(existing);
			}
			parmdefs.add(jobParameterDef);
			jobParameterDef.setJobDef(this);
		}
		return this;
	}

	public JobParameterDef findParameterDef(String name) {
		if(name == null)
			return null;
		for(JobParameterDef def : parmdefs) {
			if(def.getName() == null)
				continue;
			if(name.trim().equalsIgnoreCase(def.getName().trim())) {
				return def;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[className=").append(className).append(", parameterDefs=[");
		for (Iterator<JobParameterDef> iterator = parmdefs.iterator(); iterator.hasNext();) {
			JobParameterDef def = (JobParameterDef) iterator.next();
			builder.append(def.getName()).append(":").append(def.isRequired() ? "true" : "false");
			if(iterator.hasNext())
				builder.append(", ");
		}
		builder.append("]]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((parmdefs == null) ? 0 : parmdefs.hashCode());
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
		JenkinsJobDef other = (JenkinsJobDef) obj;
		if (getClassName() == null) {
			if (other.getClassName() != null)
				return false;
		} else if (!getClassName().equals(other.getClassName()))
			return false;
		if (getParameterDefs() == null) {
			if (other.getParameterDefs() != null)
				return false;
		} else if (other.getParameterDefs() == null) {
			return false;
		} else if (getParameterDefs().size() != other.getParameterDefs().size()) {
			return false;
		}
		return true;
	}

	@Override
	public String toJSON() {
		return Utils.getJSON(this);
	}
}
