package edu.bu.ist.ci.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.ist.ci.BasicEnvironment;
import edu.bu.ist.ci.Utils;

public class BasicEnvironmentTest {

	private static String ENV_RESOURCE = "edu/bu/ist/ci/test/env-vars.txt";
	public static Map<String, String> ENVS = new HashMap<String, String>();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		String vars = Utils.getClassPathResourceContent(ENV_RESOURCE);
		String[] lines = vars.split("[\\r\\n]+");
		for(String line : lines) {
			int instr = line.indexOf("=");
			if(instr > 0) {
				String key = line.substring(0, instr).trim();
				String value = line.substring(instr+1);
				ENVS.put(key, value);
			}
		}
	}
	
	@Test
	public void testGetVariables() {
		BasicEnvironment env = new BasicEnvironment(ENVS);
		assertEquals(42, env.getVariables().size());
	}
	
	@Test
	public void testGetVariable() {
		BasicEnvironment env = new BasicEnvironment(ENVS);
		assertEquals("linux", env.getVariable("TERM"));
		assertEquals("/var/lib/jenkins", env.getVariable("JENKINS_HOME"));
		assertEquals("/bin/bash", env.getVariable("SHELL"));		
	}

	@Test
	public void testPrintVariables() throws IOException {
		
		ByteArrayOutputStream bs = null;
		BasicEnvironment env = null;
		String result = null;
		
		// 1) Test the method from an instance based on the no arg method constructor. This will print out all system environment variables.
		bs = new ByteArrayOutputStream();
		env = new BasicEnvironment();
		BasicEnvironment.printVariables(env.getVariables(), bs, true);
		result = bs.toString();		
		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertTrue(result.contains("PATH = "));
		
		// 2) Test the method from an instance based on the single arg method constructor. This will print out all variables provided. 
		bs = new ByteArrayOutputStream();
		env = new BasicEnvironment(ENVS);
		BasicEnvironment.printVariables(env.getVariables(), bs, true);
		result = bs.toString().trim();		
		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertTrue(result.startsWith("_ = "));
		assertTrue(result.endsWith("XFILESEARCHPATH =  /usr/dt/app-defaults/%L/Dt"));
	}

}
