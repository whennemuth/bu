package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;
import edu.bu.ist.apps.kualiautomation.ElementsAssertion;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperlinkElementLocatorTest extends AbstractJettyBasedTest {

	static {
		// This test operates on iframe content directly without outer page and its scripts present.
		// Normally this would cause a javascript exception due to the missing scripts, so suppress. 
		javascriptIgnoreExceptions = true;		
	}

	private HyperlinkElementLocator locator;
	private String url = "http://localhost:8080/hyperlink-page";
	
	@Override
	public void setupBefore() { 
		locator = new HyperlinkElementLocator(driver, null);
	}

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("hyperlink-page", "HyperlinkPage.htm");
	}
	
	@Test
	public void assert01Links1and2() {		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("anchor tag 1")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a1")
		.findAndAssertElements();		
	}
	
	@Test
	public void assert02Links1and2() {				
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("anchor TAG 2")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a2")
		.findAndAssertElements();
	}
	
	@Test
	public void assert03Links1and2() {			
		new ElementsAssertion(locator)
		.setUrl(url)
		.addAttributeValue("anchor tag 2")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a2")
		.findAndAssertElements();
	}
	
	@Test
	public void assert04Links1and2() {			
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("anchor tag")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(9)
		.findAndAssertElements();
	}

	@Test
	public void assert05Links4() {		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("anchor tag 4")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(2)
		.findAndAssertElements();
	}

	@Test
	public void assert06Links4() {		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("anchor tag 4")
		.addAttributeValue("anchor4")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a5")
		.findAndAssertElements();
	}

	@Test
	public void assert07Links4() {		
		new ElementsAssertion(locator)
		.setUrl(url)
		.addAttributeValue("anchor4")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a5")
		.findAndAssertElements();
	}

	@Test
	public void assert08Links4() {		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("label 4")
		.addAttributeValue("anchor tag 4")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a6")
		.findAndAssertElements();
	}

	@Test
	public void assert09Links5() {		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("anchor tag 5")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(2)
		.findAndAssertElements();
	}

	@Test
	public void assert10Links5() {		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("anchor tag 5")
		.addAttributeValue("anchor5")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a8")
		.findAndAssertElements();
	}

	@Test
	public void assert11Links5() {		
		new ElementsAssertion(locator)
		.setUrl(url)
		.addAttributeValue("anchor5")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a8")
		.findAndAssertElements();
	}

	@Test
	public void assert12Links5() {		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("label 5a")
		.addAttributeValue("anchor tag 5")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(2)
		.findAndAssertElements();
	}

	@Test
	public void assert13Links6() {
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("label 6")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a9")
		.findAndAssertElements();
	}

	@Test
	public void assert14Links6() {
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("anchor tag 6")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a9")
		.findAndAssertElements();
	}

	@Test
	public void assert15Links6() {
		new ElementsAssertion(locator)
		.setUrl(url)
		.addAttributeValue("anchor tag 6")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a9")
		.findAndAssertElements();
	}

	@Test
	public void assert16Links6() {
		new ElementsAssertion(locator)
		.setUrl(url)
		.addAttributeValue("label 6")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a9")
		.findAndAssertElements();
	}

	@Test
	public void assert17Links6() {
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("top label")
		.addAttributeValue("anchor tag 6")
		.setElementType(ElementType.HYPERLINK)
		.setNumResults(1)
		.addAttributeAssertion("testid", "a9")
		.findAndAssertElements();
	}
}
