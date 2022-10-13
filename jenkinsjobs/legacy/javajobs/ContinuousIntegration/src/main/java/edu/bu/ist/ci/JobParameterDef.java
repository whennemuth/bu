package edu.bu.ist.ci;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface JobParameterDef {

	/**
	 * The Job this JobParameterDef belongs to.
	 * @return
	 */
	JobDef getJobDef();
	
	void setJobDef(JobDef jobDef);
	
	/**.
	 * The name of the JobParameterDef
	 * @return
	 */
	String getName();
	
	/**
	 * Refers to a file (with/wo path information) containing any markup or templating that presents a visual component for data entry of the parameter.
	 * @return
	 */
	String getViewPathName();
	
	/**
	 * Refers to any markup or template that presents a visual component for data entry of the parameter.
	 * @return
	 */
	String getView(String...parms);
	
	/**
	 * An instance of an implementing class, parsed as JSON.
	 * @return
	 */
	@JsonIgnore
	String toJSON();
	
	/**
	 * Required entry. If true, the job should fail if no value is provided for the associated JobParameter.
	 * @return
	 */
	boolean isRequired();
}
