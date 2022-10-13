package edu.bu.ist.ci.test.jenkins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import edu.bu.ist.ci.BasicEnvironment;
import edu.bu.ist.ci.JobDef;
import edu.bu.ist.ci.JobParameter;
import edu.bu.ist.ci.jenkins.AbstractJenkinsJob;
import edu.bu.ist.ci.jenkins.JenkinsJobDef;
import edu.bu.ist.ci.jenkins.JenkinsJobEnvironment;
import edu.bu.ist.ci.jenkins.JenkinsJobParameterDef;
import edu.bu.ist.ci.test.BasicEnvironmentTest;

@RunWith(MockitoJUnitRunner.class)
public class AbstractJenkinsjobTest {
	
	private static BasicEnvironment basic;
	private boolean ran;
	@Mock 
	private JenkinsJobEnvironment envMock;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicEnvironmentTest.setUpBeforeClass();
		basic = new BasicEnvironment(BasicEnvironmentTest.ENVS);
	}

	@Before
	public void setUp() throws Exception {
		final JenkinsJobEnvironment env = new JenkinsJobEnvironment(basic);
		
		doAnswer(new Answer<String>(){
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				String name = (String) invocation.getArguments()[0];
				return env.getVariable(name);
			}}).when(envMock).getVariable(Mockito.anyString());
		
		doAnswer(new Answer<String>(){
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				String name = (String) invocation.getArguments()[0];
				return env.getParameter(name);
			}}).when(envMock).getParameter(Mockito.anyString());
		
		doAnswer(new Answer<Map<String, String>>(){
			@Override
			public Map<String, String> answer(InvocationOnMock invocation) throws Throwable {
				return env.getVariables();
			}}).when(envMock).getVariable(Mockito.anyString());
	}
	
	private AbstractJenkinsJob getJob(String jobname, String[] parmdefs) throws Exception {
		JenkinsJobDef jobdef = new JenkinsJobDef(jobname);
		for(String parmdef : parmdefs) {
			@SuppressWarnings("unused")
			JenkinsJobParameterDef jpdef = new JenkinsJobParameterDef(jobdef, parmdef);
		}
		
		return new AbstractJenkinsJob(jobdef, envMock) {
			@Override public void run() { run(new String[]{}); }
			@Override public void run(List<JobParameter> parameters) { ran = true; }
			@Override public JobDef getJobDef() {
				return super.jobDef;
			}
			@Override
			public String getView(String parameterName, List<JobParameter> parameters) {
				return null; // TODO: change this return value and make a test for it.
			}};		
	}
	
	/**
	 * All 3 parameters passed as arguments to the run method should be used for the job, replacing 
	 * all the 3 of the same parameters created as environment variables.
	 * @throws Exception 
	 */
	@Test
	public void testAbstractJenkinsJob1() throws Exception {
		ran = false;
		AbstractJenkinsJob job = getJob("myjob", new String[]{ "MyParm1:required", "MyParm2:required", "MyParm3" });
		job.run(new String[]{ "myparm1=apples", "myparm2=oranges", "myparm3=pears" });		
		assertTrue(ran);
		Mockito.verify(envMock, Mockito.times(3)).getParameter(Mockito.anyString());
		assertEquals(3, job.getJobParameters().size());
		assertEquals("apples", job.getJobParameters().get(0).getValue());
		assertEquals("oranges", job.getJobParameters().get(1).getValue());
		assertEquals("pears", job.getJobParameters().get(2).getValue());
		assertNull(job.getInvalidParametersMessage());
	}
	
	/**
	 * All 3 parameters created as environment variables should be used for the job because no 
	 * corresponding parameters have been provided as arguments to the run method.
	 * @throws Exception 
	 */
	@Test
	public void testAbstractJenkinsJob2() throws Exception {
		ran = false;
		AbstractJenkinsJob job = getJob("myjob", new String[]{ "MyParm1:required", "MyParm2:required", "MyParm3" });
		job.run();
		assertTrue(ran);
		Mockito.verify(envMock, Mockito.times(3)).getParameter(Mockito.anyString());
		assertEquals(3, job.getJobParameters().size());
		assertEquals("testing1", job.getJobParameters().get(0).getValue());
		assertEquals("testing2", job.getJobParameters().get(1).getValue());
		assertEquals("testing3", job.getJobParameters().get(2).getValue());
		assertNull(job.getInvalidParametersMessage());
	}
	
	/**
	 * All 3 parameters passed as arguments to the run method should be used for the job - no corresponding
	 * environment variables could be used for the job anyway. 
	 * @throws Exception 
	 */
	@Test
	public void testAbstractJenkinsJob3() throws Exception {
		ran = false;
		AbstractJenkinsJob job = getJob("myjob", new String[]{ "parm1:required", "parm2:required", "parm3" });
		job.run(new String[]{ "parm1=apples", "parm2=oranges", "parm3=pears" });		
		assertTrue(ran);
		Mockito.verify(envMock, Mockito.times(3)).getParameter(Mockito.anyString());
		assertEquals(3, job.getJobParameters().size());
		assertEquals("apples", job.getJobParameters().get(0).getValue());
		assertEquals("oranges", job.getJobParameters().get(1).getValue());
		assertEquals("pears", job.getJobParameters().get(2).getValue());
		assertNull(job.getInvalidParametersMessage());
	}
	
	/**
	 * Both of the required parameters are missing (not available as environment variables, and 
	 * not provided as run method arguments). The job should therefore not run.
	 * @throws Exception 
	 */
	@Test
	public void testAbstractJenkinsJob4() throws Exception {
		ran = false;
		AbstractJenkinsJob job = getJob("myjob", new String[]{ "parm1:required", "parm2:required", "parm3" });
		job.run();		
		assertFalse(ran);
		Mockito.verify(envMock, Mockito.times(3)).getParameter(Mockito.anyString());
		assertTrue(job.getJobParameters().isEmpty());
		assertEquals("Missing job parameters! [parm1, parm2]", job.getInvalidParametersMessage());
	}
	
	/**
	 * One of the required parameters is missing (not available as environment variables, and 
	 * not provided as run method arguments). The job should therefore not run.
	 * @throws Exception 
	 */
	@Test
	public void testAbstractJenkinsJob5() throws Exception {
		ran = false;
		AbstractJenkinsJob job = getJob("myjob", new String[]{ "parm1:required", "parm2:required", "parm3" });
		job.run(new String[]{ "parm1=apples" });		
		assertFalse(ran);
		Mockito.verify(envMock, Mockito.times(3)).getParameter(Mockito.anyString());
		assertEquals(1, job.getJobParameters().size());
		assertEquals("Missing job parameters! [parm2]", job.getInvalidParametersMessage());
	}
	
	/**
	 * Both of the required parameters are provided. The job should therefore not run.
	 * @throws Exception 
	 */
	@Test
	public void testAbstractJenkinsJob6() throws Exception {
		ran = false;
		AbstractJenkinsJob job = getJob("myjob", new String[]{ "parm1:required", "parm2:required", "parm3" });
		job.run(new String[]{ "parm1=apples", "parm2=oranges" });		
		assertTrue(ran);
		Mockito.verify(envMock, Mockito.times(3)).getParameter(Mockito.anyString());
		assertEquals(2, job.getJobParameters().size());
		assertNull(job.getInvalidParametersMessage());
	}
	
	/**
	 * The job should use all parameters provided as arguments in the run method in favor of
	 * those corresponding parameters created as environment variables, EXCEPT if their values are empty.
	 * @throws Exception 
	 */
	@Test
	public void testAbstractJenkinsJob7() throws Exception {
		ran = false;
		AbstractJenkinsJob job = getJob("myjob", new String[]{ "MyParm1:required", "MyParm2:required", "MyParm3" });
		job.run(new String[]{ "myparm1= ", "myparm2=oranges", "myparm3= " });		
		assertTrue(ran);
		Mockito.verify(envMock, Mockito.times(3)).getParameter(Mockito.anyString());
		assertEquals(3, job.getJobParameters().size());
		assertEquals("testing1", job.getJobParameters().get(0).getValue());
		assertEquals("oranges", job.getJobParameters().get(1).getValue());
		assertEquals("testing3", job.getJobParameters().get(2).getValue());
		assertNull(job.getInvalidParametersMessage());
	}

}
