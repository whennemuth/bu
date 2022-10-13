package edu.bu.ist.ci.test.jenkins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import edu.bu.ist.ci.JobParameterDef;
import edu.bu.ist.ci.jenkins.JenkinsJobDef;
import edu.bu.ist.ci.test.ExceptionTester;

@RunWith(MockitoJUnitRunner.class)
public class JenkinsJobDefTest {

	Set<JobParameterDef> jpdefs;
	
	@Before
	public void setUp() throws Exception {
		jpdefs = BasicMock.getJob().getJobDef().getParameterDefs();
	}
	
	@Test
	public void testJobDefImpl() {
		// Each one of the following is an invalid instantiation (null and/or empty parameters)
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() {
				JenkinsJobDef def = new JenkinsJobDef(null);
			}}).Assert(IllegalArgumentException.class, "JenkinsJobDef constructor: name parameter cannot be null or empty");
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() {
				JenkinsJobDef def = new JenkinsJobDef("");
			}}).Assert(IllegalArgumentException.class, "JenkinsJobDef constructor: name parameter cannot be null or empty");
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() {
				JenkinsJobDef def = new JenkinsJobDef(" ");
			}}).Assert(IllegalArgumentException.class, "JenkinsJobDef constructor: name parameter cannot be null or empty");
		
		JenkinsJobDef def = new JenkinsJobDef("myjobdef");
		for(JobParameterDef jpdef : jpdefs) {
			def.addParameterDef(jpdef);
		}
		assertEquals("myjobdef", def.getClassName());
		assertNotNull(def.getParameterDefs());
		assertFalse(def.getParameterDefs().isEmpty());
		assertEquals(3, def.getParameterDefs().size());
	}
	
	@Test
	public void testFindParameterDef() {
		JenkinsJobDef def = new JenkinsJobDef("myjobdef");
		for(JobParameterDef jpdef : jpdefs) {
			def.addParameterDef(jpdef);
		}
		
		JobParameterDef jpd = def.findParameterDef("parmDef1");
		assertNotNull(jpd);
		assertEquals("parmDef1", jpd.getName());
		
		jpd = def.findParameterDef("PARMdef1");
		assertNotNull(jpd);
		assertEquals("parmDef1", jpd.getName());
		
		jpd = def.findParameterDef(" parmDEF1 ");
		assertNotNull(jpd);
		assertEquals("parmDef1", jpd.getName());
		
		jpd = def.findParameterDef("bogus");
		assertNull(jpd);
	}
}
