package edu.bu.ist.ci.test.jenkins;

import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

import org.mockito.Mockito;

import edu.bu.ist.ci.Job;
import edu.bu.ist.ci.JobDef;
import edu.bu.ist.ci.JobParameterDef;
import edu.bu.ist.ci.jenkins.JenkinsJobDef;
import edu.bu.ist.ci.jenkins.JenkinsJobParameterDef;
import edu.bu.ist.ci.jenkins.job.SampleJavaJob;

public class BasicMock {
	
	private JobDef jobdef1;
	private Job job1;
	private JobParameterDef jpdef1;
	private JobParameterDef jpdef2;
	private JobParameterDef jpdef3;
	private Set<JobParameterDef> jpdefs = new LinkedHashSet<JobParameterDef>();
	
	private JenkinsJobDef jobdef2;
	private SampleJavaJob job2;
	private JenkinsJobParameterDef jpdef4;
	private JenkinsJobParameterDef jpdef5;
	private JenkinsJobParameterDef jpdef6;
	private Set<JobParameterDef> jpdefs2 = new LinkedHashSet<JobParameterDef>();

	private BasicMock() { /* Restrict private constructor */ }
	
	private BasicMock initializeBasicMock() {
		
		jobdef1 = Mockito.mock(JobDef.class);
		when(jobdef1.getClassName()).thenReturn("myjobdef1");
		when(jobdef1.getParameterDefs()).thenReturn(jpdefs);
		
		job1 = Mockito.mock(Job.class);
		when(job1.getJobDef()).thenReturn(jobdef1);
		
		jpdef1 = Mockito.mock(JobParameterDef.class);
		when(jpdef1.getName()).thenReturn("parmDef1");
		when(jpdef1.getJobDef()).thenReturn(jobdef1);
		
		jpdef2 = Mockito.mock(JobParameterDef.class);
		when(jpdef2.getName()).thenReturn("parmDef2");
		when(jpdef2.getJobDef()).thenReturn(jobdef1);
		
		jpdef3 = Mockito.mock(JobParameterDef.class);
		when(jpdef3.getName()).thenReturn("parmDef3");
		when(jpdef3.getJobDef()).thenReturn(jobdef1);
		
		jpdefs.add(jpdef1);
		jpdefs.add(jpdef2);
		jpdefs.add(jpdef3);
		
		return this;
	}
	
	private BasicMock initializeJenkinsMock() {
		
		jobdef2 = Mockito.mock(JenkinsJobDef.class);
		when(jobdef2.getClassName()).thenReturn("myjobdef1");
		when(jobdef2.getParameterDefs()).thenReturn(jpdefs2);
		
		job2 = Mockito.mock(SampleJavaJob.class);
		when(job2.getJobDef()).thenReturn(jobdef2);
		
		jpdef4 = Mockito.mock(JenkinsJobParameterDef.class);
		when(jpdef4.getName()).thenReturn("parmDef1");
		when(jpdef4.getJobDef()).thenReturn(jobdef2);
		when(jobdef2.findParameterDef("parmDef1")).thenReturn(jpdef4);
		
		jpdef5 = Mockito.mock(JenkinsJobParameterDef.class);
		when(jpdef5.getName()).thenReturn("parmDef2");
		when(jpdef5.getJobDef()).thenReturn(jobdef2);
		when(jobdef2.findParameterDef("parmDef2")).thenReturn(jpdef5);
		
		jpdef6 = Mockito.mock(JenkinsJobParameterDef.class);
		when(jpdef6.getName()).thenReturn("parmDef3");
		when(jpdef6.getJobDef()).thenReturn(jobdef2);
		when(jobdef2.findParameterDef("parmDef3")).thenReturn(jpdef6);
		
		jpdefs2.add(jpdef4);
		jpdefs2.add(jpdef5);
		jpdefs2.add(jpdef6);
		
		return this;
		
	}
	
	public static Job getJob() {
		return (new BasicMock()).initializeBasicMock().job1;
	}
	
	public static JobDef getJobDef() {
		return (new BasicMock()).initializeBasicMock().jobdef1;
	}
	
	public static SampleJavaJob getJenkinsJob() {
		return (new BasicMock()).initializeJenkinsMock().job2;
	}
	
	public static JenkinsJobDef getJenkinsJobDef() {
		return (new BasicMock()).initializeJenkinsMock().jobdef2;
	}
	
}
