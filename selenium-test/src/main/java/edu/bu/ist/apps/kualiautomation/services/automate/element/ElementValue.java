package edu.bu.ist.apps.kualiautomation.services.automate.element;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ElementValue {

	private WebDriver driver;
	private String value;
	private Element element;
	private WebDriverWait wait;
	private boolean navigation;
	
	public ElementValue(WebDriver driver, String value) {
		this.driver = driver;
		if(value != null)
			this.value = value.trim();
		this.wait = new WebDriverWait(driver, 10);
	}

	public boolean applyTo(Element element, boolean navigate) {

		this.element = element;
		
		switch(element.getElementType()) {
		case TEXTBOX: case TEXTAREA: case PASSWORD:
			element.getWebElement().clear();
			element.getWebElement().sendKeys(value);
			break;
		case CHECKBOX: case RADIO:
			if(shouldCheck()) {
				element.click();
			}
			break;
		case BUTTON: case BUTTONIMAGE: case HOTSPOT: case HYPERLINK: case OTHER:
			element.click();
			break;
		case SELECT:
			Select select = new Select(element.getWebElement());
			try {
				select.selectByVisibleText(value);
			} 
			catch (NoSuchElementException e) {
				try {
					select.selectByValue(value);
				} 
				catch (NoSuchElementException e1) {
					if(value.matches("\\d+")) {
						try {
							// Assume the user is not basing the index on zero, but one (so subtract one from it).
							int idx = Integer.valueOf(value);
							if(idx < 0) 
								idx = 0;
							if(idx > 0)
								idx--;
							select.selectByIndex(idx);
						} 
						catch (NoSuchElementException e2) {
							// Do nothing. No matching option element can be found.
						}
					}
				}
			}
			// TODO: Match by partial text?
			break;
		case SCREENSCRAPE:
			// Do nothing. ScreenScrape instances find and apply their own values to themselves.
			return true;
		case SHORTCUT:
			element.click();
			break;
		default:
			break;
		}
		
		if(navigate && element.getElementType().canNavigate()) {
			wait.until(arrivedAtNextPage());	
		}
		
		return true;
	}
	
	private boolean shouldCheck() {
		if(wantChecked() && !isChecked())
			return true;
		if(!wantChecked() && isChecked())
			return true;
		return false;
	}
	
	private boolean isChecked() {
		return "true".equalsIgnoreCase(element.getWebElement().getAttribute("checked"));
	}

	private boolean wantChecked() {
		return "true".equalsIgnoreCase(value);
	}

	public boolean isNavigation() {
		return navigation;
	}

	public void setNavigation(boolean navigation) {
		this.navigation = navigation;
	}

	private Boolean waitingForNextPage = false;
	private ExpectedCondition<Boolean> arrivedAtNextPage() {		
		ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {			  
			public Boolean apply(WebDriver drv) {
				if(waitingForNextPage) {
					System.out.println("busy finding next page");
					return false;
				}
				try {
					waitingForNextPage = true;
					element.getWebElement().findElements(By.id("does not matter"));
				} 
				catch (StaleElementReferenceException e) {
					if(!driver.findElements(By.tagName("html")).isEmpty()) {
						return true;
					}
				}
				catch(Exception e) {
					// The apply method will ignore checked exceptions, so wrap them in RuntimeExceptions
					throw new RuntimeException(e);
				}
				finally {
					waitingForNextPage = false;
				}
				return false;
			}
		};
		return condition;
	}

}
