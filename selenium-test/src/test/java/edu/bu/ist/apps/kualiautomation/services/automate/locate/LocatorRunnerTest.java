package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;
import edu.bu.ist.apps.kualiautomation.ElementsAssertion;
import edu.bu.ist.apps.kualiautomation.entity.LabelAndValue;
import edu.bu.ist.apps.kualiautomation.services.automate.RunLog;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocatorRunnerTest extends AbstractJettyBasedTest {

	private RunLog runlog;
	private LocatorRunner runner;
	
	@Override
	public void setupBefore() { 
		runlog = new RunLog(true);
		runner = new LocatorRunner(driver, runlog);
		AbstractElementLocator.printDuration = true;	
	}

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		
		handlers.put("address-book-lookup", "AddressBookLookup1.htm");
		handlers.put("AddressBookLookup1_files", "AddressBookLookup1_files");
		
		handlers.put("subaward-entry-1", "SubawardEntry.htm");
		handlers.put("SubawardEntry_files", "SubawardEntry_files");
		
		handlers.put("subaward-lookup-1", "SubawardLookup.htm");
		handlers.put("SubawardLookup_files", "SubawardLookup_files");
		
		handlers.put("subaward-actions-1", "SubawardActions.htm");
		handlers.put("SubawardActions_files", "SubawardActions_files");
		
		handlers.put("subaward-financial-1", "SubawardFinancial.htm");
		handlers.put("SubawardFinancial_files", "SubawardFinancial_files");
	}
	
	@Test
	public void assert01FindSearchButtonImage() {

		LabelAndValue lv = new LabelAndValue();
		lv.setLabel("search");
		lv.setElementType(ElementType.BUTTON.name());
		
		// 1) Start a search for buttons which fails-over to a search for a hotspot
		ElementsAssertion asserter = new ElementsAssertion(runner, true);
		asserter
			.setUrl("http://localhost:8080/address-book-lookup")
			.addLabelAndValue(lv)
			.setNumResults(1)
			.addAttributeAssertion("title", "search")
			.findAndAssertElements();
		
		// 2) Search for the hotspot directly (should be faster).
		lv.setElementType(ElementType.HOTSPOT.name());
		asserter.findAndAssertElements();
	}
	
	/**
	 * Search for links with innerText of "Return Value", labelled by a table column "Return Value".
	 * DO NOT specify an extra attribute.
	 */
	@Test
	public void assert02FindFirstOfIdenticalLinks() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setLabel("Return Value");
		lv.setElementType(ElementType.HYPERLINK.name());
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/address-book-lookup")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("a")
		.setTextAssertion("return value")
		.findAndAssertElements();
	}
	
	/**
	 * Search for links with innerText of "Return Value", labelled by a table column "Return Value".
	 * Add an extra attribute to the search criteria of "return value".
	 */
	@Test
	public void assert03FindFirstOfIdenticalLinks() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setLabel("Return Value");
		lv.setIdentifier("return value");
		lv.setElementType(ElementType.HYPERLINK.name());
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/address-book-lookup")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("a")
		.setTextAssertion("return value")
		.findAndAssertElements();
	}
	
	/**
	 * Search for links with innerText of "open", labelled by a table column "Actions".
	 * DO NOT specify an extra attribute.
	 */
	@Test
	public void assert04FindFirstOfIdenticalLinks() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setLabel("Actions");
		lv.setIdentifier("open");
		lv.setElementType(ElementType.HYPERLINK.name());
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-lookup-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("a")
		.setTextAssertion("open")
		.findAndAssertElements();
	}
	
	@Test
	public void assert05FindDescriptionField() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setLabel("Description");
		lv.setElementType(ElementType.TEXTBOX.name());
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-entry-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("title", "* Document Description")
		.findAndAssertElements();		
	}
	
	@Test
	public void assert06FindAddButton() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setLabel("Actions");
		lv.setElementType(ElementType.BUTTON.name());
		lv.setIdentifier("methodToCall.addContacts.anchorContacts");
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-entry-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "image")
		.findAndAssertElements();		
	}
	
	@Test
	public void assert07FindSubmitButton() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setLabel(null);
		lv.setElementType(ElementType.BUTTON.name());
		lv.setIdentifier("methodToCall.save");
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-entry-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "image")
		.findAndAssertElements();		
	}
	
	@Test
	public void assert08FindSubawardIdTextbox() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setLabel("Subaward ID");
		lv.setElementType(ElementType.TEXTBOX.name());
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-lookup-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "text")
		.findAndAssertElements();		
	}
	
	@Test
	public void assert09FindSearchButton() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.BUTTON.name());
		lv.setIdentifier("methodToCall.search");
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-lookup-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "image")
		.findAndAssertElements();		
	}
	
	@Test
	public void assert10FindSubmitButton() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.HOTSPOT.name());
		lv.setIdentifier("methodToCall.route");
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-actions-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "image")
		.findAndAssertElements();				
	}
	
	@Test
	public void assert10aFindLookupContactButton() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.HOTSPOT.name());
		lv.setLabel("Non-employee ID");
		lv.setIdentifier("search");
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-entry-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "image")
		.findAndAssertElements();				
	}
	
	@Test
	public void assert11FindSubmitButton() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.BUTTON.name());
		lv.setIdentifier("Subaward Actions");
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-actions-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "submit")
		.findAndAssertElements();				
	}
	
	@Test
	public void assert12FindSubmitButton() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.TEXTBOX.name());
		lv.setLabel("* Effective Date:");
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-financial-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "text")
		.findAndAssertElements();				
	}
	
	@Test
	public void assert13FindBudgetStartDateTextbox() {
		
		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.TEXTBOX.name());
		lv.setLabel("* Budget Start Date:");
		
		new ElementsAssertion(runner, true)
		.setUrl("http://localhost:8080/subaward-financial-1")
		.addLabelAndValue(lv)
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "text")
		.addAttributeAssertion("title", "* Budget Start Date")
		.findAndAssertElements();				
	}
}
