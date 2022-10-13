package edu.bu.ist.apps.kualiautomation.services.automate.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.locate.label.ComparableLabel;
import edu.bu.ist.apps.kualiautomation.util.Utils;

/**
 * Check attributes of the webElement for the specified attributeValue(s) in the order found in attributesToCheck.
 * 
 * @author wrh
 *
 */
public class Attribute {

	private WebElement webElement;
	private ElementType elementType;
	private Set<String> attributesToCheck = new HashSet<String>();
	private StringBuilder message = new StringBuilder();

	public Attribute(WebElement webElement, Set<String> attributesToCheck) {
		this.webElement = AbstractWebElement.wrap(webElement);
		this.attributesToCheck.addAll(attributesToCheck);
		this.elementType = ElementType.getInstance(this.webElement);
	}

	public Attribute(WebElement webElement, String attributeToCheck) {
		this.webElement = AbstractWebElement.wrap(webElement);
		this.attributesToCheck.add(attributeToCheck);
		this.elementType = ElementType.getInstance(this.webElement);
	}

	public boolean exists(String attributeName) {
		return webElement.getAttribute(attributeName) != null;
	}
	
	public String getValue(String attributeName) {
		return webElement.getAttribute(attributeName);
	}
	
	/**
	 * Check attributes of the webElement for the specified attributeValue in the order:
	 * they appear in the attributesToCheck list and return the names of those that were found.
	 * @param attributeValue
	 * @return
	 */
	public List<String> forValue(String attributeValue) {
		
		List<String> found = new ArrayList<String>();
		
		for(String attributeName : attributesToCheck) {
			if(elementType.acceptsKeystrokes() && "value".equalsIgnoreCase(attributeName)) {
				// This attribute will have a value that is the result of user input, so skip it.
				continue;
			}
			String attribval = webElement.getAttribute(attributeName);
			if(Utils.trimIgnoreCaseUnemptyEqual(attributeValue, attribval)) {
				setMessage(attributeName, attributeValue);
				found.add(attributeName);
			}
		}

		if("a".equalsIgnoreCase(webElement.getTagName())) {
			String cleanedVal = ComparableLabel.getCleanedValue(attributeValue);
			String cleanedText = ComparableLabel.getCleanedValue(webElement.getText());
			if(cleanedVal.equalsIgnoreCase(cleanedText)) {
				found.add("innerText");
			}
		}
		
		return found;
	}
	
	public List<String> forValue(String attributeValue, String[] attributesToCheck) {
		this.attributesToCheck = new HashSet<String>(Arrays.asList(attributesToCheck));
		return forValue(attributeValue);
	}
	
	public boolean existsForValue(String attributeValue) {
		return forValue(attributeValue).isEmpty() == false;
	}
	
	public WebElement getWebElement() {
		return webElement;
	}

	private void setMessage(String attributeName, String attributeValue) {
		if(attributeName == null) {
			message.append("Could not find ")
				.append(webElement.getTagName())
				.append(" element attribute having value \"")
				.append(attributeValue)
				.append("\"");
		}
		else {
			message.append("Found ")
				.append(webElement.getTagName())
				.append(" element attribute \"")
				.append(attributeName)
				.append("\" having value \"")
				.append(attributeValue)
				.append("\"");
		}
	}
	public String getMessage() {
		return message.toString();
	}
}
