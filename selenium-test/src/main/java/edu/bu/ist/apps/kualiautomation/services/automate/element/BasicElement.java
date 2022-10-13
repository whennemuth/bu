package edu.bu.ist.apps.kualiautomation.services.automate.element;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import edu.bu.ist.apps.kualiautomation.util.Utils;

public class BasicElement implements Element {

	private WebDriver driver;
	private WebElement webElement;
	private WebElement label;
	private ElementType elementType;
	
	public BasicElement(WebDriver driver, WebElement webElement) {
		this.driver = driver;
		setWebElement(webElement);
	}
	
	public BasicElement(WebDriver driver, WebElement webElement, WebElement label) {
		this.driver = driver;
		setWebElement(webElement);
		this.label = label;
	}

	@Override
	public WebElement getWebElement() {
		return webElement;
	}
	
	private void setWebElement(WebElement webElement) {
		this.webElement = AbstractWebElement.wrap(webElement);
		if(this.elementType == null) {
			this.elementType = ElementType.getInstance(webElement);
		}
	}

	@Override
	public ElementType getElementType() {
		return elementType;
	}

	@Override
	public void setElementType(ElementType elementType) {
		this.elementType = elementType;
	}

	@Override
	public WebElement getLabel() {
		return label;
	}

	@Override
	public boolean isInteractive() {
		return webElement != null && webElement.isDisplayed() && webElement.isEnabled();
	}

	@Override
	public void click() {
		webElement.click();
	}

	@Override
	public void setValue(String value) {		
		ElementValue ev = new ElementValue(driver, value);
		ev.applyTo(this, false);
	}

	@Override
	public String getValue() {
		if(ElementType.SELECT.equals(elementType)) {
			Select select = new Select(webElement);
			List<WebElement> selected = select.getAllSelectedOptions();
			if(selected.isEmpty()) {
				return "";
			}
			else if(select.isMultiple()) {
				StringBuilder s = new StringBuilder("");
				for(WebElement option : selected) {
					if(s.toString().isEmpty())
						s.append(":");
					if(!Utils.isEmpty(option.getAttribute("value"))) {
						s.append(option.getAttribute("value"));
					}
				}
				return s.toString();
			}
			else {
				return selected.get(0).getAttribute("value");
			}
		}
		else if(ElementType.SCREENSCRAPE.equals(elementType)) {
			return webElement.getText(); // WebElement is Subclassed to return a "scraped" value for getText()
		}
		else if(elementType.isCheckable()) {
			return webElement.getAttribute("checked");
		}
		else {
			return webElement.getAttribute("value");
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("webElement=[ ");
		if(webElement == null) {
			builder.append("null");
		}
		else {
			builder
				.append("label=").append(label).append(", ")
				.append("elementType=").append(elementType.name()).append(", ")
				.append("enabled=").append(Boolean.valueOf(webElement.isEnabled())).append(", ")
				.append("displayed=").append(Boolean.valueOf(webElement.isDisplayed())).append(", ")
				.append("selected=").append(Boolean.valueOf(webElement.isSelected())).append(", ")
				.append("html=<").append(webElement.getTagName()).append(" ");
			for (int i = 0; i < AttributeInspector.DEFAULT_ATTRIBUTES_TO_CHECK.length; i++) {
				String attribute = AttributeInspector.DEFAULT_ATTRIBUTES_TO_CHECK[i];
				builder.append(attribute).append("='").append(String.valueOf(webElement.getAttribute(attribute)));
				builder.append("'");
				if((i+1) == AttributeInspector.DEFAULT_ATTRIBUTES_TO_CHECK.length)
					break;
				builder.append(" ");
			}
			builder.append(">");
		}
		if(!Utils.isEmpty(webElement.getText())) {
			builder.append(webElement.getText());
		}
		builder.append("</").append(webElement.getTagName()).append("> ]");
		return builder.toString();
	}

}
