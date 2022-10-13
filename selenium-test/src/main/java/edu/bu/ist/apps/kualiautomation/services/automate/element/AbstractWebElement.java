package edu.bu.ist.apps.kualiautomation.services.automate.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape.ScreenScrapeWebElement;

/**
 * This class wraps the {@link org.openqa.selenium.WebElement} in an abstract class of the same type
 * so as to allow subclasses to override methods and to that degree effect the Adapter design pattern.
 * 
 * @author wrh
 *
 */
public class AbstractWebElement implements WebElement {

	protected WebElement webElement;
	private String tagname;
	private String text;
	private Map<String, Object> attributes = new HashMap<String, Object>();
	
	public AbstractWebElement(WebElement webElement) {
		this.webElement = webElement;
	}
	
	@Override public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException { return webElement.getScreenshotAs(target); }
	@Override public void click() { webElement.click(); }
	@Override public void submit() { webElement.submit(); }
	@Override public void sendKeys(CharSequence... keysToSend) { webElement.sendKeys(keysToSend); }
	@Override public void clear() { webElement.clear(); }
	@Override public String getTagName() { return getCachedTagname(); }
	@Override public String getAttribute(String name) { return getCachedAttribute(name); }
	@Override public boolean isSelected() { return webElement.isSelected(); }
	@Override public boolean isEnabled() { return webElement.isEnabled(); }
	@Override public String getText() { return getCachedText(); }
	@Override public List<WebElement> findElements(By by) { return webElement.findElements(by); }
	@Override public WebElement findElement(By by) { return webElement.findElement(by); }
	@Override public boolean isDisplayed() { return webElement.isDisplayed(); }
	@Override public Point getLocation() { return webElement.getLocation(); }
	@Override public Dimension getSize() { return webElement.getSize(); }
	@Override public Rectangle getRect() { return webElement.getRect(); }
	@Override public String getCssValue(String propertyName) { return webElement.getCssValue(propertyName); }

	
	@Override
	public boolean equals(Object obj) {
		if(webElement == null) {
			return ScreenScrapeWebElement.areEqual(this, obj);
		}
		if(obj != null && obj instanceof AbstractWebElement) {
			return webElement.equals(((AbstractWebElement) obj).webElement);
		}
		return webElement.equals(obj);
	}

	/**
	 * Calls to getTagName() are expensive (WebDriver in use may be slow), so cache the value the first time accessed.
	 * @return
	 */
	private String getCachedTagname() {
		if(tagname == null) 
			tagname = webElement.getTagName();
		return tagname;
	}

	/**
	 * Calls to getText() are expensive (WebDriver in use may be slow), so cache the value the first time accessed.
	 * @return
	 */
	private String getCachedText() {
		if(text == null) 
			text = webElement.getText();
		return text;
	}
	
	/**
	 * Calls to getAttribute() are expensive (WebDriver in use may be slow), so cache the value the first time accessed.
	 * @return
	 */
	private String getCachedAttribute(String name) {
		if(!attributes.containsKey(name) 
				|| "checked".equalsIgnoreCase(name)
				|| "value".equalsIgnoreCase(name)
				|| "selected".equalsIgnoreCase(name)) {
			attributes.put(name, webElement.getAttribute(name));
		}
		return (String) attributes.get(name);
	}
	
	public static WebElement wrap(WebElement we) {
		if(we == null)
			return null;
		if(we instanceof AbstractWebElement)
			return we;
		return new AbstractWebElement(we);
	}
	
	public static WebElement unwrap(WebElement we) {
		if(we == null)
			return null;
		if(we instanceof AbstractWebElement)
			return ((AbstractWebElement) we).webElement;
		return we;
	}
	
	public static List<WebElement> wrap(List<WebElement> webElements) {
		List<WebElement> wrapped = new ArrayList<WebElement>();
		for(WebElement we : webElements) {
			wrapped.add(AbstractWebElement.wrap(we));
		}
		return wrapped;
	}
	
	public static List<WebElement> unwrap(List<WebElement> webElements) {
		List<WebElement> unwrapped = new ArrayList<WebElement>();
		for(WebElement we : webElements) {
			unwrapped.add(unwrap(we));
		}
		return unwrapped;
	}

}
