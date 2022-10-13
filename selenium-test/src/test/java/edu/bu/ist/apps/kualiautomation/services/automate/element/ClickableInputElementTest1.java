package edu.bu.ist.apps.kualiautomation.services.automate.element;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;
import edu.bu.ist.apps.kualiautomation.ElementsAssertion;
import edu.bu.ist.apps.kualiautomation.entity.LabelAndValue;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.label.LabelledElementLocator;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClickableInputElementTest1 extends AbstractJettyBasedTest {
	
	private ElementValue ev;
	@Mock private LabelAndValue lv;

	@Override
	public void setupBefore() { /* Nothing to implement */ }

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("radio-button-page1", "RadioButtonTest.htm");
		handlers.put("select-element-page1", "SelectElementTest.htm");
		handlers.put("checkbox-page1", 
				"<html><body>"
				+ "   <label for='checkbox1'>checkbox 1</label>"
				+ "   <input type='checkbox' id='checkbox1'>"
				+ "</body></html>");
		handlers.put("checkbox-page2", 
				"<html><body>"
				+ "   <label for='checkbox1'>checkbox 1</label>"
				+ "   <input type='checkbox' id='checkbox1' CHECKED>"
				+ "</body></html>");
	}
	
	private Element findAndAssertElement(String page, int index, ElementType etype) {		
		List<Element> results = null;
		ElementsAssertion asserter = null;
		LabelledElementLocator locator = new LabelledElementLocator(driver, null);
		asserter = new ElementsAssertion(locator);
		asserter.setUrl("http://localhost:8080/" + page);
		asserter.setElementType(etype);
		asserter.setNumResults(1);
		asserter.addAttributeValue(etype.name() + " " + String.valueOf(index));
		asserter.addAttributeAssertion("id", (etype.name() + String.valueOf(index)));
		results = asserter.findAndAssertElements();		
		return results.get(0);
	}
	
	private void configureLabelAndValue(int index, String value, ElementType etype) {
		when(lv.getElementType()).thenReturn(etype.name());
		when(lv.getElementTypeEnum()).thenReturn(etype);
		when(lv.getLabel()).thenReturn(etype.name() + " " + String.valueOf(index));
		when(lv.getValue()).thenReturn(value);
	}

	@Test
	public void test1ClickRadioButton() {
		
		// 1) All radio buttons are unchecked
		// ----------------------------------
		// a) Find and assert all radio buttons are NOT checked.
		configureLabelAndValue(1, "true", ElementType.RADIO);
		Element radio1 = findAndAssertElement("radio-button-page1", 1, ElementType.RADIO);
		Element radio2 = findAndAssertElement("radio-button-page1", 2, ElementType.RADIO);
		Element radio3 = findAndAssertElement("radio-button-page1", 3, ElementType.RADIO);
		assertFalse("true".equalsIgnoreCase(radio1.getWebElement().getAttribute("checked")));		
		assertFalse("true".equalsIgnoreCase(radio2.getWebElement().getAttribute("checked")));		
		assertFalse("true".equalsIgnoreCase(radio1.getWebElement().getAttribute("checked")));		
		// b) Check radio1 button
		ev = new ElementValue(driver, lv.getValue());
		ev.applyTo(radio1, false);		
		// c) Re-assert only radio1 is checked.
		assertTrue("true".equalsIgnoreCase(radio1.getWebElement().getAttribute("checked")));
		assertFalse("true".equalsIgnoreCase(radio2.getWebElement().getAttribute("checked")));		
		assertFalse("true".equalsIgnoreCase(radio3.getWebElement().getAttribute("checked")));		
		
		// 2) One radio buttons is already checked
		// ----------------------------------
		// a) Find and assert all radio buttons are NOT checked except one.
		configureLabelAndValue(1, "true", ElementType.RADIO);
		Element radio4 = findAndAssertElement("radio-button-page1", 4, ElementType.RADIO);
		Element radio5 = findAndAssertElement("radio-button-page1", 5, ElementType.RADIO);
		Element radio6 = findAndAssertElement("radio-button-page1", 6, ElementType.RADIO);
		assertFalse("true".equalsIgnoreCase(radio4.getWebElement().getAttribute("checked")));		
		assertTrue("true".equalsIgnoreCase(radio5.getWebElement().getAttribute("checked")));		
		assertFalse("true".equalsIgnoreCase(radio6.getWebElement().getAttribute("checked")));		
		// b) Check radio4 button
		ev = new ElementValue(driver, lv.getValue());
		ev.applyTo(radio4, false);		
		// c) Re-assert only radio4 is checked.
		assertTrue("true".equalsIgnoreCase(radio4.getWebElement().getAttribute("checked")));
		assertFalse("true".equalsIgnoreCase(radio5.getWebElement().getAttribute("checked")));		
		assertFalse("true".equalsIgnoreCase(radio6.getWebElement().getAttribute("checked")));		
		
		// 3) Checking an already checked radio should not result in any change
		// ----------------------------------
		// a) Check radio4 button again
		ev = new ElementValue(driver, lv.getValue());
		ev.applyTo(radio4, false);		
		// c) Re-assert only radio4 is checked.
		assertTrue("true".equalsIgnoreCase(radio4.getWebElement().getAttribute("checked")));
		assertFalse("true".equalsIgnoreCase(radio5.getWebElement().getAttribute("checked")));		
		assertFalse("true".equalsIgnoreCase(radio6.getWebElement().getAttribute("checked")));		
	}

	@Test
	public void test2ClickCheckbox() {
		
		// 1) Unchecking an already unchecked checkbox should result in no change.
		Element chk1 = findAndAssertElement("checkbox-page1", 1, ElementType.CHECKBOX);
		assertFalse("true".equalsIgnoreCase(chk1.getWebElement().getAttribute("checked")));		
		configureLabelAndValue(1, "false", ElementType.CHECKBOX);
		ev = new ElementValue(driver, lv.getValue());
		ev.applyTo(chk1, false);		
		assertFalse("true".equalsIgnoreCase(chk1.getWebElement().getAttribute("checked")));	
		
		// 2) Checking the checkbox should result in a change.
		configureLabelAndValue(1, "true", ElementType.CHECKBOX);
		ev = new ElementValue(driver, lv.getValue());
		ev.applyTo(chk1, false);		
		assertTrue("true".equalsIgnoreCase(chk1.getWebElement().getAttribute("checked")));	
	}

	@Test
	public void test3ClickSelectElement() {
		
		// 1) Test a select element that does not have a top blank value at the top
		// ------------------------------------------------------------------------
		// a) Assert the first option is selected in select1 if no action has been taken
		Element select1 = findAndAssertElement("select-element-page1", 1, ElementType.SELECT);
		assertEquals("val1", select1.getValue());		
		// b) Select the second option by text and assert the value of the select element accordingly.
		configureLabelAndValue(1, "my second value", ElementType.SELECT);
		ev = new ElementValue(driver, lv.getValue());
		ev.applyTo(select1, false);		
		assertEquals("val2", select1.getValue());		
		// c) Select the third option by text and assert the value of the select element accordingly.
		configureLabelAndValue(1, "my third value", ElementType.SELECT);
		ev = new ElementValue(driver, lv.getValue());
		ev.applyTo(select1, false);		
		assertEquals("val3", select1.getValue());		
		// d) Select the second option by value and assert the value of the select element accordingly.
		configureLabelAndValue(1, "val2", ElementType.SELECT);
		ev = new ElementValue(driver, lv.getValue());
		ev.applyTo(select1, false);		
		assertEquals("val2", select1.getValue());		
		// e) Select the third option by index and assert the value of the select element accordingly.
		configureLabelAndValue(1, "3", ElementType.SELECT);
		ev = new ElementValue(driver, lv.getValue());
		ev.applyTo(select1, false);		
		assertEquals("val3", select1.getValue());	
		// f) Assert the prior value remains after attempt to select non-existing option
		configureLabelAndValue(1, "bogus value", ElementType.SELECT);
		ev = new ElementValue(driver, lv.getValue());
		ev.applyTo(select1, false);		
		assertEquals("val3", select1.getValue());	
	}
	
}
