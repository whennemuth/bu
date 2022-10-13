package edu.bu.ist.apps.kualiautomation.services.automate.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.util.Utils;

/**
 * Given one or more WebElement instances, this class is used to return those that satisfy a search
 * against their attributes. To satisfy this search, each of a specified list of attribute values 
 * must find a match.
 * 
 * @author wrh
 *
 */
public class AttributeInspector {
	
	public static String[] DEFAULT_ATTRIBUTES_TO_CHECK = new String[] {
			"id", "title", "placeholder", "name", "value"
	};

	private List<WebElement> webElements = new ArrayList<WebElement>();
	private SearchContext searchContext;
	
	public AttributeInspector(WebElement webElement) {
		this.webElements.add(webElement);	
	}
	
	public AttributeInspector(List<WebElement> webElements) {
		this.webElements.addAll(webElements);		
	}
	
	public AttributeInspector(SearchContext searchContext, WebElement webElement) {
		this.searchContext = searchContext;
		this.webElements.add(webElement);		
	}
	
	public AttributeInspector(SearchContext searchContext, List<WebElement> webElements) {
		this.searchContext = searchContext;
		this.webElements.addAll(webElements);		
	}
	
	/**
	 * For a given list of webElements, find each that has an attribute with the specified attribute value.
	 * Of those results, return a list of those whose attribute match are foremost in the array of attributes to check.
	 *   
	 * @param webElements
	 * @param attributeValue
	 * @return
	 */
	private List<WebElement> findForValue(String attributeValue, boolean restrictToList) {
		List<WebElement> results = new ArrayList<WebElement>();
		
		// Find any WebElements that have any attribute with attributeValue as a value.
		for(WebElement we : webElements) {
			Map<String, String> unfiltered = getAttributes(we);
			Attribute attribute = new Attribute(we, unfiltered.keySet());
			List<String> found = attribute.forValue(attributeValue);
			if(!found.isEmpty()) {
				if(searchContext == null && results.isEmpty() && restrictToList) {
					/**
					 * Accept the attribute only if its name is foremost in the array of attributes to check
					 * For example, for all webElements, if there are 2 id matches and 2 title matches, 
					 * only the 2 id matches would put in results.
					 */
					for(String attrib : DEFAULT_ATTRIBUTES_TO_CHECK) {
						if(found.contains(attrib)) {
							results.add(attribute.getWebElement());
						}
					}
				}
				else {
					/** SearchContext is available, which means we got ALL attributes for we */
					results.add(attribute.getWebElement());
				}
			}
		}
		
		return results;
	}
	public List<WebElement> findForValue(String attributeValue) {
		return findForValue(attributeValue, true);
	}
	
	public List<WebElement> findAnyForValue(String attributeValue) {
		return findForValue(attributeValue, false);
	}
	
	/**
	 * For a given list of webElements, find each that has every one of the specified 
	 * attribute values accounted for among its attributes.
	 *  
	 * @param webElements
	 * @param attributeValues
	 * @return
	 */
	public List<WebElement> findForValues(List<String> attributeValues) {
		List<WebElement> filtered = new ArrayList<WebElement>();
		outerloop:
		for(WebElement we : webElements) {			 
			for(String attributeValue : attributeValues) {
				if(Utils.trimIgnoreCaseEqual("null", attributeValue) || Utils.isEmpty(attributeValue))
					continue;
				Map<String, String> unfiltered = getAttributes(we);
				Attribute attribute = new Attribute(we, unfiltered.keySet());
				List<String> found = attribute.forValue(attributeValue);
				if(found.isEmpty()) {
					continue outerloop;
				}
			}
			filtered.add(we);
		}
		return filtered;
	}
	
	public Map<String, String> getAttributes() {
		// Usage assumes we only have one WebElement instance in the list.
		return getAttributes(webElements.get(0));
	}
	
	/**
	 * Get either all the attributes (name and value) a WebElement has or get only those
	 * specified in a list of attribute names (does not require searchContext).
	 * @param we
	 * @return
	 */
	public Map<String, String> getAttributes(WebElement we) {
		if(searchContext == null) {
			Map<String, String> attributes = new HashMap<String, String>();
			 for(String attributeToCheck : DEFAULT_ATTRIBUTES_TO_CHECK) {
				 Attribute attribute = new Attribute(we, attributeToCheck);
				 if(attribute.exists(attributeToCheck)) {
					 attributes.put(attributeToCheck, attribute.getValue(attributeToCheck));
				 }
			 }
			 
			 return attributes;
		}
		else {
			JavascriptExecutor executor = (JavascriptExecutor) searchContext;
			@SuppressWarnings("unchecked")
			Map<String, String> attributes = (Map<String, String>) executor.executeScript(""
					+ "var items = {}; "
					+ "for (i = 0; i < arguments[0].attributes.length; ++i) { "
					+ "   items[arguments[0].attributes[i].name] = arguments[0].attributes[i].value; "
					+ "} "
					+ "return items;", 
					AbstractWebElement.unwrap(we));
			
			return attributes;
		}
	}
}
