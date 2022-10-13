package edu.bu.ist.apps.kualiautomation.services.automate.element;

import org.openqa.selenium.WebElement;

public interface Element {

	public WebElement getWebElement();
	
	public WebElement getLabel();
	
	public boolean isInteractive();
	
	public void click();
	
	public void setValue(String value);
	
	public String getValue();
	
	public ElementType getElementType();

	public void setElementType(ElementType elementType);
	
}
