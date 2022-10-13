package com.bu.ist.kcbuilder;

import static org.junit.Assert.fail;

import java.io.PrintWriter;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestScriptFile {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testWriteContent() {
		PomFile pom = Util.getDefaultPomFile();
		ScriptFile sf = new ScriptFile(pom);
		sf.writeContent(new PrintWriter(System.out));
		// TODO: write tests here.
	}

	//@Test
	public void testGetFile() {
		fail("Not yet implemented");
	}

}
