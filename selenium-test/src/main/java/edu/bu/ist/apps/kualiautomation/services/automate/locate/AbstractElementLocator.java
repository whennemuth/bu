package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import edu.bu.ist.apps.kualiautomation.services.automate.element.AbstractWebElement;
import edu.bu.ist.apps.kualiautomation.services.automate.element.AttributeInspector;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;
import edu.bu.ist.apps.kualiautomation.services.automate.element.XpathElementCache;

public abstract class AbstractElementLocator implements Locator {
	protected SearchContext searchContext;
	protected WebDriver driver;
	protected Locator parent;
	protected ElementType elementType;
	protected List<String> parameters = new ArrayList<String>();
	protected List<WebElement> iframes;
	protected String currentFrameSrc = TOP_LEVEL_WINDOW;
	protected boolean skipParameterMatching;
	protected boolean defaultRan;
	protected Set<String> defaultRanFor = new HashSet<String>();
	/**
	 * busy variable is queried to prevent repeated calls in case locator inside a WebDriverWait.until() method.
	 * The find methods of the WebDriver will no longer return immediately with results but will instead inherit
	 * the timeout of the WebDriverWait method.
	 */
	protected boolean busy; 
	protected boolean skipFrameSearch;
	protected boolean ignoreHidden = true;
	protected boolean ignoreDisabled = true;
	protected String message;
	
	public static boolean printDuration = true;
	public static final String TOP_LEVEL_WINDOW = "top level window";
	
	public AbstractElementLocator(WebDriver driver, Locator parent) {
		this.driver = driver;
		this.searchContext = driver;
		this.parent = parent;
	}
	
	public AbstractElementLocator(WebDriver driver, SearchContext searchContext, Locator parent) {
		this.driver = driver;
		this.searchContext = searchContext;
		this.parent = parent;
	}
	
	@Override
	public Element locateFirst(ElementType elementType, List<String> parameters) {		
		List<Element> results = locateAll(elementType, parameters);
		if(results.isEmpty())
			return null;
		
		return results.get(0);
	}
	
	@Override
	public List<Element> locateAll(ElementType elementType, List<String> parameters) {
		List<Element> results = new ArrayList<Element>();
		long start = 0;
		try {
			if(printDuration)
				start = System.currentTimeMillis();
			
			busy = true;
			defaultRan = false;
			if(elementType != null) {
				this.elementType = elementType;
			}
			this.parameters = parameters;
			
			results = new ArrayList<Element>();
			
			if(framesFirst()) {
				
				loadFrameResults(results);
				
				if(results.isEmpty()) {
					
					loadResults(results);	
				}
			}
			else {			
				
				loadResults(results);
				
				if(results.isEmpty()) {
					
					loadFrameResults(results);
				}
			}
			
		} 
		finally {
			busy = false;
			iframes = null;
			if(printDuration) {
			//if(printDuration && skipFrameSearch) {
				long end = System.currentTimeMillis();
				Long mils = end - start;
				System.out.println(this.getClass().getName() + " found " + String.valueOf(results.size()) + " results in " + mils.toString() + " milliseconds for parameter(s):");
				for(String p : parameters) {
					System.out.println(p);
				}
			}				
		}
		
		return results;
	}
	
	/**
	 * Execute the searches - custom search first, followed by the default search if no results from custom.
	 * @param webElements
	 */
	private void loadResults(List<Element> results) {

		List<WebElement> webElements = new ArrayList<WebElement>();
		
		List<WebElement> custom = customLocate();
		
		webElements.addAll(
				removeHidden(
				removeDisabled(
				removeDuplicates(custom))));
		
		if(webElements.isEmpty()) {			
			List<WebElement> defaults = defaultLocate();			
			webElements.addAll(
					removeHidden(
					removeDisabled(
					removeDuplicates(defaults))));
		}
		
		for(WebElement we : webElements) {
			results.add(getElement(driver, we));
		}
	}
	
	/**
	 * Search for web elements within a frame.
	 * 
	 * @param elementType
	 * @param parameters
	 * @return
	 */
	private void loadFrameResults(List<Element> results) {
		
		if(skipFrameSearch) {
			return;
		}
		
		if(!iframes.isEmpty()) {
			for(WebElement iframe : iframes) {
				XpathElementCache.clear();
				// (new WebDriverWait(driver, 5)).until(ExpectedConditions.visibilityOf(iframe));
				if(parent == null) {
					currentFrameSrc = iframe.getAttribute("src");
					(new WebDriverWait(driver, 5)).until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframe));
				}
				skipFrameSearch = true;
				searchContext = driver;
				try {
					List<Element> frameResults = locateAll(elementType, parameters);
					results.addAll(frameResults);
					if(frameResults.isEmpty() && parent == null) {
						driver.switchTo().defaultContent();
						searchContext = driver;
						currentFrameSrc = TOP_LEVEL_WINDOW;
					}
					else {
						// Don't switch back to the parent window because you will not be able to use the WebElement as it would 
						// then belong to a frame that the WebDriver is no longer focused on ( you will get a StaleElementReferenceException ).
						// driver.switchTo().defaultContent();
					}
				}
				finally {
					skipFrameSearch = false;
				}
			}
		}
	}
	
	/**
	 * By experience, most elements are to be found in a frame (at least for kuali). So, searching
	 * should start inside the frame and only be conducted outside of the frame if no results are found.
	 * However, shortcuts are usually found outside of frames as peripheral menu items.
	 * NOTE: This is arbitrary and favors searching norms for kuali - a future enhancement might be to inject this logic.
	 * @return
	 */
	private boolean framesFirst() {
		if(iframes == null)
			iframes = searchContext.findElements(By.tagName("iframe"));
		if(iframes.isEmpty())
			return false;
		return !ElementType.SHORTCUT.equals(elementType);
	}
	
	private List<WebElement> removeDuplicates(List<WebElement> results) {
		List<WebElement> set = new ArrayList<WebElement>();
		outerloop:
		for (Iterator<WebElement> iterator = results.iterator(); iterator.hasNext();) {
			WebElement candidate = iterator.next();
			for(WebElement unique : set) {
				if(candidate.equals(unique))
					continue outerloop;				
			}
			set.add(candidate);
		}
		return set;
	}

	private List<WebElement> removeHidden(List<WebElement> results) {
		List<WebElement> webElements = new ArrayList<WebElement>();
		
		for(WebElement found : results) {
			if(ignoreHidden && !found.isDisplayed())
				continue;
			if("0".equals(found.getAttribute("width")) || "0".equals(found.getAttribute("length"))) {
				// The firefox webdriver lies about width and height attributes - sometimes having neither, 
				// getAttribute("width") still returns "0". Therefore verify by looking at the outerhtml. 
				// NOTE: the javascript call takes time, so don't use this method if it will be repeated many times during a cycle.
				if(hiddenByAttribute(found)) {
					continue;
				}
			}
			webElements.add(found);
		}
		return webElements;
	}

	/**
	 * Find out if a web element has a width or height attribute with a zero value by looking at its outerHTML.
	 * NOTE: This can suck up time if it is used repeatedly (can be 2 seconds for each execution).
	 * @param we
	 * @return
	 */
	private boolean hiddenByAttribute(WebElement we) {
		long start = System.currentTimeMillis();
		boolean hidden = false;
		String html = (String) ((JavascriptExecutor) driver).executeScript(
				"return arguments[0].outerHTML", AbstractWebElement.unwrap(we));
		html = html.split(">")[0];
		if(html.matches(".*width\\s*=\\s*[\"']?[^\"'1-9][\"']?.*")) {
			hidden = true;
		}
		if(html.matches(".*height\\s*=\\s*[\"']?[^\"'1-9][\"']?.*")) {
			hidden = true;
		}
		System.out.println(System.currentTimeMillis() - start);
		return hidden;
	}
	
	private List<WebElement> removeDisabled(List<WebElement> results) {
		List<WebElement> webElements = new ArrayList<WebElement>();
		
		for(WebElement found : results) {
			if(ignoreDisabled && !found.isEnabled())
				continue;
			webElements.add(found);
		}
		return webElements;
	}
	
	public void setDefaultRan(boolean defaultRan) {
//		this.defaultRan = defaultRan;
		this.defaultRanFor.add(TOP_LEVEL_WINDOW);
	}
	
	public boolean isDefaultRan() {
//		return defaultRan;
		return defaultRanFor.contains(TOP_LEVEL_WINDOW);
	}
	
	@Override
	public boolean busy() {
		return busy;
	}

	protected abstract List<WebElement> customLocate();
	
	protected abstract Element getElement(WebDriver driver, WebElement we);
	
	/**
	 * This is the default method for locating a WebElement on an html page.
	 * It contains generic methods for matching and is invoked if the custom method found no results.
	 * 
	 * @return
	 */
	protected List<WebElement> defaultLocate() {
		
		List<WebElement> results = new ArrayList<WebElement>();
		List<WebElement> candidates = new ArrayList<WebElement>();
		
		if(defaultRanFor.contains(currentFrameSrc) || parameters.isEmpty() || elementType == null) 
			return results;
		
//		if(defaultRan || parameters.isEmpty() || elementType == null)
//			return results;
		
		switch(elementType) {
		case BUTTON:
		case BUTTONIMAGE:
		case CHECKBOX:
		case HYPERLINK:
		case TEXTBOX:
		case PASSWORD: 
		case TEXTAREA:
		case SELECT:
		case RADIO:
		case HOTSPOT:
		case OTHER:
			candidates = elementType.findAll(searchContext, skipFrameSearch);
			if(skipParameterMatching) {
				results.addAll(candidates);
			}
			else {
				AttributeInspector inspector = new AttributeInspector(candidates);
				results.addAll(inspector.findForValues(parameters));
			}
			break;
		case SHORTCUT: case SCREENSCRAPE:
			break;
		default:
			break;
		}
		
		defaultRan = true;
		
		defaultRanFor.add(currentFrameSrc);
		
		return results;
	}
	
	/**
	 * WebElement.getText() will always return an empty string if the WebElement is not displayed,
	 * even if it has innerText. This function gets the text regardless of the WebElements display status.
	 *  
	 * @param driver
	 * @param we
	 * @return
	 */
	public static String getText(WebDriver driver, WebElement we) {
		if(we.isDisplayed()) {
			return we.getText();
		}
		String txt = we.getAttribute("textContent");
		
		if(txt != null && !txt.trim().isEmpty())
			return txt;
		
		txt = (String) ((JavascriptExecutor) driver).executeScript(
				"return arguments[0].innerHTML", 
				AbstractWebElement.unwrap(we));
		return txt;
	}
	
	@Override
	public SearchContext getSearchContext() {
		return searchContext;
	}

	@Override
	public WebDriver getWebDriver() {
		return driver;
	}

	public void setSkipParameterMatching(boolean skipParameterMatching) {
		this.skipParameterMatching = skipParameterMatching;
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

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
