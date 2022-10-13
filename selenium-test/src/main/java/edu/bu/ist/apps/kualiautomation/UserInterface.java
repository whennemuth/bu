package edu.bu.ist.apps.kualiautomation;

import java.io.Console;
import java.util.List;

import edu.bu.ist.apps.kualiautomation.services.automate.Driver;
import edu.bu.ist.apps.kualiautomation.services.config.EmbeddedJettyServer;
import edu.bu.ist.apps.kualiautomation.services.config.OperatingSystem;
import edu.bu.ist.apps.kualiautomation.services.config.WebBrowser;
import edu.bu.ist.apps.kualiautomation.services.config.WebBrowserFactory;
import edu.bu.ist.apps.kualiautomation.util.Utils;

public class UserInterface {

	private static OperatingSystem os = new OperatingSystem();
	
	public void open(final Integer port) throws Exception {
		
		// Start the web server
		EmbeddedJettyServer jetty = new EmbeddedJettyServer(port) {
			@Override public void onStart() throws Exception {
				// Open a web browser to a page from the web server
				WebBrowser wb = new WebBrowserFactory().getBrowser(os);
				wb.open("http://localhost" + (port == null ? "" : (":"+String.valueOf(port))) + "/main.htm");
			}
		};
		jetty.start();		
	}

	/**
	 * Prompt the user for a browser presented to them from a list based on what drivers could be found.
	 * @return  The driver based on the users browser selection.
	 */
	private static Driver getDriver() {
		
		List<Driver> drivers = Driver.getAvailableDrivers();
		
		if(drivers.size() == 1) {
			return drivers.get(0);
		}
		else {
			while(true) {
				Console c = System.console();
		        if (c == null) {
		            System.err.println("No console.");
		            System.exit(1);
		        }
		        
		        StringBuilder prompt = new StringBuilder()
		        	.append("What browser would you like your scripts to run in?\n");
		        
		        for(int i=0; i<drivers.size(); i++) {
		        	prompt.append(String.valueOf(i+1))
		        	.append(") ")
		        	.append(drivers.get(i).name())
		        	.append("\n");
		        }
		        prompt.append("(select by number from the list above)");
		        
		        String userinput = c.readLine(prompt.toString());
		        
		        if(userinput.matches("\\d{1,2}")) {
		        	Integer i = Integer.valueOf(userinput);
		        	if(i > 0 && i <= drivers.size()) {
		        		return drivers.get(i-1);
		        	}
		        }
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		for(Object key : System.getProperties().keySet()) {
			System.out.println(key.toString() + " = " + System.getProperty(key.toString()));
		}
		if(	args.length > 0 && 
			Utils.isEmpty(args[0]) == false && 
			Utils.trimIgnoreCaseEqual(args[0], "desktop")) {
			
//			Driver driver = getDriver();
			Driver driver = Driver.FIREFOX;
			
			System.setProperty(Driver.DRIVER_SYSTEM_PROPERTY, driver.name());
			
			new UserInterface().open(8080);		
		}
		else {
			// Kicked off automatically - no person involved.
		}
	}
}
