package edu.bu.ist.apps.kualiautomation.services.automate;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.Response;

public class ReusableWebDriver extends RemoteWebDriver {

	public static Map<Driver, RemoteWebDriver> cache = new HashMap<Driver, RemoteWebDriver>();
	
	public static WebDriver getInstance(Driver driver, boolean enableJavascript, boolean useCached) throws Exception {
		boolean remote = RemoteWebDriver.class.isAssignableFrom(driver.getDriverClass());
		
		// First determine if a prior remote web driver session existed (want to reuse it).
		RemoteWebDriver cachedWebDriver = null;
		WebDriver webdriver = null;		
		if(remote && useCached) {
			Driver d = getCachedInstance(driver);
			if(d != null) {
				if(d.equals(driver)) {
					cachedWebDriver = cache.get(driver);
					if(driver.hasWindow(cachedWebDriver)) {
						/**
						 * This assumes that the browser window that the cached driver was originally created
						 * for is still open. That window will be reused for the next activity the driver is
						 * to be used for.
						 */
						return cachedWebDriver;
					}
					else {
						/**
						 * A new browser window is to be used for the cached driver and no association yet 
						 * exists. The prior browser window was closed by the user. TODO: Have not tested yet if this works.
						 */
//						driver.setLoggedIn(false);
//						return new ReusableWebDriver(
//							driver, 
//							new URL(cachedWebDriver.getCurrentUrl()), 
//							driver.getSessionId());
					}
				}
			}
		}
		
		// Nothing cached, so create a new web driver from scratch.
		DesiredCapabilities desiredCapabilities = driver.getDesiredCapabilities();
		desiredCapabilities.setJavascriptEnabled(enableJavascript);
		
		try {
			if(driver.getDriverClass() != null && driver.getDriverFile() != null) {
				Constructor<?> con = null;
				Class<?> conParmType = null;
				for(Constructor<?> c : driver.getDriverClass().getConstructors()) {
					if(c.getParameterCount() == 1) {
						conParmType = c.getParameterTypes()[0];
						if(conParmType.equals(DesiredCapabilities.class)) {
							con = c;
							break;
						}
						else if(conParmType.equals(Capabilities.class)) {
							con = c;
						}
					}
				}
				if(con == null) {
					// Invoke no-arg constructor
					webdriver = (WebDriver) driver.getDriverClass().newInstance();
				}
				else {
					// DesiredCapabilities.firefox() has "marionette" set to true by default, if that changes:
					// desiredCapabilities.setCapability("marionette", true);
					webdriver = (WebDriver) con.newInstance(desiredCapabilities);
				}
			}
		} 
		catch (InstantiationException e) {
			e.printStackTrace(System.out);
		} 
		catch (IllegalAccessException e) {
			e.printStackTrace(System.out);
		}		
		
		if(remote) {
			driver.setHandle(webdriver.getWindowHandle());
			driver.setSessionId(((RemoteWebDriver) webdriver).getSessionId().toString());
			driver.setLoggedIn(false);
			cache.put(driver, ((RemoteWebDriver) webdriver));
		}
		
		return webdriver;
	}
	
	public static WebDriver getInstance(Driver driver, boolean enableJavascript) throws Exception {
		return getInstance(driver, enableJavascript, true);
	}
	
	public static Driver getCachedInstance(Driver driver) {
		if(RemoteWebDriver.class.isAssignableFrom(driver.getDriverClass())) {	
			for(Driver d : cache.keySet()) {
				if(d.equals(driver)) {
					return d;
				}
			}
		}
		return null;
	}
	
	public static void setLoggedIn(Driver driver) {
		Driver d = getCachedInstance(driver);
		d.setLoggedIn(true);
	}
	
	public static boolean isLoggedIn(Driver driver) {
		Driver d = getCachedInstance(driver);
		return d != null && d.isLoggedIn();
	}
	
	
	/**
	 * The session whose id is being provided was never associated with the open browser window.
	 * Use this function to a get a driver using an existing session and a new browser window.
	 * @param driver
	 * @param url
	 * @param sessionId
	 */
	public ReusableWebDriver(Driver driver, URL url, String sessionId) {
		super();
        setSessionId(sessionId);
        setCommandExecutor(new HttpCommandExecutor(url) {
            @Override
            public Response execute(Command command) throws IOException {
                if (command.getName() != "newSession") {
                    return super.execute(command);
                }
                return super.execute(new Command(getSessionId(), "getCapabilities"));
            }
        });
        // startSession(new DesiredCapabilities());
        startSession(driver.getDesiredCapabilities());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ReusableWebDriver))
			return false;
		ReusableWebDriver other = (ReusableWebDriver) obj;

		if(this.getSessionId() == null)
			return false;
		return other.getSessionId().equals(this.getSessionId());
	}
}
