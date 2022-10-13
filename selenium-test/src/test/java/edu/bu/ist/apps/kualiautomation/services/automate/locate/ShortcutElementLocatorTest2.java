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
 * This provides content for unit tests that concern links that are to be located through the ShortcutElementLocator.
 * The tests focus on items that rely on parent elements to be clicked before becoming accessible.
 * 
 * @author wrh
 *
 */
@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShortcutElementLocatorTest2 extends AbstractJettyBasedTest {
	
	private ShortcutElementLocator locator;

	@Override
	public void setupBefore() { /* Nothing to implement */ }

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("shortcut-page2", "ShortcutTestPage2.htm");
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
		asserter.setUrl("http://localhost:8080/shortcut-page2");
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
		shortcut.setElementType(ElementType.HOTSPOT.name());
		shortcut.setIdentifier("icon-plus");
		shortcut.setLabelHierarchyParts(new String[] {
			"headingB 1", "headingB 2", "headingB 3"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page2");
		asserter.setElementType(ElementType.HOTSPOT);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("u10mjulv", results.get(0).getWebElement().getAttribute("id"));		
	}
	
	@Test
	public void test3() {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		List<Element> results = null;
		
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HOTSPOT.name());
		shortcut.setIdentifier("icon-search");
		shortcut.setLabelHierarchyParts(new String[] {
			"headingC 1", "headingC 2", "headingC 3"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/shortcut-page2");
		asserter.setElementType(ElementType.HOTSPOT);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("u11mjulv", results.get(0).getWebElement().getAttribute("id"));		
	}

}
