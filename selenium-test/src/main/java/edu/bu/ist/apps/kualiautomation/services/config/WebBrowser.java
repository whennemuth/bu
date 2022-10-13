package edu.bu.ist.apps.kualiautomation.services.config;

public abstract class WebBrowser {

	private static OperatingSystem os = new OperatingSystem();
	
	public abstract void open(String url) throws Exception;
	
	public static void main(String[] args) throws Exception {
		WebBrowser wb = new WebBrowserFactory().getBrowser(os);
		wb.open("http://www.github.com");
	}
}
