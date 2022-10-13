package edu.bu.ist.ci.test.jenkins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import edu.bu.ist.ci.jenkins.JenkinsJobParameter;
import edu.bu.ist.ci.jenkins.JenkinsJobParameterDef;
import edu.bu.ist.ci.test.ExceptionTester;

@RunWith(MockitoJUnitRunner.class)
public class JenkinsjobParameterTest {
	
	private JenkinsJobParameterDef jpdef1;
	private JenkinsJobParameterDef jpdef2;
	private JenkinsJobParameterDef jpdef3;
	
	@Before
	public void setUp() throws Exception {
		Object[] objs = BasicMock.getJenkinsJobDef().getParameterDefs().toArray();
		jpdef1 = (JenkinsJobParameterDef) objs[0];
		jpdef2 = (JenkinsJobParameterDef) objs[1];
		jpdef3 = (JenkinsJobParameterDef) objs[2];		
	}

	@Test
	public void testBadGetInstance() {
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() {
				JenkinsJobParameter def = JenkinsJobParameter.getInstance(null);
			}}).Assert(IllegalArgumentException.class, JenkinsJobParameter.INVALID_NULL_JOB_PARM_DEF);
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() {
				JenkinsJobParameter def = JenkinsJobParameter.getInstance(null, null);
			}}).Assert(IllegalArgumentException.class, JenkinsJobParameter.INVALID_NULL_JOB_PARM_DEF);
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() {
				JenkinsJobParameter def = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), "");
			}}).Assert(IllegalArgumentException.class, JenkinsJobParameter.INVALID_EMPTY_JOB_PARM_DEF);
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() {
				JenkinsJobParameter def = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), " ");
			}}).Assert(IllegalArgumentException.class, JenkinsJobParameter.INVALID_EMPTY_JOB_PARM_DEF);
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() {
				JenkinsJobParameter def = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), "=myvalue");
			}}).Assert(IllegalArgumentException.class, JenkinsJobParameter.INVALID_EMPTY_JOB_PARM_DEF);
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() {
				JenkinsJobParameter def = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), " = myvalue");
			}}).Assert(IllegalArgumentException.class, JenkinsJobParameter.INVALID_EMPTY_JOB_PARM_DEF);
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() {
				JenkinsJobParameter def = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), "bogus");
			}}).Assert(IllegalArgumentException.class, JenkinsJobParameter.INVALID_UNKNOWN_JOB_PARM_DEF);
	}
	
	@Test
	public void testGetInstance() {
		// Test normal use of single arg constructor
		JenkinsJobParameter parm = JenkinsJobParameter.getInstance(jpdef1);
		assertNull(parm.getValue());
		assertNotNull(parm.getJobParameterDef());
		assertEquals("parmDef1", parm.getJobParameterDef().getName());
		
		// Test normal use of 2 arg constructor
		long now = System.currentTimeMillis();
		parm = JenkinsJobParameter.getInstance(jpdef1, new Date(now));
		assertNotNull(parm.getValue());
		assertNotNull(parm.getJobParameterDef());
		assertEquals("parmDef1", parm.getJobParameterDef().getName());
		assertEquals(now, ((Date) parm.getValue()).getTime());
	}
	
	
	@Test
	public void testGetInstanceFromString() {
		
		// 1) Test string argument that carries name data but no value data:
		JenkinsJobParameter parm = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), jpdef1.getName());
		assertNotNull(parm);
		assertNotNull(parm.getJobParameterDef());
		assertEquals(jpdef1.getName(), parm.getJobParameterDef().getName());
		assertNull(parm.getValue());
		
		parm = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), jpdef2.getName());
		assertNotNull(parm);
		assertNotNull(parm.getJobParameterDef());
		assertEquals(jpdef2.getName(), parm.getJobParameterDef().getName());
		assertNull(parm.getValue());
		
		parm = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), jpdef3.getName());
		assertNotNull(parm);
		assertNotNull(parm.getJobParameterDef());
		assertEquals(jpdef3.getName(), parm.getJobParameterDef().getName());
		assertNull(parm.getValue());
		
		parm = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), jpdef1.getName() + "=");
		assertNotNull(parm);
		assertNotNull(parm.getJobParameterDef());
		assertEquals(jpdef1.getName(), parm.getJobParameterDef().getName());
		assertNull(parm.getValue());
		
		parm = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), jpdef1.getName() + " = ");
		assertNotNull(parm);
		assertNotNull(parm.getJobParameterDef());
		assertEquals(jpdef1.getName(), parm.getJobParameterDef().getName());
		assertNull(parm.getValue());
		
		// 2) Test string argument that carries both name and value data
		parm = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), jpdef1.getName() + "=myvalue");
		assertNotNull(parm);
		assertNotNull(parm.getJobParameterDef());
		assertEquals(jpdef1.getName(), parm.getJobParameterDef().getName());
		assertEquals("myvalue", parm.getValue());
		
		parm = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), jpdef1.getName() + " = myvalue");
		assertNotNull(parm);
		assertNotNull(parm.getJobParameterDef());
		assertEquals(jpdef1.getName(), parm.getJobParameterDef().getName());
		assertEquals("myvalue", parm.getValue());		
		
		parm = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), jpdef1.getName() + "=val3=val4");
		assertNotNull(parm);
		assertNotNull(parm.getJobParameterDef());
		assertEquals(jpdef1.getName(), parm.getJobParameterDef().getName());
		assertEquals("val3=val4", parm.getValue());		
		
		parm = JenkinsJobParameter.getInstanceFromString(BasicMock.getJenkinsJob(), jpdef1.getName() + " =  val: 5 + 6 = 11  ");
		assertNotNull(parm);
		assertNotNull(parm.getJobParameterDef());
		assertEquals(jpdef1.getName(), parm.getJobParameterDef().getName());
		assertEquals("val: 5 + 6 = 11", parm.getValue());		
		
		//=val3=val4
	}

}
