package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.runners.MockitoJUnitRunner;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;
import edu.bu.ist.apps.kualiautomation.ElementsAssertion;
import edu.bu.ist.apps.kualiautomation.entity.ConfigShortcut;
import edu.bu.ist.apps.kualiautomation.entity.LabelAndValue;
import edu.bu.ist.apps.kualiautomation.services.automate.RunLog;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShortcutElementLocatorTest5 extends AbstractJettyBasedTest {

	@Override
	public void setupBefore() { /* TODO Auto-generated method stub */ }

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		
		handlers.put("inner", ""
				+ "<html><body leftmargin=10 rightmargin=10>"
				+ "    <h3>This is the inner page</h3>"
				+ "    <span>MY</span>"
				+ "    <div>"
				+ "        <span>NESTED</span>"
				+ "        <div>"
				+ "            <input id='button1' type='button' value='click me 1'>"
				+ "            <input id='button2' type='button' value='click me 2'>"
				+ "        </div>"
				+ "    </div>"
				+ "</body></html>");
		handlers.put("outer", ""
				+ "<html><body leftmargin=50 rightmargin=50>"
				+ "    <p><h3>This is the outer page</h3></p>"
				+ "    <iframe src='http://localhost:8080/inner' style='width:50%; height:50%;'></iframe>"
				+ "</body></html>");

		handlers.put("subaward-entry-1", "SubawardEntry.htm");
		handlers.put("SubawardEntry_files", "SubawardEntry_files");
	}

	@Test
	public void find01AddButton() {
		
		RunLog runlog = new RunLog(true);
		LocatorRunner runner = new LocatorRunner(driver, runlog);

		ConfigShortcut shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setIdentifier(null);
		shortcut.setElementType(ElementType.BUTTON.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"MY", "NESTED", "button1"
		});		

		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.SHORTCUT.name());
		lv.setConfigShortcut(shortcut);
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/outer")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "button")
		.addAttributeAssertion("id", "button1")
		.findAndAssertElements();		
	}

	// TODO: See the TODO in ShortcutElementLocator.getNestedShortcut()
	//@Test
	public void find02AddButton() {
		ConfigShortcut shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setIdentifier(null);
		shortcut.setElementType(ElementType.BUTTON.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"Contacts", "Actions"
		});		

		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.SHORTCUT.name());
		lv.setConfigShortcut(shortcut);
		
		ShortcutElementLocator locator = new ShortcutElementLocator(driver, shortcut, null);
		
		new ElementsAssertion(locator)
		.setUrl("http://localhost:8080/subaward-entry-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "image")
		.addAttributeAssertion("name", "methodToCall.addContacts.anchorContacts")
		.findAndAssertElements();		
	}

	// TODO: Get find02AddButton() working. Then, this test will still not work because use of the LocatorRunner
	// causes a StaleReferenceException - fix this.
	//@Test
	public void find03AddButton() {
		RunLog runlog = new RunLog(true);
		LocatorRunner runner = new LocatorRunner(driver, runlog);		
		
		ConfigShortcut shortcut = new ConfigShortcut();
		shortcut.setNavigates(true);
		shortcut.setIdentifier(null);
		shortcut.setElementType(ElementType.BUTTON.name());
		shortcut.setLabelHierarchyParts(new String[] {
			"Contacts", "Actions"
		});		

		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.SHORTCUT.name());
		lv.setConfigShortcut(shortcut);
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-entry-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "image")
		.addAttributeAssertion("name", "methodToCall.addContacts.anchorContacts")
		.findAndAssertElements();		
	}

}
