package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

/**
 * This locator finds web elements by delegating to one or more other locators and returning their output. 
 * This is done by invoking other Locator implementations and returning the aggregate results (with duplicates removed).
 * The other implementations are indicated by class name and invoked through reflection.
 * 
 * @author wrh
 *
 */
public class BatchElementLocator implements Locator {
	
	public static final String PARAMETER_DELIMITER = "||";
	
	private WebDriver driver;
	private SearchContext searchContext;
	private boolean defaultRan;
	private boolean busy;
	private boolean ignoreHidden;
	private boolean ignoreDisabled;
	private List<String> messages;
	
	public BatchElementLocator(SearchContext searchContext) {
		if(searchContext instanceof WebDriver) {
			this.driver = (WebDriver) searchContext;
			this.searchContext = searchContext;
		}
		else {
			throw new IllegalStateException("Parameter 'searchContext' not an instance of WebDriver!");
		}		
	}
	
	public BatchElementLocator(WebDriver driver, SearchContext searchContext) {
		this.driver = driver;
		this.searchContext = searchContext;
	}

	public Element locateFirst(ElementType elementType, Map<Class<?>, List<String>> parms) {		
		try {
			busy = true;
			List<Element> results = locateAll(elementType, parms, false);
			if(results.isEmpty())
				return null;
			
			return results.get(0);
		} 
		finally {
			busy = false;
		}
	}
	
	@Override
	public Element locateFirst(ElementType elementType, List<String> parms) {
		return locateFirst(elementType, parseParms(parms));
	}

	@Override
	public List<Element> locateAll(ElementType elementType, List<String> parms) {
		return locateAll(elementType, parseParms(parms), true);
	}
	
	private List<Element> locateAll(ElementType elementType, Map<Class<?>, List<String>> parms, boolean greedy) {
		
		defaultRan = false;
		List<Element> results = new ArrayList<Element>();
		
		try {
			busy = true;
			for(Class<?> clazz : parms.keySet()) {
				try {
					Constructor<?> ctr = clazz.getConstructor(WebDriver.class, SearchContext.class, Locator.class);
					List<String> attributes = parms.get(clazz);
					Locator locator = (Locator) ctr.newInstance(driver, searchContext, null);
					
					Method setter = clazz.getMethod("setIgnoreHidden", boolean.class);
					if(setter != null) {
						setter.invoke(locator, this.ignoreHidden);
					}
					
					setter = clazz.getMethod("setIgnoreDisabled", boolean.class);
					if(setter != null) {
						setter.invoke(locator, this.ignoreDisabled);
					}
					
					List<Element> moreResults = runLocator(locator, elementType, attributes);
					
					boolean foundResult = false;
					for(Element result : moreResults) {
						if(addUniqueResult(results, result) && !greedy) {
							foundResult = true;
						}
					}
					if(foundResult)
						break;
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			return results;
		} 
		finally {
			busy = false;
		}		
	}
	
	/**
	 * Add an Element to a list of found elements only if it is not found to be equal (by toString() value) to an element already in the list.
	 * 
	 * @param results
	 * @param result
	 * @return
	 */
	private boolean addUniqueResult(List<Element> results, Element result) {
		if(result == null)
			return false;
		for(Element e : results) {
			if(e.toString().equals(result.toString())) {
				return false;
			}
		}
		results.add(result);
		return true;
	}
	
	/**
	 * Run the locator. If a locator is of type AbstractElementLocator, it will always run the default locate method for the elementType
	 * unless flagged not to do so, therefore set the defaultRan boolean field to true after the first locator has run
	 * so that subsequent locators in the batch do not also run this default location method for the same elementType.
	 *  
	 * @param locator
	 * @param elementType
	 * @param parameters
	 * @return
	 */
	private List<Element> runLocator(Locator locator, ElementType elementType, List<String> parameters) {
		if(defaultRan) {
			if(locator instanceof AbstractElementLocator) {
				((AbstractElementLocator) locator).setDefaultRan(true);
			}
		}
		
		List<Element> results = locator.locateAll(elementType, parameters);
		if(locator.getMessage() != null) {
			messages.add(locator.getMessage());
		}
		
		if(locator instanceof AbstractElementLocator) {
			if(((AbstractElementLocator) locator).isDefaultRan()) {
				defaultRan = true;
			}
		}
		
		return results;
	}
	
	private Map<Class<?>, List<String>> parseParms(List<String> parms) {
		Map<Class<?>, List<String>> map = new LinkedHashMap<Class<?>, List<String>>();
		for(String parm : parms) {
			String[] parts = parm.split(getDelimiterRegex());
			String classname = parts[0];
			Class<?> clazz = null;
			try {
				clazz = Class.forName(classname);
			} 
			catch (ClassNotFoundException e) {
				e.printStackTrace();
				continue;
			}
			List<String> attributes = new ArrayList<String>();
			if(parts.length > 1) {
				for(int i=1; i<parts.length; i++) {
					attributes.add(parts[i]);
				}
			}
			map.put(clazz, attributes);
		}
		return map;
	}

	@Override
	public SearchContext getSearchContext() {
		return searchContext;
	}

	@Override
	public WebDriver getWebDriver() {
		return driver;
	}

	@Override
	public boolean busy() {
		return busy;
	}

	private static String getDelimiterRegex() {
		return "\\" + StringUtils.join(PARAMETER_DELIMITER.split(""), "\\");
	}


	@Override
	public boolean ignoreHidden() {
		return ignoreHidden;
	}

	@Override
	public boolean ignoreDisabled() {
		return ignoreDisabled;
	}
	
	public void setIgnoreHidden(boolean ignoreHidden) {
		this.ignoreHidden = ignoreHidden;
	}
	
	public void setIgnoreDisabled(boolean ignoreDisabled) {
		this.ignoreDisabled = ignoreDisabled;
	}

	@Override
	public String getMessage() {
		if(messages.isEmpty())
			return null;
		StringBuilder message = new StringBuilder();
		for (Iterator<String> iterator = messages.iterator(); iterator.hasNext();) {
			String msg = (String) iterator.next();
			message.append(msg);
			if(iterator.hasNext()) {
				message.append("\r\n");
			}
		}
		return message.toString();
	}

	public static void main(String[] args) {
		WebDriver driver = null;
		try {
			driver = new HtmlUnitDriver();
			driver.get("file:///C:/whennemuth/workspaces/bu_workspace/selenium-test/src/test/resources/html/ProposalLogLookupFrame.htm");
			BatchElementLocator locator = new BatchElementLocator(driver, driver);
			List<Element> elements = locator.locateAll(ElementType.TEXTBOX, Arrays.asList(new String[]{
					"edu.bu.ist.apps.kualiautomation.services.automate.element.LabelElementLocator" + PARAMETER_DELIMITER + "Proposal Number",
					"edu.bu.ist.apps.kualiautomation.services.automate.element.LabelledElementLocator" + PARAMETER_DELIMITER + "Proposal Number"
			}));
			for(Element e : elements) {
				System.out.println(e.getWebElement().getTagName() + ": " + e.getWebElement().getText());
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if(driver != null) {
				driver.close();
				driver.quit();
			}
		}

	}
}
