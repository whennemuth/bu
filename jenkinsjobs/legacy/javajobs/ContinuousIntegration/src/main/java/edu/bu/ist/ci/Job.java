package edu.bu.ist.ci;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Job {
	
	void run(List<JobParameter> parameters);
	
	void run(String[] parameters);
	
	void run();
	
	@JsonIgnore
	String getView(String parameterName);
	
	@JsonIgnore
	List<JobParameter> getJobParameters();
	
	JobDef getJobDef();
}
