package edu.bu.ist.ci.jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import edu.bu.ist.ci.Job;
import edu.bu.ist.ci.JobDef;
import edu.bu.ist.ci.JobParameter;
import edu.bu.ist.ci.JobParameterDef;
import edu.bu.ist.ci.Utils;
import edu.bu.ist.ci.jenkins.job.SampleJavaJob;

public abstract class AbstractJenkinsJob implements Job {

	protected JobDef jobDef;
	protected List<JobParameter> parameters = new ArrayList<JobParameter>();
	protected JenkinsJobEnvironment env;
	
	private String invalidParametersMessage;
	
	protected AbstractJenkinsJob(JobDef jobDef, JenkinsJobEnvironment env) {
		this.jobDef = jobDef;
		this.env = env;
		// The parameters to run the job may be set as environment variables. 
		// Add any found this way to the parameters list.
		for(JobParameterDef jpdef : jobDef.getParameterDefs()) {
			String parmval = env.getParameter(jpdef.getName());
			if(parmval != null && !parmval.trim().isEmpty()) {
				parameters.add(JenkinsJobParameter.getInstance(jpdef, parmval));
			}
		}
	}
	
	
	protected AbstractJenkinsJob(JobDef jobDef) {
		this(jobDef, new JenkinsJobEnvironment());
	}

	/**
	 * Get a custom view that will vary depending on the content of the parameters collection.
	 * Return null or an empty string if you want to instead return the view from the getView()
	 * method of the JobParameterDef instance that corresponds to parameterName.
	 * 
	 * @param parameterName
	 * @param parameters
	 * @return
	 */
	public abstract String getView(String parameterName, List<JobParameter> parameters);
	
	@Override
	public String getView(String parameterName) {
		
		// Get a view that is dynamic, based on the values of the other parameters of the job.
		String view = getView(parameterName, parameters);		
		
		if(Utils.isEmpty(view)) {
			// Get a static view based on the viewPathName property of the matching JobParameterDef.
			for(JobParameterDef jpdef : jobDef.getParameterDefs()) {
				if(jpdef.getName().equalsIgnoreCase(parameterName)) {
					return jpdef.getView();
				}
			}
		}
		return view;
	}

	@SuppressWarnings("null")
	public void run(String[] parameters) {
		List<JobParameter> plist = new ArrayList<JobParameter>();
		if(parameters != null || parameters.length > 0) {
			for(int i=0; i<parameters.length; i++) {
				JenkinsJobParameter parm = JenkinsJobParameter.getInstanceFromString(this, parameters[i]);
				if(parm.isValid()) {
					plist.add(parm);
				}
			}
		}
		
		processParameters(plist);
		
		if(validParameters()) {			
			run(this.parameters);
		}
	}
	
	@Override
	public void run() {
		run(new String[]{});
	}

	@Override
	public List<JobParameter> getJobParameters() {
		return parameters;
	}

	public void addStringParameter(String nameAndValue) {		
		JobParameter parm = JenkinsJobParameter.getInstanceFromString(this, nameAndValue);
		parameters.remove(parm);
		parameters.add(parm);
	}
	
	public void addJobParameter(JobParameter parm) {
		
	}
	
	/**
	 * It is possible that the parameters necessary to run the job have been provided as both environment variables
	 * AND as explicit parameters to the run method. If this is the case, the explicitly provided parameters take
	 * precedence, which requires they replace any existing counterparts in the parameters list.
	 * 
	 * @param parameters
	 */
	private void processParameters(List<JobParameter> parameters) {
		outerloop:
		for(JobParameter runparm : parameters) {
			for (ListIterator<JobParameter> iterator = this.parameters.listIterator(); iterator.hasNext();) {
				JobParameter envparm = (JobParameter) iterator.next();
				if(envparm.getJobParameterDef().equals(runparm.getJobParameterDef())) {
					if(!runparm.isEmpty()) {
						iterator.remove();
						iterator.add(runparm);
					}
					continue outerloop;
				}
			}
			this.parameters.add(runparm);
		}
	}

	/**
	 * If any JobParameterDef in this instances JobDef.getParameterDefs() collection whose required property is true
	 * does not have a corresponding JobParameter (matched by name) in plist, then plist is invalid.
	 * @param plist
	 */
	private boolean validParameters() {
		StringBuilder msg = new StringBuilder();
		
		outerloop:
		for(JobParameterDef jpdef : jobDef.getParameterDefs()) {
			boolean found = false;
			if(jpdef.isRequired()) {
				for(JobParameter parm : parameters) {
					if(jpdef.equals(parm.getJobParameterDef())) {
						if(!parm.getValue().toString().trim().isEmpty()) {
							found = true;
						}
						continue outerloop;
					}
				}
				if(!found) {
					if(msg.length() > 0)
						msg.append(", ");
					msg.append(jpdef.getName());	
				}
			}
		}
		if(msg.length() > 0) {			
			invalidParametersMessage = "Missing job parameters! [" + msg.toString() + "]";
			return false;
		}
		
		return true;
	}

	public String getInvalidParametersMessage() {
		return invalidParametersMessage;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jobDef == null || jobDef.getClassName() == null) ? 0 : jobDef.getClassName().hashCode());
		result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
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
		SampleJavaJob other = (SampleJavaJob) obj;
		if (getJobDef() == null) {
			if (other.getJobDef() != null)
				return false;
		} else if (!getJobDef().equals(other.getJobDef()))
			return false;
		return true;
	}
}
