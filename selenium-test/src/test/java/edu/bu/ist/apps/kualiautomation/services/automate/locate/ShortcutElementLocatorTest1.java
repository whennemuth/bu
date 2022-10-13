package edu.bu.ist.apps.kualiautomation.services.automate.locate;

/**
 * This provides content for unit tests that concern links that are to be located through the 
 * ShortcutElementLocator. The tests include anchor tags, buttons, and hidden/disabled items.
 */
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

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShortcutElementLocatorTest1 extends AbstractJettyBasedTest {
	
	private ShortcutElementLocator locator;

	@Override
	public void setupBefore() { /* Nothing to implement */ }

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("shortcut-page1", "ShortcutTestPage1.htm");
	}

	@Test
	public void test1() throws CloneNotSupportedException {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		List<Element> results = null;
		
		// 1)
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HYPERLINK.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingA 1", "headingA 2", "headingA 3", "target 1"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page1");
		asserter.setElementType(ElementType.HYPERLINK);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("target 1", results.get(0).getWebElement().getText());
		
		// 2)
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HOTSPOT.name());
		shortcut.setIdentifier("icon-plus");
		shortcut.setLabelHierarchyParts(new String[] {
			"headingA 1", "headingA 2", "headingA 3"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page1");
		asserter.setElementType(ElementType.HOTSPOT);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("u10mjulv", results.get(0).getWebElement().getAttribute("id"));
		
		// 3)
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HOTSPOT.name());
		shortcut.setIdentifier("icon-search");
		shortcut.setLabelHierarchyParts(new String[] {
			"headingA 1", "headingA 2", "headingA 3"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page1");
		asserter.setElementType(ElementType.HOTSPOT);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("u11mjulv", results.get(0).getWebElement().getAttribute("id"));
		
		// 4)
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HOTSPOT.name());
		shortcut.setIdentifier("uif-actionLink");
		shortcut.setLabelHierarchyParts(new String[] {
			"headingA 1", "headingA 2", "headingA 3"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page1");
		asserter.setElementType(ElementType.HOTSPOT);
		asserter.setNumResults(2);
		asserter.findAndAssertElements();
	}
	
	
	@Test
	public void test2() throws CloneNotSupportedException {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		List<Element> results = null;
		
		// 1)
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setIdentifier(null);
		shortcut.setElementType(ElementType.BUTTON.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingB 1", "headingB 2", "headingB 3", "target 2"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page1");
		asserter.setElementType(ElementType.BUTTON);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("target 2", results.get(0).getWebElement().getAttribute("value"));
		
		// 2)
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setIdentifier(null);
		shortcut.setElementType(ElementType.BUTTON.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingB 1", "headingB 2", "headingB 3", "icon-plus"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page1");
		asserter.setElementType(ElementType.BUTTON);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("u10mjulv", results.get(0).getWebElement().getAttribute("id"));
		
		// 3)
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setIdentifier(null);
		shortcut.setElementType(ElementType.BUTTON.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingB 1", "headingB 2", "headingB 3", "u10mjulv"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page1");
		asserter.setElementType(ElementType.BUTTON);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("u10mjulv", results.get(0).getWebElement().getAttribute("id"));
	}
	
	
	@Test
	public void test3() throws CloneNotSupportedException {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		
		// 1)
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setIdentifier(null);
		shortcut.setElementType(ElementType.HYPERLINK.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingC 1", "headingC 2", "headingC 3", "target 3"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page1");
		asserter.setElementType(ElementType.HYPERLINK);
		asserter.setNumResults(0);
		asserter.findAndAssertElements();
		
		// 2)
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HOTSPOT.name());
		shortcut.setIdentifier("icon-plus");
		shortcut.setLabelHierarchyParts(new String[] {
			"headingC 1", "headingC 2", "headingC 3"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page1");
		asserter.setElementType(ElementType.HOTSPOT);
		asserter.setNumResults(0);
		asserter.findAndAssertElements();
	}
	
	
	@Test
	public void test4() throws CloneNotSupportedException {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		
		// 1)
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setIdentifier(null);
		shortcut.setElementType(ElementType.BUTTON.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"headingD 1", "headingD 2", "headingD 3", "target 4"
		});	
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page1");
		asserter.setElementType(ElementType.BUTTON);
		asserter.setNumResults(0);
		asserter.findAndAssertElements();
		
		// 2)
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HOTSPOT.name());
		shortcut.setIdentifier("icon-plus");
		shortcut.setLabelHierarchyParts(new String[] {
			"headingD 1", "headingD 2", "headingD 3"
		});	
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page1");
		asserter.setElementType(ElementType.HOTSPOT);
		asserter.setNumResults(0);
		asserter.findAndAssertElements();
	}
}
