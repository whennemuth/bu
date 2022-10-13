package com.bu.ist.kcbuilder;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bu.ist.kcbuilder.config.Module;

public class PomFileTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCreateFromURL() throws Exception {
		
		PomFile pom = Util.getDefaultPomFile();
		if(!pom.isBadUrl()) {
			assertTrue(pom.isLoaded());
		}
	}

	@Test
	public void testCreateFromResource() throws Exception {

		PomFile pom = Util.getDefaultPomFile();
		
		assertTrue(pom.isLoaded());
		Map<Module, String> subModuleVersions = pom.getSubModuleVersions();
		assertEquals(5, subModuleVersions.size());
		
		assertEquals(
				"schemaspy-1507.2", 
				subModuleVersions.get(pom.getConfig().getModules()[0]));
		
		assertEquals(
				"rice-2.5.3.1509.0002-kualico", 
				subModuleVersions.get(pom.getConfig().getModules()[1]));
		
		assertEquals(
				"coeus-api-1509.0003", 
				subModuleVersions.get(pom.getConfig().getModules()[2]));
		
		assertEquals(
				"coeus-s2sgen-1509.0014", 
				subModuleVersions.get(pom.getConfig().getModules()[3]));
		
		assertEquals(
				"1509.58-SNAPSHOT", 
				subModuleVersions.get(pom.getConfig().getModules()[4]));
	}

	@Test
	public void testGetVersionRegex() {
		PomFile pom = new PomFile(null);
		String element = pom.getVersionRegex("<coeus-api-all.version>");
		assertEquals("<coeus\\-api\\-all\\.version>([^<>]+)</coeus\\-api\\-all\\.version>", element);
	}

	@Test
	public void testGetVersion() {
		PomFile pom = new PomFile(null);
		String element = "<coeus-api-all.version>";
		String regex = pom.getVersionRegex(element);
		String version = pom.getVersion(
				"mary had a little lamb "
				+ "<coeus-api-all.version>1601.003</coeus-api-all.version>"
				+ " whose fleece was...",
				regex);
		assertEquals("1601.003", version);
	}
}
