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
 * Test menu selection on the main kuali-research welcome page.
 * 
 * @author wrh
 *
 */
@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShortcutElementLocatorTest4 extends AbstractJettyBasedTest {
	
	private ShortcutElementLocator locator;

	@Override
	public void setupBefore() { /* Nothing to implement */ }

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("welcome-page", "welcome.htm");
		handlers.put("welcome_files", "welcome_files");
	}
	
	@Test
	public void test1() {
		ConfigShortcut shortcut = null;
		ElementsAssertion asserter = null;
		List<Element> results = null;
		
		
		// 1) RESEARCHER menu
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HYPERLINK.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"RESEARCHER", "IACUC Protocols", "Lists", "All My Schedules"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/welcome-page");
		asserter.setElementType(ElementType.HYPERLINK);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("All My Schedules", results.get(0).getWebElement().getText());	
		
		
		// 2) UNIT menu
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setIdentifier("icon-plus");
		shortcut.setElementType(ElementType.HOTSPOT.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"unit", "Pre-submission compliance", "protocols", "animals"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/welcome-page");
		asserter.setElementType(ElementType.HOTSPOT);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("uhs1jvz", results.get(0).getWebElement().getAttribute("id"));
		
		
		// 3) CENTRAL ADMIN menu
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setIdentifier("icon-search");
		shortcut.setElementType(ElementType.HOTSPOT.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"central admin", "pre-award", "proposal log"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/welcome-page");
		asserter.setElementType(ElementType.HOTSPOT);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("u1qogous", results.get(0).getWebElement().getAttribute("id"));		
		
		
		// 4) User dropdown menu
		shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setElementType(ElementType.HYPERLINK.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"User: wrh", "preferences"
		});		
		locator = new ShortcutElementLocator(driver, shortcut, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/welcome-page");
		asserter.setElementType(ElementType.HYPERLINK);
		asserter.setNumResults(1);
		results = asserter.findAndAssertElements();
		assertEquals("u13jrel9", results.get(0).getWebElement().getAttribute("id"));		
		
	}
}
