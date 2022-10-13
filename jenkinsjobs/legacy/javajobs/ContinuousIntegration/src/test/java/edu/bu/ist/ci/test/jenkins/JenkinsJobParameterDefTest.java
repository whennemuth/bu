package edu.bu.ist.ci.test.jenkins;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.bu.ist.ci.Job;
import edu.bu.ist.ci.JobDef;
import edu.bu.ist.ci.JobParameterDef;
import edu.bu.ist.ci.jenkins.JenkinsJobDef;
import edu.bu.ist.ci.jenkins.JenkinsJobParameterDef;
import edu.bu.ist.ci.test.ExceptionTester;

public class JenkinsJobParameterDefTest {
	
	JobDef jobdef1;
	Job job1;
	
	@Before
	public void setUp() throws Exception {
		
		jobdef1 = Mockito.mock(JobDef.class);
		when(jobdef1.getClassName()).thenReturn("myjobdef1");
		job1 = Mockito.mock(Job.class);
		when(job1.getJobDef()).thenReturn(jobdef1);
	}
	
	/**
	 * Test invalid usage of JenkinsJobParameterDef
	 */
	@Test
	public void testBadJobParameterDef() {
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() throws Exception {
				JenkinsJobParameterDef def = new JenkinsJobParameterDef(null, null, null, false);
			}}).Assert(IllegalArgumentException.class, "IllegalArgumentException expected but not thrown");
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() throws Exception {
				JenkinsJobParameterDef def = new JenkinsJobParameterDef(jobdef1, null, null, false);
			}}).Assert(IllegalArgumentException.class, "IllegalArgumentException expected but not thrown");
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() throws Exception {
				JenkinsJobParameterDef def = new JenkinsJobParameterDef(jobdef1, "", null, false);
			}}).Assert(IllegalArgumentException.class, "IllegalArgumentException expected but not thrown");
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() throws Exception {
				JenkinsJobParameterDef def = new JenkinsJobParameterDef(jobdef1, " ", null, false);
			}}).Assert(IllegalArgumentException.class, "IllegalArgumentException expected but not thrown");
	}
	
	/**
	 * Test basic usage of the full arg constructor
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJobParameterDef1() throws Exception {
		JenkinsJobParameterDef def = getBasicInstance("myJobParmDef", "", false);
		assertEquals("myJobParmDef", def.getName());
		assertEquals("myjobdef1", def.getJobDef().getClassName());
	}
	
	/**
	 * Test usage of the minimum arg constructor
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJobParameterDef2() throws Exception {
		JenkinsJobParameterDef def = new JenkinsJobParameterDef(jobdef1, "myJobParmDef:required");
		Set<JobParameterDef> defs= new HashSet<JobParameterDef>();
		defs.add(def);		
		when(jobdef1.getParameterDefs()).thenReturn(defs);
		assertEquals("myJobParmDef", def.getName());
		assertEquals("myjobdef1", def.getJobDef().getClassName());		
		assertTrue(def.isRequired());
		
		def = new JenkinsJobParameterDef(jobdef1, "myJobParmDef:TRUE");
		when(jobdef1.getParameterDefs()).thenReturn(defs);
		when(jobdef1.getParameterDefs()).thenReturn(defs);
		assertEquals("myJobParmDef", def.getName());
		assertEquals("myjobdef1", def.getJobDef().getClassName());
		assertTrue(def.isRequired());
		
		def = new JenkinsJobParameterDef(jobdef1, "  myJobParmDef  : YES  ");
		when(jobdef1.getParameterDefs()).thenReturn(defs);
		assertEquals("myJobParmDef", def.getName());
		assertEquals("myjobdef1", def.getJobDef().getClassName());
		assertTrue(def.isRequired());

		def = new JenkinsJobParameterDef(jobdef1, "myJobParmDef:no");
		when(jobdef1.getParameterDefs()).thenReturn(defs);
		assertEquals("myJobParmDef", def.getName());
		assertEquals("myjobdef1", def.getJobDef().getClassName());
		assertFalse(def.isRequired());		

		def = new JenkinsJobParameterDef(jobdef1, "  myJobParmDef  ");
		when(jobdef1.getParameterDefs()).thenReturn(defs);
		assertEquals("myJobParmDef", def.getName());
		assertEquals("myjobdef1", def.getJobDef().getClassName());
		assertFalse(def.isRequired());
		
		(new ExceptionTester() {
			@SuppressWarnings("unused") @Override public void runCode() throws Exception {
				JenkinsJobParameterDef def = new JenkinsJobParameterDef(jobdef1, " :required");
			}}).Assert(IllegalArgumentException.class, "Expected IllegalArgument Exception: " + JenkinsJobParameterDef.INVALID_EMPTY_NAME_PARM);			
	}

	@Test
	public void testGetView() throws Exception {
		
		createAndAssertHasSampleUIView("edu/bu/ist/ci/test/SampleUI.htm", null);
		
		createAndAssertHasSampleUIView("edu/bu/ist/ci/test/sampleui", null);
		
		createAndAssertHasSampleUIView("edu/bu/ist/ci/test/sampleui.txt", null);
		
		createAndAssertHasSampleUIView("edu/bu/ist/ci/test/sampleui.html", null);
		
		createAndAssertHasSampleUIView("edu/bu/ist/ci/test/SAMPLEUI.cfg", null);
		
		createAndAssertHasSampleUIView("SAMPLEUI", "edu/bu/ist/ci/test");
	}

	@Test
	public void testCannotGetView() throws Exception {
		
		createAndAssertViewless("edu/bu/ist/ci/test/bogus.htm", null);
		
		createAndAssertViewless("edu/bu/ist/ci/test/SampleUI.xls", null);
		
		createAndAssertViewless("bogus.htm", "edu/bu/ist/ci/test");
		
		createAndAssertViewless("SampleUI.xls", "edu/bu/ist/ci/test");
	}
	
	@Test
	public void testGetJSON() throws Exception {
		
		// Create a JenkinsJobParameterDef and assert that its toJSON() method returns data.
		JobDef jobdef = new JenkinsJobDef("myjobdef1");
		JenkinsJobParameterDef def = new JenkinsJobParameterDef(jobdef, "myJobParmDef1:required", "edu/bu/ist/ci/test/SampleUI.htm");
		String json = def.toJSON();
		assertNotNull(json);
		
		// Evaluate the JSON itself by deserializing it back into an instance of JenkinsJobParameterDef and asserting each field value.
		ObjectMapper mapper = new ObjectMapper();
		JenkinsJobParameterDef clone = mapper.readValue(json, JenkinsJobParameterDef.class);
		assertEquals("myJobParmDef1", clone.getName());
		assertEquals("edu/bu/ist/ci/test/SampleUI.htm", clone.getViewPathName());
		assertEquals(def.getView(), clone.getView());
		assertTrue(clone.isRequired());
		
		// remove the parameterDefs collection - should still be able to deserialize the json.
		json = json.replaceFirst(",\\s*\"parameterDefs\"\\s*:\\s*\\[[^\\]]*\\]", "");
		clone = mapper.readValue(json, JenkinsJobParameterDef.class);
		assertEquals("myJobParmDef1", clone.getName());
		assertEquals("edu/bu/ist/ci/test/SampleUI.htm", clone.getViewPathName());
		assertEquals(def.getView(), clone.getView());
		assertTrue(clone.isRequired());
		
		// remove the entire jobDef property - should still be able to deserialize the json.
		json = json.replaceFirst("\"jobDef\"\\s*:\\s*\\{[^\\}]*\\}\\s*,", "");
		clone = mapper.readValue(json, JenkinsJobParameterDef.class);
		assertEquals("myJobParmDef1", clone.getName());
		assertEquals("edu/bu/ist/ci/test/SampleUI.htm", clone.getViewPathName());
		assertEquals(def.getView(), clone.getView());
		assertTrue(clone.isRequired());
	}
	
	/**
	 * Get a basic instance of JenkinsJobParameterDef with a mocked JobDef
	 * 
	 * @param name
	 * @param viewPathName
	 * @param required
	 * @return
	 * @throws Exception
	 */
	private JenkinsJobParameterDef getBasicInstance(String name, String viewPathName, boolean required) throws Exception {
		JenkinsJobParameterDef def = new JenkinsJobParameterDef(jobdef1, name, viewPathName, required);
		Set<JobParameterDef> defs= new HashSet<JobParameterDef>();
		defs.add(def);		
		when(jobdef1.getParameterDefs()).thenReturn(defs);
		return def;
	}
	/**
	 * Get a basic instance of JenkinsJobParameterDef with a mocked JobDef, set the viewPathName property and
	 * assert that the file it represents was found and the contents returned by getView().
	 * 
	 * @param viewPathName
	 * @param defaultRcsDir
	 * @throws Exception
	 */
	private void createAndAssertHasSampleUIView(String viewPathName, String defaultRcsDir) throws Exception {
		JenkinsJobParameterDef def = getBasicInstance("myJobParmDef", viewPathName, true);
		if(defaultRcsDir != null) {
			def.setDefaultClasspathResourceDirectory(defaultRcsDir);
		}
		assertEquals(viewPathName, def.getViewPathName());
		assertNotNull(def.getView());
		assertTrue(def.getView().startsWith("<table>"));
		assertTrue(def.getView().endsWith("</table>"));
		assertTrue(def.toString().contains("<table><tr><td>fi..."));		
	}
	/**
	 * Get a basic instance of JenkinsJobParameterDef with a mocked JobDef, set the viewPathName property 
	 * to an arbitrary value and assert that no corresponding file was found and null is returned by getView().
	 * 
	 * @param viewPathName
	 * @param defaultRcsDir
	 * @throws Exception
	 */
	private void createAndAssertViewless(String viewPathName, String defaultRcsDir) throws Exception {
		JenkinsJobParameterDef def = getBasicInstance("myJobParmDef", viewPathName, true);
		if(defaultRcsDir != null) {
			def.setDefaultClasspathResourceDirectory(defaultRcsDir);
		}
		assertEquals(viewPathName, def.getViewPathName());
		assertNull(def.getView());
	}
}
