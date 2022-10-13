package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import java.util.List;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;

import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

public interface Locator {

	public Element locateFirst(ElementType elementType, List<String> parameters);

	public List<Element> locateAll(ElementType elementType, List<String> parameters);
	
	public SearchContext getSearchContext();
	
	public WebDriver getWebDriver();
	
	public boolean busy();
	
	/**
	 * Don't include any hidden elements in search results that have been otherwise found to match.
	 * @return
	 */
	public boolean ignoreHidden();
	
	/**
	 * Don't include any disabled elements in search results that have been otherwise found to match.
	 * @return
	 */
	public boolean ignoreDisabled();
	
	public String getMessage();
}
