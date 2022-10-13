package edu.bu.ist.ci.test.jenkins;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.ist.ci.BasicEnvironment;
import edu.bu.ist.ci.jenkins.JenkinsJobEnvironment;
import edu.bu.ist.ci.test.BasicEnvironmentTest;

public class JenkinsJobEnvironmentTest {

	private static BasicEnvironment basic;
	private JenkinsJobEnvironment env;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicEnvironmentTest.setUpBeforeClass();
		basic = new BasicEnvironment(BasicEnvironmentTest.ENVS);
	}

	@Before
	public void setUp() throws Exception {
		env = new JenkinsJobEnvironment(basic);
	}

	@Test
	public void testGetVariables() {
		assertEquals(17, env.getVariables().size());
	}

	@Test
	public void testGetVariable() {
		final String expected = "d641b0750afcb206";
		assertEquals(expected, env.getVariable("JENKINS_SERVER_COOKIE"));
		assertEquals(expected, env.getVariable(" JENKINS_SERVER_COOKIE "));
		assertEquals(expected, env.getVariable("JenkinsServerCookie"));
		assertEquals(expected, env.getVariable(" JenkinsServerCookie "));
		assertNull(env.getVariable("bogus"));
	}

	@Test
	public void testGetParameter() {
		assertEquals("testing3", env.getParameter("MY_PARM3"));
		assertEquals("testing2", env.getParameter("MY_PARM2"));
		assertEquals("testing1", env.getParameter("MY_PARM1"));
		assertEquals("master", env.getParameter("NODE_LABELS"));
		assertNull(env.getParameter("bogus"));
		assertNull(env.getParameter(null));
	}

	@Test
	public void testGetJobUrl() {
		assertEquals("http://10.57.236.6:8080/job/test-javajobs1/", env.getJobUrl());
	}

	@Test
	public void testGetBuildId() {
		assertEquals("4", env.getBuildId());
	}

	@Test
	public void testGetJobName() {
		assertEquals("test-javajobs1", env.getJobName());
	}

	@Test
	public void testGetJenkinsServerCookie() {
		assertEquals("d641b0750afcb206", env.getJenkinsServerCookie());
	}

	@Test
	public void testGetLogName() {
		assertEquals("jenkins", env.getLogName());
	}

	@Test
	public void testGetWorkspace() {
		assertEquals("/var/lib/jenkins/workspace/test-javajobs1", env.getWorkspace());
	}

	@Test
	public void testGetPwd() {
		assertEquals("/var/lib/jenkins/workspace/test-javajobs1", env.getPwd());
	}

	@Test
	public void testGetJenkinsUrl() {
		assertEquals("http://10.57.236.6:8080/", env.getJenkinsUrl());
	}

	@Test
	public void testGetRunLevel() {
		assertEquals("3", env.getRunLevel());
	}

	@Test
	public void testGetBuildTag() {
		assertEquals("jenkins-test-javajobs1-4", env.getBuildTag());
	}

	@Test
	public void testGetExecutorNumber() {
		assertEquals("3", env.getExecutorNumber());
	}

	@Test
	public void testGetJobBaseName() {
		assertEquals("test-javajobs1", env.getJobBaseName());
	}

	@Test
	public void testGetUser() {
		assertEquals("jenkins", env.getUser());
	}

	@Test
	public void testGetJenkinsHome() {
		assertEquals("/var/lib/jenkins", env.getJenkinsHome());
	}

	@Test
	public void testGetBuildNumber() {
		assertEquals("4", env.getBuildNumber());
	}

	@Test
	public void testGetBuildDisplayName() {
		assertEquals("#4", env.getBuildDisplayName());
	}

	@Test
	public void testGetBuildUrl() {
		assertEquals("http://10.57.236.6:8080/job/test-javajobs1/4/", env.getBuildUrl());
	}

}
