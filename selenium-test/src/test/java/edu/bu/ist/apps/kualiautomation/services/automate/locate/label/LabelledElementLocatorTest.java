package edu.bu.ist.apps.kualiautomation.services.automate.locate.label;

import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;
import edu.bu.ist.apps.kualiautomation.ElementsAssertion;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.label.LabelledElementLocator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LabelledElementLocatorTest extends AbstractJettyBasedTest {
	
	private LabelledElementLocator locator;

	static {
		// This test operates on iframe content directly without outer page and its scripts present.
		// Normally this would cause a javascript exception due to the missing scripts, so suppress. 
		javascriptIgnoreExceptions = true;		
	}
	
	@Override
	public void setupBefore() {
		locator = new LabelledElementLocator(driver, null);
	}

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("hello1", "<html><body><div>hello<div> hello </div></div><div><input type='text'></div></body></html>");
		handlers.put("hello2", "<html><body><div> hello <div>hello</div></div><div><input type='text'></div></body></html>");
		handlers.put("hello3", "<html><body><div> hello <div></div></div><div><input type='text'></div></body></html>");
		handlers.put("similar1", "<html><body><div>matched similar<div> similarity </div></div><div><input type='text'></div></body></html>");
		handlers.put("similar2", "<html><body><div> simila </div><div><input type='text'></div></body></html>");
		handlers.put("quote", "<html><body><div> text with  single 'quote' </div></body></html>");
		handlers.put("colon1", "<html><body><span> label: </span></body></html>");
		handlers.put("colon2", "<html><body><span> :label </span></body></html>");
		handlers.put("colon3", "<html><body><span> label : : </span></body></html>");
		handlers.put("prop-log-lookup-frame", "ProposalLogLookup_files/ProposalLogLookupFrame.htm");
		handlers.put("prop-log-lookup", "ProposalLogLookup.htm");
		handlers.put("ProposalLogLookup_files", "ProposalLogLookup_files");
		handlers.put("subaward-entry-1", "SubawardEntry.htm");
		handlers.put("SubawardEntry_files", "SubawardEntry_files");
	}
	
	/**
	 * Find all fields neighboring a label in a frameless html page
	 */
	@Test 
	public void findFields() {
		findFields("http://localhost:8080/prop-log-lookup");		
	}
	
	/**
	 * Find all fields in a frame in an html page based on their labels
	 */
	@Test 
	public void test01FindFieldsInFrame() {
		findFields("http://localhost:8080/prop-log-lookup-frame");
	}
	
	@Test
	public void test02FindButtonImageByNearestLabel() {
		findButtonImageByNearestLabel("http://localhost:8080/prop-log-lookup");
	}
	
	@Test
	public void test03FindButtonImageByNearestLabel() {
		findButtonImageByNearestLabel("http://localhost:8080/prop-log-lookup-frame");
	}
	private void findButtonImageByNearestLabel(String url) {
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Proposal Log Status")
		.setElementType(ElementType.BUTTONIMAGE)
		.setNumResults(2)
		.findAndAssertElements();

		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Proposal Log Status")
		.addAttributeValue("Search Proposal Log Status")
		.setElementType(ElementType.BUTTONIMAGE)
		.setNumResults(1)
		.addAttributeAssertion("title", "Search Proposal Log Status")
		.findAndAssertElements();
	}
	
	@Test
	public void test04FindButtonImageByTitle() {
		findButtonImageByTitle("http://localhost:8080/prop-log-lookup");
	}
	
	@Test
	public void test05FindButtonImageByTitle() {
		findButtonImageByTitle("http://localhost:8080/prop-log-lookup-frame");
	}
	private void findButtonImageByTitle(String url) {

		new ElementsAssertion(locator)
		.setUrl(url)
		.addAttributeValue("Search Proposal Log Status")
		.setElementType(ElementType.BUTTONIMAGE)
		.setNumResults(1)
		.addAttributeAssertion("title", "Search Proposal Log Status")
		.findAndAssertElements();
	}
	
	@Test
	public void test04SubawardEntry() {
		new ElementsAssertion(locator)
		.setUrl("http://localhost:8080/subaward-entry-1")
		.setElementType(ElementType.SELECT)
		.setLabel("Requisitioner Unit:")
		.setTagNameAssertion("select")
		.setNumResults(1)
		.findAndAssertElements();	
	}
	
	@Test
	public void test05SubawardEntry() {
		new ElementsAssertion(locator)
		.setUrl("http://localhost:8080/subaward-entry-1")
		.setElementType(ElementType.TEXTBOX)
		.setLabel("Other:")
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "text")
		.addAttributeAssertion("title", "Other")
		.findAndAssertElements();		
	}
	
	@Test
	public void test06SubawardEntry() {
		new ElementsAssertion(locator)
		.setUrl("http://localhost:8080/subaward-entry-1")
		.setElementType(ElementType.TEXTBOX)
		.setLabel("Subrecipient:")
		.setNumResults(1)
		.setTagNameAssertion("input")
		.addAttributeAssertion("type", "text")
		.addAttributeAssertion("title", "* Subrecipient")
		.findAndAssertElements();		
	}

	private void findFields(String url) {
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Proposal Number")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "proposalNumber")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Proposal Log Type")
		.setElementType(ElementType.SELECT)
		.setNumResults(1)
		.addAttributeAssertion("id", "proposalLogTypeCode")
		.addAttributeAssertion("name", "proposalLogTypeCode")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Proposal Log Status")
		.setElementType(ElementType.SELECT)
		.setNumResults(1)
		.addAttributeAssertion("id", "logstatus")
		.addAttributeAssertion("name", "logstatus")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Proposal Merged With")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "mergedwith")
		.addAttributeAssertion("name", "mergedwith")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Created Institutional Proposal")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "instProposalNumber")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Proposal Type")
		.setElementType(ElementType.SELECT)
		.setNumResults(1)
		.addAttributeAssertion("id", "proposalTypeCode")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Title")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "title")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Principal Investigator (Employee)")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "person.username")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Principal Investigator (Non-Employee)")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "rolodexId")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Lead Unit")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "leadunit")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Sponsor")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "sponsorcode")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Sponsor Name")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "sponsorname")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Comments")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "comments")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Deadline Date From")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "rangeLowerBoundKeyPrefix_deadlineDate")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Deadline Date To")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "deadlinedate")
		.findAndAssertElements();
		
		new ElementsAssertion(locator)
		.setUrl(url)
		.setLabel("Principal Investigator (Employee)")
		.setElementType(ElementType.TEXTBOX)
		.setNumResults(1)
		.addAttributeAssertion("id", "person.userName")
		.findAndAssertElements();
		
		
	}
}
