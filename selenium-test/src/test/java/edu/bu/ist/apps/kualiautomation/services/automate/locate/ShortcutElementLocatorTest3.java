package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.runners.MockitoJUnitRunner;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;
import edu.bu.ist.apps.kualiautomation.ElementsAssertion;
import edu.bu.ist.apps.kualiautomation.entity.ConfigShortcut;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

/**
 * This provides content for unit tests that concern links that are to be located through the 
 * ShortcutElementLocator. The tests focus on finding elements within parent elements, where the 
 * search for the parent produced more than one result. The functionality must explore into each 
 * potential parent to find that only one has the sought child element.
 * 
 * @author wrh
 *
 */
@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShortcutElementLocatorTest3 extends AbstractJettyBasedTest {
	
	private ShortcutElementLocator locator;

	@Override
	public void setupBefore() { /* Nothing to implement */ }

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("shortcut-page3", "ShortcutTestPage3.htm");
	}
	
	@Test
	public void test1() {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		List<Element> results = null;
		
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HYPERLINK.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingA 1", "headingA 2", "headingA 3", "target 1"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page3");
		asserter.setElementType(ElementType.HYPERLINK);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("target 1", results.get(0).getWebElement().getText());		
	}
	
	@Test
	public void test2() {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		List<Element> results = null;
		
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HYPERLINK.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingB 1", "headingB 2", "headingB 3", "target 2"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page3");
		asserter.setElementType(ElementType.HYPERLINK);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("target 2", results.get(0).getWebElement().getText());		
	}
	
	@Test
	public void test3() {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		List<Element> results = null;
		
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HYPERLINK.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingC 1", "headingC 2", "headingC 3", "target 3"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page3");
		asserter.setElementType(ElementType.HYPERLINK);
		asserter.setNumResults(2);
		results = asserter.findAndAssertElements();
		assertEquals("target 3", results.get(0).getWebElement().getText());		
	}
	
	@Test
	public void test4() {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		List<Element> results = null;
		
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HYPERLINK.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingD 1", "headingD 2", "headingD 3", "target 4"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page3");
		asserter.setElementType(ElementType.HYPERLINK);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("target 4", results.get(0).getWebElement().getText());		
	}
	
	@Test
	public void test5() {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		List<Element> results = null;
		
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HYPERLINK.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingE 1", "headingE 2", "headingE 3", "target 5"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page3");
		asserter.setElementType(ElementType.HYPERLINK);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("target 5", results.get(0).getWebElement().getText());		
	}
	
	@Test
	public void test6() {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		List<Element> results = null;
		
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HYPERLINK.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingF 1", "headingF 2", "headingF 3", "target 6"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		locator.setTimeoutSeconds(3);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page3");
		asserter.setElementType(ElementType.HYPERLINK);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("target 6", results.get(0).getWebElement().getText());		
	}

}
