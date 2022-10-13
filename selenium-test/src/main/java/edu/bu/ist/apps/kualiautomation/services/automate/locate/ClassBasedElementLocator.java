package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.element.Attribute;
import edu.bu.ist.apps.kualiautomation.services.automate.element.AttributeInspector;
import edu.bu.ist.apps.kualiautomation.services.automate.element.BasicElement;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;

/**
 * Locate html elements whose class attribute(s) 
 *    1) match the parameters list 
 *       OR... 
 *    2) match some of the parameters list, in which case, the remaining parameters must match attributes of the html element.
 * @author Warren
 *
 */
public class ClassBasedElementLocator extends AbstractElementLocator implements Locator {

	public ClassBasedElementLocator(WebDriver driver, Locator parent) {
		super(driver, parent);
	}

	public ClassBasedElementLocator(WebDriver driver, SearchContext searchContext, Locator parent) {
		super(driver, searchContext, parent);
	}

	@Override
	protected List<WebElement> customLocate() {

		List<WebElement> bestMatches = new ArrayList<WebElement>();
		List<RateableWebElement> ratings = new ArrayList<RateableWebElement>();
		setSkipParameterMatching(true); // We will be doing our own custom parameter matching here.
		List<WebElement> candidates = super.defaultLocate();
		
		int id = 1;
		for(WebElement candidate : candidates) {
			RateableWebElement rateable = new RateableWebElement(id++, candidate, parameters);
			if(rateable.qualifies()) {
				ratings.add(rateable);
			}
		}
		
		if(ratings.isEmpty())
			return bestMatches;
		
		Collections.sort(ratings);
		
		// Remove those elements that rate lower than any other element in the set until only one or the highest tied are left.
		do {
			RateableWebElement first = ratings.get(0);
			RateableWebElement last = ratings.get(ratings.size()-1);
			
			if(last.compareTo(first) == 0) {
				break;
			}
			ratings.remove(first); // remove the first (lower ranked) element
		} 
		while(true);
		
		for(RateableWebElement rateable : ratings) {
			bestMatches.add(rateable.getWebElement());
		}
		
		return bestMatches;
	}

	public static class RateableWebElement implements Comparable<RateableWebElement> {
		private int id;
		private WebElement webElement;
		private List<String> classMatches = new ArrayList<String>();
		private List<String> attributeMatches = new ArrayList<String>();
		private List<String> classes = new ArrayList<String>();
		private List<String> parameters;

		public RateableWebElement(int id, WebElement webElement, List<String> parameters) {
			this.id = id;
			this.webElement = webElement;
			this.parameters = parameters;
			String cls = webElement.getAttribute("class");
			
			// Determine how many parameters match class values of the WebElement
			if(cls != null && !cls.trim().isEmpty()) {
				cls = cls.trim();
				classes.addAll(Arrays.asList(cls.split("\\s+")));
				for(String p : parameters) {
					for(String clazz : classes) {
						if(clazz.equalsIgnoreCase(p)) {
							if(!classMatches.contains(clazz.toLowerCase())) {
								classMatches.add(clazz.toLowerCase());
							}
						}
					}
				}
			}
			
			// Determine how many parameters match attribute values of the WebElement
			if(!classMatches.isEmpty()) {
				for(String p : parameters) {
					AttributeInspector inspector = new AttributeInspector(webElement);
					Map<String, String> attributes = inspector.getAttributes();
					Attribute attribute = new Attribute(webElement, attributes.keySet());
					if(attribute.existsForValue(p)) {
						if(!attributeMatches.contains(p.toLowerCase())) {
							attributeMatches.add(p.toLowerCase());
						}
					}
				}
			}
		}

		/**
		 * Returns a negative integer, zero, or a positive integer as this object is less than, 
		 * equal to, or greater than the specified object. An instance with more class matches is 
		 * greater, followed by the instance with more attribute matches
		 */
		@Override 
		public int compareTo(RateableWebElement other) {
			if(other == null)
				return 1;
			if(this.qualifies() && !other.qualifies())
				return 1;
			if(!this.qualifies() && other.qualifies())
				return -1;
			if(classMatches.size() > other.getClassMatches().size())
				return 1;
			if(classMatches.size() < other.getClassMatches().size())
				return -1;			
			if(attributeMatches.size() > other.getAttributeMatches().size())
				return 1;
			if(attributeMatches.size() < other.getAttributeMatches().size())
				return -1;

			return 0;
		}

		public boolean qualifies() {
			if(classMatches.isEmpty())
				return false;
			if((classMatches.size() + attributeMatches.size()) < parameters.size())
				return false;
			return true;
		}

		public WebElement getWebElement() {
			return webElement;
		}

		public List<String> getClassMatches() {
			return classMatches;
		}

		public List<String> getAttributeMatches() {
			return attributeMatches;
		}
	}

	@Override
	protected Element getElement(WebDriver driver, WebElement we) {
		return new BasicElement(driver, we);
	}
	
	
	
	
	
	
	
	
	
	
}
