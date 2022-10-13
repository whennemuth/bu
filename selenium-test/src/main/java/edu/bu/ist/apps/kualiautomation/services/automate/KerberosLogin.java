package edu.bu.ist.apps.kualiautomation.services.automate;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import edu.bu.ist.apps.kualiautomation.entity.Config;
import edu.bu.ist.apps.kualiautomation.entity.ConfigEnvironment;
import edu.bu.ist.apps.kualiautomation.entity.Cycle;
import edu.bu.ist.apps.kualiautomation.entity.LabelAndValue;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.BasicElementLocator;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.LocatorRunner;
import edu.bu.ist.apps.kualiautomation.services.config.ConfigService;
import edu.bu.ist.apps.kualiautomation.services.config.ConfigTestingDefaults;

public class KerberosLogin {
	
	private KerberosLoginParms loginParms;
	private String loginUrl;
	private WebDriver driver;
	private long submittedAt;
	
	private final LocatorRunner locatorRunner;
	private WebDriverWait wait;
	private int timeout;
	private Element username;
	private String failureMessage;
	private RunLog runlog;
	
	public KerberosLogin(Session session, int timeout, RunLog runlog) {
		this.loginParms = session.getCycle().getKerberosLoginParms();
		if(loginParms.getConfigEnvironmentId() == null || loginParms.getConfigEnvironmentId() < 1) {
			this.loginUrl = session.getConfig().getCurrentEnvironment().getUrl();
		}
		else {
			this.loginUrl = (new ConfigService()).getConfigEnvironmentById(
					loginParms.getConfigEnvironmentId()).getUrl();
		}
		loginParms.setKerberosLoginFields(KerberosLoginFields.KUALI);
		this.driver = session.getDriver();
		this.runlog = runlog;
		this.locatorRunner = new LocatorRunner(driver, this.runlog);
		this.timeout = timeout;
		this.wait = new WebDriverWait(driver, timeout);
	}

	public boolean login() {
		try {
			driver.get(loginUrl);
			
			wait.until(loginPageLoaded());
			
			typeAndSubmit();
			
			wait.until(arrivedAtNextPage());
		} 
		catch (TimeoutException e) {
			failureMessage = "Timed out (" + 
					String.valueOf(timeout) + " seconds) waiting for " + 
					(submittedAt == 0 ? "initial login page to load." : "login submission to succeed");
			return false;
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
			return false;
		}

		return true;
	}

	private void typeAndSubmit() throws Exception {
		System.out.println("Going to login page...");
		this.locatorRunner.setIgnoreHidden(true);
		// Also use BasicElementLocator so that the label in LabelAndValue will also be treated as an attribute of the sought element
		this.locatorRunner.addLocator(BasicElementLocator.class);
		
		// 1) Get the password field
		Element password = locatorRunner.run(
			loginParms.getKerberosLoginFields().getPasswordLabelAndValue(), false);
		
		// 2) Get the submit button
		Element submit = locatorRunner.run(
				loginParms.getKerberosLoginFields().getSubmitButtonLabelAndValue(), false);
		
		username.setValue(loginParms.getUsername());
		if(password != null && password.isInteractive()) {
			password.setValue(loginParms.getPassword());
		}
		submit.click();
		
		submittedAt = System.currentTimeMillis();
	}

	private ExpectedCondition<Boolean> loginPageLoaded() {
		ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {			  
			public Boolean apply(WebDriver drv) {
				String label = loginParms.getKerberosLoginFields().getUserLabel();
				String identifier = loginParms.getKerberosLoginFields().getUserIdentifier();
				if(locatorRunner.isBusy()) {
					System.out.println("busy finding " + (label == null ? identifier : label));
					return false;
				}
				
				try {
					System.out.println("Start search for " + (label == null ? identifier : label));
					
					locatorRunner.setIgnoreHidden(true);
					// Also use BasicElementLocator so that the label in LabelAndValue will also be treated as an attribute of the sought element
					locatorRunner.addLocator(BasicElementLocator.class);

					LabelAndValue lv = loginParms.getKerberosLoginFields().getUsernameLabelAndValue();
					
					username = locatorRunner.run(lv, false);
					if(username == null) {
						loginParms.setKerberosLoginFields(KerberosLoginFields.KUALI_TESTDRIVE_1);
						lv = loginParms.getKerberosLoginFields().getUsernameLabelAndValue();
						username = locatorRunner.run(lv);
						if(username == null) {
							loginParms.setKerberosLoginFields(KerberosLoginFields.KUALI_TESTDRIVE_2);
							lv = loginParms.getKerberosLoginFields().getUsernameLabelAndValue();
							username = locatorRunner.run(lv);							
						}
					}
					
					return usernameLocated();
				} 
				catch (Exception e) {
					// The apply method will ignore checked exceptions, so wrap them in RuntimeExceptions
					throw new RuntimeException(e);
				}
			}
		};
		return condition;
	}
	
	private Boolean waitingForNextPage = false;
	private ExpectedCondition<Boolean> arrivedAtNextPage() {		
		ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {			  
			public Boolean apply(WebDriver drv) {
				if(waitingForNextPage) {
					System.out.println("busy finding next page");
					return false;
				}
				try {
					waitingForNextPage = true;
					username.getWebElement().findElements(By.id("does not matter"));
				} 
				catch (StaleElementReferenceException e) {
					if(!driver.findElements(By.tagName("html")).isEmpty()) {
						return true;
					}
				}
				catch(Exception e) {
					// The apply method will ignore checked exceptions, so wrap them in RuntimeExceptions
					throw new RuntimeException(e);
				}
				finally {
					waitingForNextPage = false;
				}
				return false;
			}
		};
		return condition;
	}
	
	private boolean usernameLocated() {
		boolean located = username != null;
		if(located) {
			located = username.isInteractive() && username.getElementType().acceptsKeystrokes();
		}
		String label = loginParms.getKerberosLoginFields().getUserLabel();
		String identifier = loginParms.getKerberosLoginFields().getUserIdentifier();
		StringBuilder s = new StringBuilder(label == null ? identifier : label)
				.append(" ");
		if(!located)
			s.append("NOT ");
		s.append("located");
		System.out.println(s.toString());
		return located;
	}
	
	public boolean loginFailed() {
		return failureMessage != null;
	}
	
	public String getFailureMessage() {
		return failureMessage;
	}
	
	public static void main(String[] args) {
		
		// Create the configuration
		Config config = new Config();
		ConfigTestingDefaults.populate(config);
		
		// Change the default environment
		ConfigEnvironment testDriveEnv = new ConfigEnvironment();
		testDriveEnv.setUrl("https://res-demo2.kuali.co/kc-dev/kr-login/login?viewId=DummyLoginView&returnLocation=%2Fkc-krad%2FlandingPage&formKey=68ba02c6-3587-4b81-a4c9-d8eb465eaa01&cacheKey=7ft9p0xr31nwqntioww2pa");
		testDriveEnv.setId(999);
		config.addConfigEnvironment(testDriveEnv);
		config.setCurrentEnvironment(testDriveEnv);
		
		// Create the cycle to run
		Cycle cycle = new Cycle();
		KerberosLoginParms parms = new KerberosLoginParms();
		parms.setUsername("quickstart");
		parms.setPassword(null);
		cycle.setKerberosLoginParms(parms);
		
		// Create and run the session
		Session session = new Session(config, cycle, false);
		Thread thread = new Thread(session);
		thread.start();
		System.out.println("thread started...");
	}
}
