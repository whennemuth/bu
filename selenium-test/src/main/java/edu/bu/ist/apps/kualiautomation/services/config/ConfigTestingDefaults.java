package edu.bu.ist.apps.kualiautomation.services.config;

import edu.bu.ist.apps.kualiautomation.entity.Config;

public enum ConfigTestingDefaults {
	CONFIG_FILE_NAME("kualiautomation.cfg"), 
	DEFAULT_ENVIRONMENT("test-drive"),
	ENVIRONMENTS(
		String.join("&&",
				"TEST",
				"https://kuali-test.bu.edu/kc/portal.do",
				"STAGING",
				"https://kuali-stg.bu.edu/kc/portal.do",
				"DEV",
				"http://ist-kuali-sb1:8080/kc-dev",
				"LOGIN",
				"file:///C:/Users/wrh/Desktop/welcome/login.htm",
				"TEST-DRIVE",
				"https://res-demo2.kuali.co/kc-dev/kr-login/login?viewId=DummyLoginView&returnLocation=%2Fkc-krad%2FlandingPage&formKey=68ba02c6-3587-4b81-a4c9-d8eb465eaa01&cacheKey=7ft9p0xr31nwqntioww2pa",
				"WELCOME",
				"file:///C:/Users/wrh/Desktop/welcome/welcome.htm")
	);

	private String value;
	private ConfigTestingDefaults(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
	public String[] getValues() {
		return value.split("&&");
	}
	/**
	 * Populate an empty Config instance with the enums values.
	 * @param cfg
	 */
	public static void populate(Config cfg) {
		ConfigDefaults.populate(cfg, ENVIRONMENTS.getValues(), DEFAULT_ENVIRONMENT.getValue());
	}
}