package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;
import edu.bu.ist.apps.kualiautomation.ElementsAssertion;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HotspotElementLocatorTest extends AbstractJettyBasedTest {

	private HotspotElementLocator locator;
	
	@Override
	public void setupBefore() { 
		locator = new HotspotElementLocator(driver, null);
	}

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("hotspot-test-page", "HotspotTestPage1.htm");
		handlers.put("subaward-entry-1", "SubawardEntry.htm");
		handlers.put("SubawardEntry_files", "SubawardEntry_files");
	}
	
	/**
	 * No matching label. Button image and image link both have a matching attribute. Button image wins.
	 */
	@Test
	public void test01() {
		new ElementsAssertion(locator)
			.setUrl("http://localhost:8080/hotspot-test-page")
			.setTagNameAssertion("input")
			.setLabel("label1")
			.setElementType(ElementType.HOTSPOT)
			.addAttributeValue("test1")
			.setNumResults(1)
			.addAttributeAssertion("title", "test1")
			.findAndAssertElements();
	}
	
	/**
	 * No matching label. Button image and image link both have a matching attribute. Image link wins.
	 */
	@Test
	public void test02() {
		new ElementsAssertion(locator)
			.setUrl("http://localhost:8080/hotspot-test-page")
			.setTagNameAssertion("a")
			.setLabel("label2")
			.setElementType(ElementType.HOTSPOT)
			.addAttributeValue("test2")
			.setNumResults(1)
			.addAttributeAssertion("name", "test2")
			.findAndAssertElements();
	}
	
	/**
	* No matching label. Button image and an image link both have a matching attribute.
	* Another image link shares the same label, but has the attribute value as its innerText.
	* Both image links win and are tied
	*/
	@Test
	public void test03() {
	new ElementsAssertion(locator)
		.setUrl("http://localhost:8080/hotspot-test-page")
		.setTagNameAssertion("a")
		.setLabel("label3")
		.setElementType(ElementType.HOTSPOT)
		.addAttributeValue("test3")
		.setNumResults(2)
		.addAnyAttributeAssertion("name", "test3")
		.setAnyTextAssertions(new String[]{ "test3" })
		.findAndAssertElements();
	}
	
	/**
	 * 
	 */
	@Test
	public void test04() {
		
		// Find hyperlink without the aid of an attribute (innerText only)
		new ElementsAssertion(locator)
			.setUrl("http://localhost:8080/hotspot-test-page")
			.setTagNameAssertion("a")
			.setLabel("label4")
			.setElementType(ElementType.HOTSPOT)
			.setNumResults(1)
			.addAttributeAssertion("myprop", "test4")
			.addAttributeValue("test4")
			.findAndAssertElements();
	}
	
	@Test
	public void test05() {		
		new ElementsAssertion(locator)
			.setUrl("http://localhost:8080/hotspot-test-page")
			.setTagNameAssertion("input")
			.setLabel("label5b:")
			.setElementType(ElementType.HOTSPOT)
			.setNumResults(1)
			.addAttributeValue("test5")
			.addAttributeAssertion("type", "image")
			.addAttributeAssertion("myprop", "bettermatch")
			.findAndAssertElements();
	}
	
	@Test
	public void test06SubawardSaved() {
		new ElementsAssertion(locator)
			.setUrl("http://localhost:8080/subaward-entry-1")
			.setTagNameAssertion("input")
			.setLabel("Requisitioner User Name:")
			.setElementType(ElementType.HOTSPOT)
			.setNumResults(1)
			.addAttributeValue("Search")
			.addAttributeAssertion("title", "Search")
			.addAttributeAssertion("type", "image")
			.findAndAssertElements();		
	}
	
	@Test public void test07SubawardSavedFindTab() {
		new ElementsAssertion(locator)
		.setUrl("http://localhost:8080/subaward-entry-1")
		.setTagNameAssertion("input")
		.setLabel("Financial")
		.setElementType(ElementType.HOTSPOT)
		.setNumResults(1)
		.addAttributeAssertion("type", "submit")
		.findAndAssertElements();				
	}
	
	
}
