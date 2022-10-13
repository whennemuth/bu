package edu.bu.ist.ci.jenkins;

import edu.bu.ist.ci.Job;
import edu.bu.ist.ci.JobParameter;
import edu.bu.ist.ci.JobParameterDef;

public class JenkinsJobParameter implements JobParameter {
	
	private JobParameterDef jobParameterDef;
	private Object value;
	private boolean valid = true;
	
	public static final String INVALID_NULL_JOB_PARM_DEF = "JenkinsJobParameter: constructor arg jobParameterDef cannot be null";	
	public static final String INVALID_EMPTY_JOB_PARM_DEF = "JenkinsJobParameter: constructor arg jobParameterDef cannot be empty";	
	public static final String INVALID_UNKNOWN_JOB_PARM_DEF = "JenkinsJobParameter: No JobParameterDef exists by the name of ?";	
	
	private JenkinsJobParameter() { /* Restrict default constructor */ }
	
	public static JenkinsJobParameter getInstance(JobParameterDef jobParameterDef, Object value) {
		if(jobParameterDef == null) {
			throw new IllegalArgumentException(INVALID_NULL_JOB_PARM_DEF);
		}
		JenkinsJobParameter jp = new JenkinsJobParameter();
		jp.jobParameterDef = jobParameterDef;
		jp.value = value;
		return jp;
	}
	
	public static JenkinsJobParameter getInstance(JobParameterDef jobParameterDef) {
		return getInstance(jobParameterDef, null);
	}
	
	public static JenkinsJobParameter getInstanceFromString(Job job, String parm) {
		JenkinsJobParameter jp = new JenkinsJobParameter();
		String name = "";
		String value = "";
		if(parm == null) {
			throw new IllegalArgumentException(INVALID_NULL_JOB_PARM_DEF);
		}
		
		if(parm.contains("=")) {
			name = parm.substring(0, parm.indexOf("=")).trim();
			if(name.isEmpty()) {
				throw new IllegalArgumentException(INVALID_EMPTY_JOB_PARM_DEF);
			}
			jp.jobParameterDef = ((JenkinsJobDef) job.getJobDef()).findParameterDef(name);
			if(jp.jobParameterDef == null) {
				throw new IllegalArgumentException(INVALID_UNKNOWN_JOB_PARM_DEF.replaceFirst("\\?", name));
			}
			value = parm.substring(parm.indexOf("=") + 1).trim();
		}
		else {
			name = parm.trim();
			if(name.isEmpty()) {
				throw new IllegalArgumentException(INVALID_EMPTY_JOB_PARM_DEF);
			}			
			jp.jobParameterDef = ((JenkinsJobDef) job.getJobDef()).findParameterDef(name);
			if(jp.jobParameterDef == null) {
				throw new IllegalArgumentException(INVALID_UNKNOWN_JOB_PARM_DEF.replaceFirst("\\?", name));
			}
		}
		
		if(!value.isEmpty()) {
			jp.value = value;
		}
		return jp;
	}
	
	
	@Override
	public JobParameterDef getJobParameterDef() {
		return jobParameterDef;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public boolean isValid() {
		return valid;
	}
	
	@Override
	public boolean isEmpty() {
		return value == null || value.toString().trim().isEmpty();
	}
	
	@Override
	public String toString() {
		return "[jobParameterDef=" + jobParameterDef + ", value=" + value + ", valid=" + valid + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jobParameterDef == null || jobParameterDef.getName() == null) ? 0 : jobParameterDef.getName().hashCode());
		result = prime * result + (valid ? 1231 : 1237);
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		JenkinsJobParameter other = (JenkinsJobParameter) obj;
		if (getJobParameterDef() == null) {
			if (other.getJobParameterDef() != null)
				return false;
		} else if (!getJobParameterDef().equals(other.getJobParameterDef()))
			return false;
		if (isValid() != other.isValid())
			return false;
		if (getValue() == null) {
			if (other.getValue() != null)
				return false;
		} else if (!getValue().equals(other.getValue()))
			return false;
		return true;
	}

}
