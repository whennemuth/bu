package edu.bu.ist.ci;

/**
 * A job parameter has a definition (name and other defining fields) and a value that is fully open as to type.
 * 
 * @author wrh
 *
 */
public interface JobParameter {

	JobParameterDef getJobParameterDef();

	Object getValue();
	
	boolean isValid();
	
	boolean isEmpty();
}
