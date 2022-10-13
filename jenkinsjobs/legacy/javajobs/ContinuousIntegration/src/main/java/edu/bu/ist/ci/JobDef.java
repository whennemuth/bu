package edu.bu.ist.ci;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface JobDef {

	String getClassName();

	Set<JobParameterDef> getParameterDefs();

	/**
	 * Add a JobParameterDef
	 * @param jobParameterDef
	 * @return The instance of JobDef for method call chaining.
	 */
	@JsonIgnore
	JobDef addParameterDef(JobParameterDef jobParameterDef);

	@JsonIgnore
	String toJSON();
}
