package edu.bu.ist.ci.jenkins.job;

import java.util.List;

import edu.bu.ist.ci.JobDef;
import edu.bu.ist.ci.JobParameter;
import edu.bu.ist.ci.jenkins.AbstractJenkinsJob;
import edu.bu.ist.ci.jenkins.JenkinsJobEnvironment;

public class SampleJavaJob extends AbstractJenkinsJob {

	public SampleJavaJob(JobDef jobDef) {
		super(jobDef, new JenkinsJobEnvironment());
	}
	
	public SampleJavaJob(JobDef jobDef, JenkinsJobEnvironment env) {
		super(jobDef, env);
	}

	@Override
	public JobDef getJobDef() {
		return super.jobDef;
	}
	
	@Override
	public void run(List<JobParameter> parameters) {
		
		System.out.println(this.getJobDef().toJSON());
		
		for(JobParameter parm : parameters) {
			System.out.println();
			System.out.println(parm);
		}
	}

	@Override
	public String getView(String parameterName, List<JobParameter> parameters) {
		
		StringBuilder html = new StringBuilder();
		String parm1 = null;
		String parm2 = null;
		String parm3 = null;
		String typeOf = null;
		
		for(JobParameter parm : parameters) {			
			String name = parm.getJobParameterDef().getName().toLowerCase();
			switch(name) {
			case "myparmdef1":
				parm1 = String.valueOf(parm.getValue()); break;
			case "myparmdef2":
				parm2 = String.valueOf(parm.getValue()); break;
			case "myparmdef3":
				parm3 = String.valueOf(parm.getValue()); break;
			}				
		}
			
		String[] options = null;
		
		switch(parameterName.toLowerCase()) {
		case "myparmdef1": options = new String[]{"fruit", "dairy", "vegetable"}; typeOf = "food"; break;
		case "myparmdef2":
			switch(parm1) {
			case "fruit": options = new String[]{"apple", "orange", "melon"}; break;
			case "vegetable": options = new String[]{"lettuce", "tomato", "onion"}; break;
			case "dairy": options = new String[]{"cheese", "yogurt", "milk"}; break;
			};
			typeOf = parm1;
			break;
		case "myparmdef3":
			switch(parm2) {
			case "apple": options = new String[]{"macintosh", "granny smith", "red delicious"}; break;
			case "orange": options = new String[]{"navel", "tangerine", "valencia"}; break;
			case "melon": options = new String[]{"honeydew", "watermelon", "cantaloupe"}; break;
			case "lettuce": options = new String[]{"iceberg", "romaine", "red leaf"}; break;
			case "tomato": options = new String[]{"cherry", "roma", "campari"}; break;
			case "onion": options = new String[]{"vidalia", "red", "yellow"}; break;
			case "cheese": options = new String[]{"cheddar", "mozzarella", "swiss"}; break;
			case "yogurt": options = new String[]{"greek", "traditional", "frozen"}; break;
			case "milk": options = new String[]{"whole", "skim", "soy"}; break;
			};
			typeOf = parm2;
			break;
		}				

		
		html.append("<table cellpadding=5 border=0 style='padding-left:20px; padding-top:20px;'>")
		 .append("<tr><td align=right style='width:180px;'>Select a type of ")
		 .append(typeOf)
		 .append("</td><td><select name='value' id='")
		 .append(parameterName)
		 .append("' style='width:200px;'>");
		
		for(String option : options) {
			html.append("<option value='").append(option).append("'>").append(option).append("</option>");
		}
			
		html.append("</select></td></tr></table>");
		
		return html.toString();
	}
}
