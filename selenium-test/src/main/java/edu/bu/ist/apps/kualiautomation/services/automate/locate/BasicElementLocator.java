package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import java.util.List;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.element.BasicElement;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

/**
 * This locator has no custom location logic and only invokes the default logic of the abstract locator class.
 * 
 * @author wrh
 *
 */
public class BasicElementLocator extends AbstractElementLocator {

	private ElementType elementType;
	
	public BasicElementLocator(WebDriver driver, Locator parent) {
		super(driver, parent);
	}
	
	public BasicElementLocator(WebDriver driver, SearchContext searchContext, Locator parent) {
		super(driver, searchContext, parent);
	}
	
	public BasicElementLocator(ElementType elementType, WebDriver driver, SearchContext searchContext, Locator parent) {
		super(driver, searchContext, parent);
		this.elementType = elementType;
	}

	@Override
	protected List<WebElement> customLocate() {
		return super.defaultLocate();
	}

	public Element locateFirst(List<String> parameters) {
		return super.locateFirst(elementType, parameters);
	}

	public List<Element> locateAll(List<String> parameters) {
		return super.locateAll(elementType, parameters);
	}

	@Override
	protected Element getElement(WebDriver driver, WebElement we) {
		return new BasicElement(driver, we);
	}
}
