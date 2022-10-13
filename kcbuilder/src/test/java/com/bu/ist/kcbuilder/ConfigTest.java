package com.bu.ist.kcbuilder;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bu.ist.kcbuilder.config.Config;

public class ConfigTest {

	private static String json;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		json = 
			"{\r\n" + 
			"  \"pomUrl\" : \"https://raw.githubusercontent.com/bu-ist/kuali_research/master/pom.xml\",\r\n" + 
			"  \"gitExePath\" : \"C:/whennemuth/mytechnicalstuff/downloads/Git/bin\",\r\n" + 
			"  \"mavenBinPath\" : \"C:/whennemuth/mytechnicalstuff/downloads/maven/apache-maven-3.3.9/bin\",\r\n" + 
			"  \"workspacePath\" : \"C:/Users/hennemuthw/Desktop/kcbuilder_workspace\",\r\n" + 
			"  \"runScript\" : \"false\",\r\n" + 
			"  \"modules\" : [ {\r\n" + 
			"    \"gitUrl\" : \"https://github.com/kuali/schemaspy.git\",\r\n" + 
			"    \"tagPrefix\" : \"schemaspy-\",\r\n" + 
			"    \"versionElement\" : \"<schemaspy.version>\",\r\n" + 
			"    \"runInstall\" : \"false\",\r\n" + 
			"    \"buildOrder\" : 1\r\n" + 
			"  }, {\r\n" + 
			"    \"gitUrl\" : \"https://github.com/kuali/kc-api.git\",\r\n" + 
			"    \"tagPrefix\" : \"coeus-api-\",\r\n" + 
			"    \"versionElement\" : \"<coeus-api-all.version>\",\r\n" + 
			"    \"runInstall\" : \"false\",\r\n" + 
			"    \"buildOrder\" : 3\r\n" + 
			"  }, {\r\n" + 
			"    \"gitUrl\" : \"https://github.com/kuali/kc-rice.git\",\r\n" + 
			"    \"tagPrefix\" : \"rice-\",\r\n" + 
			"    \"versionElement\" : \"<rice.version>\",\r\n" + 
			"    \"runInstall\" : \"false\",\r\n" + 
			"    \"buildOrder\" : 2\r\n" + 
			"  }, {\r\n" + 
			"    \"gitUrl\" : \"https://github.com/kuali/kc-s2sgen.git\",\r\n" + 
			"    \"tagPrefix\" : \"coeus-s2sgen-\",\r\n" + 
			"    \"versionElement\" : \"<coeus-s2sgen.version>\",\r\n" + 
			"    \"runInstall\" : \"false\",\r\n" + 
			"    \"buildOrder\" : 4\r\n" + 
			"  }, {\r\n" + 
			"    \"gitUrl\" : \"https://github.com/kuali/kc.git\",\r\n" + 
			"    \"tagPrefix\" : \"\",\r\n" + 
			"    \"versionElement\" : \"<version>\",\r\n" + 
			"    \"runInstall\" : \"false\",\r\n" + 
			"    \"buildOrder\" : 5\r\n" + 
			"  } ]\r\n" + 
			"}";
		}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testInputStreamParm() {
		Config cfg = null;
		try {
			cfg = Config.getInstance(new ByteArrayInputStream(json.getBytes()));
			assertEquals("C:/whennemuth/mytechnicalstuff/downloads/Git/bin", cfg.getGitExePath());
			assertEquals("C:/whennemuth/mytechnicalstuff/downloads/maven/apache-maven-3.3.9/bin", cfg.getMavenBinPath());
			assertEquals("https://raw.githubusercontent.com/bu-ist/kuali_research/master/pom.xml", cfg.getPomUrl());
			assertEquals("C:/Users/hennemuthw/Desktop/kcbuilder_workspace", cfg.getWorkspacePath());
			assertEquals(5, cfg.getModules().length);
			assertEquals("kc-rice", cfg.getModules()[1].getProjectDirectoryName());
			assertEquals("kc-api", cfg.getModules()[2].getProjectDirectoryName());
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

}
