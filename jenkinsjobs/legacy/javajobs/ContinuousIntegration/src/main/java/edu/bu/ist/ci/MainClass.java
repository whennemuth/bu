package edu.bu.ist.ci;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import edu.bu.ist.ci.jenkins.JenkinsJobDef;

public class MainClass {
	
	/**
	 * This is the entrance point for the application.
	 * To build the application: 
	 * 
	 * 1) Push changes to github.
	 * 2) Navigate to the jenkins website and execute the git fetch job.
	 *    or alternatively...
	 *    Shell into the jenkins server and navigate to ${JENKINS_HOME}/jobs and perform a pull.
	 * 3) Build the jar file:
	 *    a) cd ${JENKINS_HOME}/jobs/javajobs/ContinuousIntegration
	 *    b) mvn clean compile package.
	 * 4) Run the jar file:
	 * 
	 *    java \
	 *       -Djob.get.html="MY_PARM_NAME"
	 *       -Djob.def.json="${JOB_JSON}" \
	 *       -jar \
	 *       "${CIJOBS_JAR_LOCATION}" \
	 *       "jobparameter1=value1" \
	 *       "jobparameter2=value2" \
	 *       "jobparameter3=value3"
	 *       
	 *    or without optional parameters...
	 *    
	 *    java -jar "${CIJOBS_JAR_LOCATION}"
	 *       
	 *    where:
	 *       a) "-Djob.get.html" is a VM arg that sets a system property that indicates the name of a particular Active choices 
	 *          job parameter for which we want to obtain the corresponding html instead of running the main job (exclude this property to run the main job).
	 *          
	 *       b) "-Djob.def.json" is a VM arg that sets a system property that indicates a json string 
	 *          that defines the configuration details for the java-based function call that we delegate the running of the job to.
	 *       
	 *       c) ${CIJOBS_JAR_LOCATION} is the absolute path of this jar file, currently:
	 *          "${JENKINS_HOME}/jobs/javajobs/ContinuousIntegration/target/ContinuousIntegration-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
	 *          
	 *       d) jobparameterX=valueX are the program arguments. Each represents the name of a job parameter and its value.
	 *          If provided, they would override the same job parameters already set by JENKINS as environment variables.
	 *          Therefore, these arguments are optional.
	 *    
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		try {
			String htmlFor = System.getProperty("job.get.html");
			String jobJson = System.getProperty("job.def.json");
			if(Utils.isBase64(jobJson)) {
				jobJson = Utils.base64Decode(jobJson);
			}
			
			if(Utils.isEmpty(jobJson)) {
				System.out.println("ERROR! Missing system property: job.def.json");
			}
			else {
				Job job = getJob(jobJson, args);

				if(Utils.isEmpty(htmlFor)) {
					
					job.run();	
				}
				else {
					
					printView(htmlFor, job);
				}
			}
		} 
		catch (Exception e) {
			// We want the stack trace to go to stdout, not stderr
			e.printStackTrace(System.out);
		}
	}
	
	/**
	 * Print to standard out the view for the specified field.
	 * 
	 * @param parameterName
	 * @param job
	 */
	private static void printView(String parameterName, Job job) {
		String view = job.getView(parameterName);
		if(Utils.isEmpty(view)) {
			System.out.println("<span style='color:red'>ERROR! no view available for " + parameterName + "</span>");
		}
		else {
			System.out.println(view);
		}		
	}
	
	/**
	 * Create an instance of Job based on a specified JobDef instance using reflection against the 
	 * className property of that JobDef instance.
	 *  
	 * @param jobJson The JobDef instance will be the deserialized result of this json.
	 * @param env 
	 * @return A constructed Job instance or null.
	 * 
	 * @throws Exception
	 */
	private static Job getJob(String jobJson, String...args) throws Exception {
		
		JenkinsJobDef jobdef = Utils.getFromJSON(JenkinsJobDef.class, jobJson);
		
		if(jobdef == null || jobdef.getClassName() == null) {
			System.out.println("ERROR! Cannot instantiate job: null Job or JobDef");
			return null;
		}
		
		Class<?> jobClass = Class.forName(jobdef.getClassName());
		if(!Job.class.isAssignableFrom(jobClass)) {
			System.out.println("ERROR! Invalid job class: " + jobdef.getClassName() + " is not a subtype of " + Job.class.getName());
			return null;
		}
		
		Constructor<?> cstr = jobClass.getConstructor(JobDef.class);
		Job job = (Job) cstr.newInstance(jobdef);
		
		if(args.length > 0) {
			Method method = null;
			try {
				method = job.getClass().getMethod("addStringParameter", String.class);
			} 
			catch (NoSuchMethodException e) {
				System.out.println("ERROR! Expecting job to have addStringParameter(String) method, but not found"); 
			}
			for(String parm : args) {
				method.invoke(job, parm);
			}
		}
		
		return job;		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
