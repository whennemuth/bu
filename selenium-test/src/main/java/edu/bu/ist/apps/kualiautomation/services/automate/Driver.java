package edu.bu.ist.apps.kualiautomation.services.automate;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;

import edu.bu.ist.apps.kualiautomation.util.Utils;

/**
 * The purpose of this class is to enumerate the main browsers and provide functionality to access their drivers
 * from root directory the application is running from.
 * 
 * @author wrh
 *
 */
public enum Driver {

	// Firefox gecko driver can be obtained from https://github.com/mozilla/geckodriver/releases
	FIREFOX(
			"webdriver.gecko.driver", 
			org.openqa.selenium.firefox.FirefoxDriver.class, 
			DesiredCapabilities.firefox(),
			new String[]{ "firefox", "gecko", "marrionette"}),
	FIREFOX_OLD(
			null, 
			org.openqa.selenium.firefox.FirefoxDriver.class,
			DesiredCapabilities.firefox(),
			"firefox"), 
	CHROME(
			"webdriver.chrome.driver", 
			org.openqa.selenium.chrome.ChromeDriver.class,
			DesiredCapabilities.chrome(),
			"chrome"), 
	IE(
			"", 
			null, 
			DesiredCapabilities.internetExplorer(),
			new String[] { "ie", "explorer"}), 
	EDGE(
			"",
			null,
			DesiredCapabilities.edge(),
			"edge"),
	SAFARI(
			"", 
			null, 
			DesiredCapabilities.safari(),
			"safari"), 
	OPERA(
			"",
			null,
			DesiredCapabilities.operaBlink(),
			"opera"),
	HEADLESS(
			"", 
			CustomHtmlUnitDriver.class, 
			DesiredCapabilities.htmlUnit(), 
			"headless");

	public static final String DRIVER_SYSTEM_PROPERTY = "automation.app.browser.driver";
	public static final Driver DEFAULT_DRIVER = FIREFOX;
	
	private String systemProperty;
	private Class<? extends WebDriver> driverClass;	
	private File root = null;
	private File driverFile = null;
	private DesiredCapabilities desiredCapabilities;
	private String[] identifiers;
	private String handle;
	private String sessionId;
	private boolean loggedIn;
		
	private Driver(String systemProperty, Class<? extends WebDriver> driverClass, DesiredCapabilities desiredCapabilities, String...identifiers ) {
		this.systemProperty = systemProperty;
		this.driverClass = driverClass;
		this.desiredCapabilities = desiredCapabilities;
		this.identifiers = identifiers;
		
		if(findDriver()) {			
			setDriverFile();
		}
	}
	
	/**
	 * @return An instance of the selected web driver as an implementation of WebDriver.
	 * @throws Exception 
	 */
	public WebDriver getDriver(boolean enableJavascript) throws Exception {		
		return ReusableWebDriver.getInstance(this, enableJavascript, true);
	}
	
	public WebDriver getDriver() throws Exception {
		return getDriver(true);
	}
	
	public File getDriverFile() {
		if(driverFile == null)
			setDriverFile();
		return driverFile;
	}
	
	
	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}
	
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

	/**
	 * Use the handle that was cached when the webdriver was first created to find if the associated 
	 * window is still open. There is no method on the RemoteWebDriver class to determine this, so we
	 * must catch the runtime error instead to make this determination.
	 * 
	 * @param webdriver
	 * @param handle
	 * @return
	 */
	public boolean hasWindow(RemoteWebDriver webdriver) {
		if(webdriver == null)
			return false;
		
		try {
			WebDriver lookup = webdriver.switchTo().window(handle);
			return lookup != null;
		}
		catch(WebDriverException e) {
			return false;
		}
	}

	/**
	 * @return The driver exe file if it exists.
	 */
	public void setDriverFile() {
		
		try {
			root = Utils.getRootDirectory();
			File[] drivers = root.listFiles(new FilenameFilter() {
				@Override public boolean accept(File dir, String name) {
					if(name.endsWith(".exe")) {
						for(String id : identifiers) {
							if(name.toLowerCase().contains(id.toLowerCase())) {
								return true;
							}
						}
					}
					return false;
				}
			});
			if(drivers.length == 0) {
				System.out.println("Cannot find any web drivers for " + name() + " at: " + root.getAbsolutePath());
			}
			else {
				if(drivers.length > 1) {
					System.out.println("Found more than one web driver for " + name() + ":\n");
					for(int i=0; i<drivers.length; i++) {
						System.out.println(drivers[i].getAbsolutePath() + "\n");
					}
				}
				System.out.println("Using web driver: " + drivers[0].getAbsolutePath());
				driverFile = drivers[0];
				
				System.setProperty(systemProperty, driverFile.getAbsolutePath());
			}
		} 
		catch (Exception e) {
			if(root == null)
				System.out.println("Problem finding web driver directory");
			else if(driverFile == null)
				System.out.println("Problem finding web driver at: " + root.getAbsolutePath());
			else
				System.out.println("Problem loading web driver: " + driverFile.getAbsolutePath());
			
			e.printStackTrace(System.out);
		}
	}
	
	/**
	 * @return Is there enough information to start a search for the driver?
	 */
	private boolean findDriver() {
		return !Utils.isEmpty(systemProperty) && driverClass != null;
	}

	public static Driver getInstance(String drivername) {
		if(drivername == null)
			return null;
		Driver driver = Driver.valueOf(drivername);
		Driver cachedDriver = ReusableWebDriver.getCachedInstance(driver);
		if(cachedDriver == null)
			return driver;
		return cachedDriver;
	}
	
	public static List<Driver> getAvailableDrivers() {
		List<Driver> drivers = new ArrayList<Driver>();
		for(Driver driver : Driver.values()) {
			switch(driver) {
			case FIREFOX_OLD:
				drivers.add(driver);
				break;
			case HEADLESS:
				break;
			default:
				if(driver.getDriverFile() != null) {
					drivers.add(driver);
				}
				break;			
			}			
		}
		return drivers;
	}
	
	public Class<? extends WebDriver> getDriverClass() {
		return driverClass;
	}

	public DesiredCapabilities getDesiredCapabilities() {
		return desiredCapabilities;
	}

	/**
	 * Allow for enabled javascript, but do not throw exceptions.
	 * @author wrh
	 *
	 */
	public static class CustomHtmlUnitDriver extends HtmlUnitDriver {
		public CustomHtmlUnitDriver(boolean javascriptEnabled) {
			super(javascriptEnabled);
		}
		public CustomHtmlUnitDriver(DesiredCapabilities capabilities) {
			super(capabilities);
		}
		public CustomHtmlUnitDriver(BrowserVersion firefox38, boolean javascriptEnabled) {
			super(firefox38, javascriptEnabled);
		}
		@Override protected WebClient modifyWebClient(WebClient client) {
			WebClient modifiedClient = super.modifyWebClient(client);
			modifiedClient.getOptions().setThrowExceptionOnScriptError(false);
			return modifiedClient;
		}
	}
}
