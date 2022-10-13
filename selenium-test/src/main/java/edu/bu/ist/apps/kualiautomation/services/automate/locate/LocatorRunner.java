package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.WebDriver;

import edu.bu.ist.apps.kualiautomation.entity.LabelAndValue;
import edu.bu.ist.apps.kualiautomation.services.automate.RunLog;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;
import edu.bu.ist.apps.kualiautomation.services.automate.element.XpathElementCache;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.label.LabelledElementLocator;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape.ScreenScrapeElementLocator;
import edu.bu.ist.apps.kualiautomation.util.Utils;

/**
 * This class is used to locate a single web element on an html page with selenium.
 * Initially given the element type, one or more implementations of Locator are used, one after the other
 * using the BatchElementLocator, until one of them finds the element.
 * 
 * @author wrh
 *
 */
public class LocatorRunner {

	private WebDriver driver;
	private BatchElementLocator locator;
	private boolean ignoreHidden = true;
	private boolean ignoreDisabled = true;
	private boolean busy;
	private Set<Class<?>> additionalLocators = new LinkedHashSet<Class<?>>();
	private RunLog runlog;
	private boolean quitFast;
	
	@SuppressWarnings("unused")
	private LocatorRunner() { /* Restrict the default constructor */ }
		
	public LocatorRunner(WebDriver driver, RunLog runlog) {
		this.driver = driver;
		this.runlog = runlog;
	}
	
	public Element run(LabelAndValue lv) {
		return run(new LabelAndValue[]{ lv });
	}
	
	public Element run(LabelAndValue[] lvs) {
		List<Element> elements = runGreedy(lvs);
		if(elements.isEmpty())
			return null;
		return elements.get(0);
	}

	public Element run(LabelAndValue lv, boolean greedy) {
		
		List<Element> elements = new ArrayList<Element>();
		if(greedy) {
			return run(lv);
		}
		else {
			elements.addAll(runNonGreedy(new LabelAndValue[]{ lv }));
		}
		if(elements.isEmpty())
			return null;
		return elements.get(0);
	}
	
	public List<Element> runNonGreedy(LabelAndValue lv) {
		return runNonGreedy(new LabelAndValue[]{ lv });
	}

	public List<Element> runNonGreedy(LabelAndValue[] lvs) {
		try {
			quitFast = true;
			busy = true;
			List<Element> elements = new ArrayList<Element>();
			for(LabelAndValue lv : lvs) {
				elements.addAll(doRun(lv));
				if(!elements.isEmpty()) {
					break;
				}
			}
			return elements;
		} 
		finally {
			quitFast = false;
			busy = false;
		}
	}

	public List<Element> runGreedy(LabelAndValue lv) {
		return runGreedy(new LabelAndValue[]{ lv });
	}

	public List<Element> runGreedy(LabelAndValue[] lvs) {
		try {
			busy = true;
			List<Element> elements = new ArrayList<Element>();
			for(LabelAndValue lv : lvs) {
				elements.addAll(doRun(lv));
			}
			return elements;
		} 
		finally {
			busy = false;
		}
	}
	
	/**
	 * Find an element on a web page, returning it or multiple elements if the search criteria yielded more than one match.
	 * @return
	 */
	private List<Element> doRun(LabelAndValue lv) {
		
		List<Element> elements = new ArrayList<Element>();
		Set<Class<?>> locators = new LinkedHashSet<Class<?>>();
		boolean labelCanAlsoBeAnAttribute = true;
		
		XpathElementCache.clear();
		
		try {
			switch(lv.getElementTypeEnum()) {
			case BUTTON: case BUTTONIMAGE:
				locators.add(LabelledElementLocator.class);
				locators.add(BasicElementLocator.class); // the label in LabelAndValue will be treated as an attribute of the sought element
				elements.addAll(runBatch(lv, locators, labelCanAlsoBeAnAttribute));
				if(elements.isEmpty() && ElementType.BUTTON.equals(lv.getElementTypeEnum())) {
					// Search for a button did not work - perhaps the user is dealing with a BUTTONIMAGE?
					LabelAndValue imgLv = lv.copy();
					imgLv.setElementType(ElementType.BUTTONIMAGE.name());
					elements.addAll(runBatch(imgLv, locators, labelCanAlsoBeAnAttribute));
				}
				break;
			case HYPERLINK:
				locators.add(HyperlinkElementLocator.class);
				elements.addAll(runBatch(lv, locators, labelCanAlsoBeAnAttribute));
				if(elements.isEmpty()) {
					// No hyperlinks found. Try hotspots.
					locators.clear();
					locators.add(HotspotElementLocator.class);
					elements.addAll(runBatch(lv, locators, labelCanAlsoBeAnAttribute));
				}
				break;
			case TEXTBOX: case PASSWORD: case TEXTAREA: case SELECT: case RADIO: case CHECKBOX:
				elements.addAll(locateElements(lv, new LabelledElementLocator(driver, null)));
				labelCanAlsoBeAnAttribute = false;
				break;
			case HOTSPOT:
				elements.addAll(locateElements(lv, new HotspotElementLocator(driver, null)));
				break;
			case SHORTCUT:
				elements.addAll(locateElements(lv, new ShortcutElementLocator(driver, lv.getConfigShortcut(), null)));
				break;
			case SCREENSCRAPE:
				elements.addAll(locateElements(lv, new ScreenScrapeElementLocator(driver, null)));
				break;
			case OTHER:
				locators.add(LabelledElementLocator.class);
				locators.add(HotspotElementLocator.class);
				if(lv.getConfigShortcut() != null) {
					locators.add(ShortcutElementLocator.class);
				}
				elements.addAll(runBatch(lv, locators, labelCanAlsoBeAnAttribute));
				break;
			default:
				break;
			}
			
			elements.addAll(runBatch(lv, additionalLocators, labelCanAlsoBeAnAttribute));
			
			return elements;
		} 
		finally {
			XpathElementCache.clear();
		}
	}
	
	private List<Element> locateElements(LabelAndValue lv, Locator locator) {
		String[] parms = null;
		if(lv.isScreenScrape()) {
			parms = new String[]{ lv.getLabel(), lv.getScreenScrapeType() };
		}
		else {
			if(Utils.isEmpty(lv.getIdentifier())) {
				parms = new String[]{ lv.getLabel() };
			}
			else if(Utils.isEmpty(lv.getLabel())) {
				parms = new String[]{ lv.getIdentifier() };
			}
			else {
				parms = new String[]{ lv.getLabel(), lv.getIdentifier() };
			}
		}
		
		if(locator instanceof AbstractElementLocator) {
			((AbstractElementLocator) locator).setIgnoreHidden(ignoreHidden);
			((AbstractElementLocator) locator).setIgnoreDisabled(ignoreDisabled);
		}
		
		List<Element> elements = locator.locateAll(lv.getElementTypeEnum(), Arrays.asList(parms));
		if(!Utils.isEmpty(locator.getMessage())) {
			runlog.printMessage(lv, locator.getMessage());
		}
		
		return elements;
	}
	
	/**
	 * Locate an element using the BatchElementLocator.
	 * Construct strings, one for each Locator implementation to invoke, constructing a string for each that  
	 * identifies the Class Locator to use, and a delimited list of parameters (label and attribute).
	 * 
	 * @param classes
	 * @return
	 */
	private List<Element> runBatch(LabelAndValue lv, Set<Class<?>> classes, boolean labelAsAttribute) {
		
		if(classes.isEmpty())
			return new ArrayList<Element>();
		
		locator = new BatchElementLocator(driver);
		locator.setIgnoreDisabled(ignoreDisabled);
		locator.setIgnoreHidden(ignoreHidden);
		
		String[] parameters = new String[classes.size()];
		
		int i = 0;
		for (Iterator<Class<?>> iterator = classes.iterator(); iterator.hasNext();) {
			Class<?> clazz = iterator.next();
			StringBuilder s = new StringBuilder(clazz.getName());
			if(!BasicElementLocator.class.equals(clazz) || labelAsAttribute) {
				s.append(BatchElementLocator.PARAMETER_DELIMITER).append(lv.getLabel());
			}
			if(!Utils.isEmpty(lv.getIdentifier())) {
				s.append(BatchElementLocator.PARAMETER_DELIMITER).append(lv.getIdentifier());
			}
			parameters[i] = s.toString();
			i++;
		}
		
		List<Element> elements = new ArrayList<Element>();
		if(quitFast) {
			elements.add(locator.locateFirst(lv.getElementTypeEnum(), Arrays.asList(parameters)));
		}
		else {
			elements.addAll(locator.locateAll(lv.getElementTypeEnum(), Arrays.asList(parameters)));
		}
		
		return elements;
	}

	public boolean ignoreHidden() {
		return ignoreHidden;
	}

	public boolean ignoreDisabled() {
		return ignoreDisabled;
	}
	
	public void setIgnoreHidden(boolean ignoreHidden) {
		this.ignoreHidden = ignoreHidden;
	}
	
	public void setIgnoreDisabled(boolean ignoreDisabled) {
		this.ignoreDisabled = ignoreDisabled;
	}

	public boolean isBusy() {
		return busy;
	}

	/**
	 * Add a supplimentary locator by class name. This locator will run after the default
	 * locators associated with the ElementType have run.
	 * 
	 * @param locator
	 */
	public void addLocator(Class<?> locator) {
		additionalLocators.add(locator);
	}
	
	public void removeLocator(Class<?> locator) {
		additionalLocators.remove(locator);
	}

	public WebDriver getWebDriver() {
		return driver;
	}
	
}
