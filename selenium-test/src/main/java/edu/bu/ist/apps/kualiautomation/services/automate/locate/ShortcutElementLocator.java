package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import edu.bu.ist.apps.kualiautomation.entity.ConfigShortcut;
import edu.bu.ist.apps.kualiautomation.services.automate.element.BasicElement;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;
import edu.bu.ist.apps.kualiautomation.services.automate.element.XpathElementCache;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.label.LabelElementLocator;

/**
 * Find a WebElement instance whose location has been described as part of a "hierarchy". That is,
 * the WebElement will exist within enclosing html that itself contains other labelling html. For
 * example, a hierarchy might be "label1 > label2 > mylink", which would indicate a hyperlink having
 * text "mylink" that exists inside html containing another element that has text  attribute "label2",
 * that itself is likewise contained in another element that has the text or attribute "label1".
 * 
 * @author wrh
 *
 */
public class ShortcutElementLocator extends AbstractElementLocator {

	private static final int DEFAULT_TIMEOUT_SECONDS = 10;
	private Integer timeoutSeconds;
	private Parameters parms;
	private ElementType targetElementType;
	private final List<Element> searchResults = new ArrayList<Element>();
	
	private ShortcutElementLocator() {
		super(null, null); // Restrict the default constructor
	}
	
	public ShortcutElementLocator(WebDriver driver, ConfigShortcut shortcut, Locator parent) {
		this(driver, driver, shortcut, parent);
	}
	
	public ShortcutElementLocator(WebDriver driver, SearchContext searchContext, ConfigShortcut shortcut, Locator parent) {
		super(driver, parent);
		parms = new Parameters();
		parms.setDriver(driver);
		parms.setSearchContext(searchContext);
		parms.setShortcut(shortcut);
		setTargetElementType();
	}
	
	public ShortcutElementLocator(Parameters parms, Locator parent) {
		super(parms.getDriver(), parent);
		this.parms = parms;
		setTargetElementType();
	}
	
	private void setTargetElementType() {
		if(parms.getShortcut() == null || parms.getShortcut().getElementType() == null) {
			targetElementType = ElementType.HOTSPOT;
		}
		else {
			targetElementType = ElementType.valueOf(parms.getShortcut().getElementType());
		}
	}

	@Override
	protected List<WebElement> customLocate() {
				
		XpathElementCache.clear();
		List<WebElement> results = new ArrayList<WebElement>();
		try {
			String node = parms.getShortcut().getLabelHierarchyParts()[0];
			
			results = find(node, parms.doWait());	
			
			removeUnqualified(results);
			
			if(!results.isEmpty()) {
				if(results.size() == 1) {
					if(!parms.isEndOfHierarchy()) {
						return findNext(results.get(0));
					}
					return results;
				}
				else if(!parms.isEndOfHierarchy()) {
					/**
					 * There is more than one matching heading, but it may still be possible that only one
					 * is part of a hierarchy that terminates at the sought WebElement. Explore down each
					 * hierarchy and determine if this is so.
					 */
					List<WebElement> aggregateResults = new ArrayList<WebElement>();
					for(WebElement result : results) {						
						aggregateResults.addAll(findNext(result));
					}
					return aggregateResults;
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<WebElement>();
		}
		
		return results;
	}
	
	/**
	 * This function performs another call to customLocate(), but with the top level of 
	 * getShortcut.getLabelHierarchyParts() popped off the start. Effectively, this repeats
	 * the search for the target element incrementally restarted one level deeper into the html element 
	 * hierarchy where the element is nested.
	 * 
	 * @param webElmt
	 * @return
	 * @throws Exception
	 */
	private List<WebElement> findNext(WebElement webElmt) throws Exception {
		ElementType etype = ElementType.getInstance(webElmt);
		Parameters newparms = new Parameters();
		
		if(etype.canNavigate()) {						
			webElmt.click();
			newparms.setWebDriverWait(new WebDriverWait(parms.getDriver(), getTimeoutSeconds()));
		}
		
		newparms.setDriver(parms.getDriver());
		WebElement parentElmt = webElmt.findElement(By.xpath("./.."));
		newparms.setSearchContext(parentElmt);
		newparms.setShortcut(getNestedShortcut());
		ShortcutElementLocator locator = new ShortcutElementLocator(newparms, this);
		locator.setTimeoutSeconds(getTimeoutSeconds());
		locator.elementType = targetElementType;
		return locator.customLocate();
	}

	/**
	 * Find an element assuming first that it is a label, secondly that it is a hyperlink or other clickable item.
	 * If clickable, click it and invoke a WebDriverWait assuming that the next element in the hierarchy may not
	 * appear instantaneously.
	 * 
	 * @param clue
	 * @return
	 */
	private List<WebElement> find(String clue, boolean wait) {
		
		List<WebElement> webElements = new ArrayList<WebElement>();
		
		if(wait) {
			try {
				parms.waitPatiently().until(webElementLocated(clue));
			} 
			catch (TimeoutException e) {
				/**
				 * TODO: For every section whose header is a clickable item, the full timeout period will be
				 * reached trying to find an element if that element does not exist. 
				 * Figure out a way to determine if pending html content has fully arrived by other means than finding 
				 * the expected element within that content.
				 */
				System.out.println(String.valueOf(getTimeoutSeconds()) + " second timeout reached trying to find \"" + clue + "\"");
			}
			
			for(Element elmt : searchResults) {
				webElements.add(elmt.getWebElement());
			}
			searchResults.clear();
		}
		else {
			List<String> searchparms = Arrays.asList(new String[]{ clue });
			if(parms.isEndOfHierarchy()) {
				
				// 1) Assume clue is the text or attribute value for a hotspot
				Locator locator = new HotspotElementLocator(parms.getDriver(), parms.getSearchContext(), this);
				searchResults.addAll(locator.locateAll(targetElementType, searchparms));
				
				// 2) Assume that clue can refer to the name of a class (CSS) or a member of a multi-valued class
				if(searchResults.isEmpty()) {
					locator = new ClassBasedElementLocator(parms.getDriver(), parms.getSearchContext(), this);
					searchResults.addAll(locator.locateAll(targetElementType, searchparms));
				}
				
				// 3) Assume that we are looking for any other kind of element.
				if(searchResults.isEmpty()) {
					locator = new BasicElementLocator(parms.getDriver(), parms.getSearchContext(), this);
					searchResults.addAll(locator.locateAll(targetElementType, searchparms));
				}
			}
			else {
				
				// 1) Assume clue is a heading label value and search accordingly
				Locator locator = new LabelElementLocator(parms.getDriver(), parms.getSearchContext(), this);
				searchResults.addAll(locator.locateAll(targetElementType, searchparms));
				
				// 2) Assume clue indicates an attribute value of a heading hotspot element and search accordingly
				if(searchResults.isEmpty()) {
					locator = new HotspotElementLocator(parms.getDriver(), parms.getSearchContext(), this);
					searchResults.addAll(locator.locateAll(targetElementType, searchparms));
				}
			}
			
			for(Element elmt : searchResults) {
				webElements.add(elmt.getWebElement());
			}
		}	
		
		return webElements;
	}

	/**
	 * Get a hierarchy array from the current shortcut with the first element removed and 
	 * base a new, smaller shortcut on it. If a shortcut is thought of as a route, going inward
	 * consecutively past labeling markers toward the center of an html hierarchy where the
	 * target element exists, then a nested shortcut is what you get when you peel off that 
	 * part of the route it took to get its own deeper starting point.
	 * 
	 * @return
	 * @throws Exception
	 */
	private ConfigShortcut getNestedShortcut() throws Exception {
		String[] oldHierarchy = parms.getShortcut().getLabelHierarchyParts();
		String[] newHierarchy = null;
		String identifier = parms.getShortcut().getIdentifier();		
		
		// 1) Get a new hierarchy based on the original hierarchy array.
		if(isHotspot() && !oldHierarchy[oldHierarchy.length-1].equals(identifier)) {
/**
 * RESUME NEXT:
 * TODO: if identifier is null, then parms.isEndOfHierarchy() will evaluate to true prematurely.
 * Try using parms.getShortcut().getElementType() as a stand-in identifier and modify this if logic to test for it.
 * Then make sure all shortcut unit tests still work and then enable and test ShortcutElementLocatorTest5.find02AddButton()
 */
			// Put the shortcut identifier at the end of the hierarchy if it is not already there.
			newHierarchy = Arrays.copyOf(oldHierarchy, oldHierarchy.length+1);
			newHierarchy[newHierarchy.length-1] = identifier;
		}
		else {
			newHierarchy = oldHierarchy;
		}
		
		// 2) Remove the first element of the new hierarchy.
		newHierarchy = Arrays.copyOfRange(newHierarchy, 1, newHierarchy.length);
		
		// 3) Create the new ConfigShortcut based on the shortened hierarchy
		ConfigShortcut subshortcut = (ConfigShortcut) parms.getShortcut().clone();
		subshortcut.setLabelHierarchyParts(newHierarchy);
		
		return subshortcut;
	}
	
	/**
	 * Hidden or disabled WebElements can be removed from the search results if necessary.
	 *  
	 * @param results
	 * @param type
	 * @return
	 */
	private <T> void removeUnqualified(List<T> results) {
		List<T> temp = new ArrayList<T>();
		
		for(T elmt : results) {
			Element tempElmt = null;
			if(elmt instanceof Element) 
				tempElmt = (Element) elmt;
			else if(elmt instanceof WebElement) 
				tempElmt = new BasicElement(driver, ((WebElement) elmt));

			if(!tempElmt.isInteractive()) {
				continue;
			}
			
			temp.add(elmt);
		}
		
		if(results.size() != temp.size()) {
			results.clear();
			results.addAll(temp);
		}
	}
	
	private boolean isHotspot() {
		return ElementType.HOTSPOT.name().equals(parms.getShortcut().getElementType());
	}
	
	/**
	 * This condition evaluates if one or more elements have been found as the result of a 
	 * search by checking the list that stores the results for content. 
	 * 
	 * @param locators
	 * @return
	 */
	private ExpectedCondition<Boolean> webElementLocated(final String heading) {	
		ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {			  
			public Boolean apply(WebDriver drv) {
				find(heading, false);
				boolean foundInteractive = false;
				for(Element e : searchResults) {
					if(e.isInteractive()) {
						foundInteractive = true;
					}
				}
				
				/**
				 * Found a matching element, so end the search, but don't let it get in to the 
				 * search results if it is not qualified (hidden/disabled).
				 */
				removeUnqualified(searchResults);
				
				return foundInteractive;
			}
		};
		return condition;
	} 
	
	private static class Parameters {
		private ConfigShortcut shortcut;
		private WebDriver driver;
		private SearchContext searchContext;
		private WebDriverWait webDriverWait;
		public ConfigShortcut getShortcut() {
			return shortcut;
		}
		public void setShortcut(ConfigShortcut shortcut) {
			this.shortcut = shortcut;
		}
		public WebDriverWait getWebDriverWait() {
			return webDriverWait;
		}
		public void setWebDriverWait(WebDriverWait webDriverWait) {
			this.webDriverWait = webDriverWait;
		}
		public WebDriver getDriver() {
			return driver;
		}
		public void setDriver(WebDriver driver) {
			this.driver = driver;
		}
		public SearchContext getSearchContext() {
			return searchContext == null ? driver : searchContext;
		}
		public void setSearchContext(SearchContext searchContext) {
			this.searchContext = searchContext;
		}
		public boolean doWait() {
			return getWebDriverWait() != null;
		}
		public WebDriverWait waitPatiently() {
			return getWebDriverWait();
		}
		public boolean isEndOfHierarchy() {
			return shortcut.getLabelHierarchyParts().length == 1;
		}
	}

	public Integer getTimeoutSeconds() {
		return timeoutSeconds == null ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
	}

	public void setTimeoutSeconds(Integer timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	@Override
	protected Element getElement(WebDriver driver, WebElement we) {
		Element e = new BasicElement(driver, we);
		e.setElementType(ElementType.SHORTCUT);
		return e;
	}
	
}
