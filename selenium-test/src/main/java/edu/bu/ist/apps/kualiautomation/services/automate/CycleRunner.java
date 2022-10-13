package edu.bu.ist.apps.kualiautomation.services.automate;

import java.util.List;

import org.openqa.selenium.WebDriver;

import edu.bu.ist.apps.kualiautomation.entity.Config;
import edu.bu.ist.apps.kualiautomation.entity.ConfigEnvironment;
import edu.bu.ist.apps.kualiautomation.entity.Cycle;
import edu.bu.ist.apps.kualiautomation.entity.LabelAndValue;
import edu.bu.ist.apps.kualiautomation.entity.Suite;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementValue;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.LocatorRunner;
import edu.bu.ist.apps.kualiautomation.services.config.ConfigTestingDefaults;
import edu.bu.ist.apps.kualiautomation.util.DateOffset;
import edu.bu.ist.apps.kualiautomation.util.DateOffset.DatePart;

/**
 * This class processes a cycle. Every suite, module, and tab is processed in order involving navigating to the
 * correct location, finding each element and applying a value to each or clicking. A log is returned detailing
 * the actions taken and any error encountered.
 * 
 * @author wrh
 *
 */
public class CycleRunner {

	private Window window;
	private WebDriver driver;
	private Cycle cycle;
	private RunLog runlog;
	
	@SuppressWarnings("unused")
	private CycleRunner() { /* Restrict private constructor */ }
	
	public CycleRunner(Session session, RunLog runlog) {
		this(session.getDriver(), session.getCycle(), runlog);
	}
	
	public CycleRunner(WebDriver driver, Cycle cycle, RunLog runlog) {
		this.window = new Window(driver);
		this.driver = driver;
		this.cycle = cycle;
		this.runlog = runlog;
	}
	
	public CycleRunner(Session session) {
		this(session.getDriver(), session.getCycle(), null);
	}
	
	public CycleRunner(WebDriver driver, Cycle cycle) {
		this(driver, cycle, null);
	}
	
	public RunLog run() {		
		System.out.println("Processing Cycle: " + cycle.getName());
		if(runlog == null) {
			runlog = new RunLog(true);
		}
		
		outerloop:
		for(Suite suite : cycle.getSuites()) {
			System.out.println("Processing Suite: " + suite.getName());
			for(LabelAndValue lv : suite.getLabelAndValues()) {
				LocatorRunner locator = new LocatorRunner(driver, runlog);
				locator.setIgnoreHidden(true);
				runlog.printMessage(lv, "Searching for:");
				List<Element> elements = locator.runGreedy(lv);
				if(elementLocated(lv, elements)) {
					if(!applyElementValue(lv, elements.get(0))) {
						window.focus();
						//driver.switchTo().defaultContent();
						break outerloop;
					}
				}
				else {
					window.focus();
					//driver.switchTo().defaultContent();
					break outerloop;
				}
				window.focus();
				//driver.switchTo().defaultContent();
			}
		}
		
		return runlog;
	}

	/**
	 * Require that one element was found from the location attempt for non-screenscrapes
	 * Require that at least one element was found from the location attempt for screenscrapes.
	 * Make a log entry if not so.
	 * 
	 * @param lv
	 * @param elements
	 */
	private boolean elementLocated(LabelAndValue lv, List<Element> elements) {
		if(elements == null || elements.isEmpty()) {
			runlog.elementNotFound(lv);
			return false;
		}
		else if(elements.size() > 1 && !lv.isScreenScrape()) {
			runlog.multipleElementCandidates(lv, elements);
			return false;
		}

		return true;
	}
	
	/**
	 * Apply the provided value to the element or log any error in the attempt.
	 * 
	 * @param lv
	 * @param elements
	 */
	private boolean applyElementValue(LabelAndValue lv, Element element) {
		String valueString = null;
		if(lv.isScreenScrape()) {
			/**
			 * SCENARIO A:
			 * ScreenScrape instances find and apply their own values to themselves.
			 * However, carry its value (a screen scrape) over to the LabelAndValue
			 * instance on which it was based so it can be relocated and used in SCENARIO B later on.
			 */
			lv.setScreenScrapeValue(element.getValue());
			return true;
		}
		if(lv.getScreenScrapeId() > 0) {
			/**
			 * SCENARIO B:
			 * Get the value from a prior screenscrape LabelAndValue instance by searching for
			 * it throughout the cycle and getting its getScreenScrapeValue() return value. 
			 */
			valueString = getScreenScrapeValue(lv.getScreenScrapeId());
		}
		else if(lv.getScreenScrapeId() == -1) {
			/**
			 * SCENARIO C:
			 * Date offset entry. Compute the date and then apply the resulting value.
			 */
			int dateUnits = Integer.valueOf(lv.getDateUnits());
			DatePart datePart = DatePart.valueOf(lv.getDatePart());
			DateOffset offset = DateOffset.valueOf(lv.getDateFormatChoice());
			if(DateOffset.CUSTOM.equals(offset)) {
				valueString = offset.getOffsetDate(lv.getDateFormat(), datePart, dateUnits);
			}
			else {
				valueString = offset.getOffsetDate(datePart, dateUnits);
			}
		}
		else {
			/**
			 * SCENARIO D: 
			 * Not screenscrape-related. Apply a value directly.
			 */
			valueString = lv.getValue();
		}
		ElementValue val = new ElementValue(driver, valueString);
		runlog.log(element, lv);
		if(!val.applyTo(element, lv.isNavigates())) {
			runlog.valueApplicationError(lv, element);
		}
		
		return true;
	}
	
	/**
	 * Obtain a screenscrape based on the id of a LabelAndValue instance whose ElementType is SCREENSCRAPE.
	 * 
	 * @param screenScrapeId
	 * @return
	 */
	private String getScreenScrapeValue(Integer screenScrapeId) {
		for(Suite suite : cycle.getSuites()) {
			for(LabelAndValue lv : suite.getLabelAndValues()) {
				if(screenScrapeId.equals(lv.getId()) && lv.isScreenScrape()) {
					return lv.getScreenScrapeValue();
				}
			}
		}
		return null;
	}
	
	public static void main(String[] args) {
		
		// Create the configuration
		Config config = new Config();
		ConfigTestingDefaults.populate(config);
		
		// Change the default environment
		ConfigEnvironment testDriveEnv = new ConfigEnvironment();
		testDriveEnv.setId(999);
		testDriveEnv.setName("TEST-DRIVE");
		testDriveEnv.setUrl("https://res-demo2.kuali.co/kc-dev/kr-login/login?viewId=DummyLoginView&returnLocation=%2Fkc-krad%2FlandingPage&formKey=68ba02c6-3587-4b81-a4c9-d8eb465eaa01&cacheKey=7ft9p0xr31nwqntioww2pa");
		config.addConfigEnvironment(testDriveEnv);
		config.setCurrentEnvironment(testDriveEnv);
		
		// Create 2nd login screen username data
		LabelAndValue lv1 = new LabelAndValue();
		lv1.setElementType(ElementType.TEXTBOX.name());
		lv1.setLabel("username / email");
		lv1.setValue("quickstart");
		
		// Create 2nd login screen username data
		LabelAndValue lv2 = new LabelAndValue();
		lv2.setElementType(ElementType.TEXTBOX.name());
		lv2.setLabel("password");
		lv2.setValue("password");
		
		// Create 2nd login screen login button
		LabelAndValue lv3 = new LabelAndValue();
		lv3.setElementType(ElementType.BUTTON.name());
		lv3.setLabel("sign in");
		
		Suite suite = new Suite();
		suite.addLabelAndValue(lv1);
		suite.addLabelAndValue(lv2);
		suite.addLabelAndValue(lv3);
		
		// Create the cycle to run and add the suite to it
		Cycle cycle = new Cycle();
		KerberosLoginParms parms = new KerberosLoginParms();
		parms.setUsername("quickstart");
		parms.setPassword(null);
		cycle.setKerberosLoginParms(parms);
		cycle.addSuite(suite);
		
		// Create and run the session
		Session session = new Session(config, cycle, false);
		Thread thread = new Thread(session);
		thread.start();
		System.out.println("thread started...");

	}
}
