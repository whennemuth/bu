package edu.bu.ist.apps.kualiautomation.services.automate.locate.label;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.label.LabelElementLocator;

public class LabelElementLocatorTest extends AbstractJettyBasedTest {

	static {
		javascriptEnabled = false;
	}
	
	private LabelElementLocator locator;

	@Override
	public void setupBefore() {
		locator = new LabelElementLocator(driver, null);
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
		handlers.put("colon4", "<html><body><span> abc :label </span></body></html>");
		handlers.put("mixture", "<html><body><label> <font color=\"red\">*&nbsp;</font>  Description:  </label></body></html>");
		handlers.put("mixture2", "<html><body>"
				+ "<label> "
				+ "<font color=\"red\">*&nbsp;</font>  Description:  "
				+ "<font color=\"red\">*&nbsp;</font>  Description2:  "
				+ "</label></body></html>");
		
		handlers.put("prop-log-lookup-frame", "ProposalLogLookup_files/ProposalLogLookupFrame.htm");
		handlers.put("prop-log-lookup", "ProposalLogLookup.htm");
		handlers.put("ProposalLogLookup_files", "ProposalLogLookup_files");
		
		handlers.put("prop-log-add", "ProposalLogAdd.htm");
		handlers.put("ProposalLogAdd_files", "ProposalLogAdd_files");
	}
	
	@Test
	public void testFindByInnerText() {
		locator.getWebDriver().get("http://localhost:8080/hello1");
		Element element = locator.locate("hello");
		assertNotNull(element);
		assertEquals("div", element.getWebElement().getTagName().toLowerCase());
		
		locator.getWebDriver().get("http://localhost:8080/hello2");
		element = locator.locate("hello");
		assertNotNull(element);
		assertEquals("div", element.getWebElement().getTagName().toLowerCase());
		
		locator.getWebDriver().get("http://localhost:8080/hello3");
		element = locator.locate("hello");
		assertNotNull(element);
		assertEquals("div", element.getWebElement().getTagName().toLowerCase());
	}
	
	@Test
	public void testAvoidSimilarInnerText() {
		locator.getWebDriver().get("http://localhost:8080/similar1");
		Element element = locator.locate("similar");
		assertNotNull(element);
		assertEquals("similarity", element.getWebElement().getText());
		
		locator.getWebDriver().get("http://localhost:8080/similar2");
		element = locator.locate("similar");
		assertNull(element);
	}
	
	/**
	 * Accounting for whitespace and colons, the ENTIRE element innerText must match the search string, not just part of it.
	 */
	@Test
	public void testFindByInnerTextWithQuote() {
		locator.getWebDriver().get("http://localhost:8080/quote");
		Element element = locator.locate("text with single 'quote'");
		assertNotNull(element);
		assertEquals("div", element.getWebElement().getTagName().toLowerCase());
	}

	@Test
	public void testFindByInnerTextWithColon() {
		Element element = null;
		
		locator.getWebDriver().get("http://localhost:8080/colon1");
		element = locator.locate("label");
		assertNotNull(element);
		assertEquals("span", element.getWebElement().getTagName().toLowerCase());
		
		locator.getWebDriver().get("http://localhost:8080/colon2");
		element = locator.locate("label");
		assertNotNull(element);
		assertEquals("span", element.getWebElement().getTagName().toLowerCase());
		
		locator.getWebDriver().get("http://localhost:8080/colon3");
		element = locator.locate("label");
		assertNotNull(element);
		assertEquals("span", element.getWebElement().getTagName().toLowerCase());
		
		locator.getWebDriver().get("http://localhost:8080/colon4");
		element = locator.locate("label");
		assertNull(element);
	}
	
	@Test
	public void testFindInnerTextWithHtmlBlock() {
		locator.getWebDriver().get("http://localhost:8080/mixture");
		Element element1 = locator.locate("description");
		assertNotNull(element1);
		assertEquals("label", element1.getWebElement().getTagName().toLowerCase());
		assertEquals("*  Description:", element1.getWebElement().getText());	
		
		locator.getWebDriver().get("http://localhost:8080/mixture2");
		Element element2 = locator.locate("description2");
		assertNull(element2);
	}
	
	@Test 
	public void findProposalLogLabel() {
		locator.getWebDriver().get("http://localhost:8080/prop-log-lookup");
		Element element = locator.locate("Proposal Number");
		assertNotNull(element);
		assertEquals("label", element.getWebElement().getTagName().toLowerCase());
		assertEquals("Proposal Number:", element.getWebElement().getText());
		
		locator.getWebDriver().get("http://localhost:8080/prop-log-add");
		Element element2 = locator.locate("description");
		assertNotNull(element2);
		assertEquals("label", element2.getWebElement().getTagName().toLowerCase());
		assertEquals("*  Description:", element2.getWebElement().getText());		
	}
	
	@Test 
	public void findProposalLogLabelInFrame() {
		locator.getWebDriver().get("http://localhost:8080/prop-log-lookup-frame");
		Element element = locator.locate("Proposal Number");
		assertNotNull(element);
		assertEquals("label", element.getWebElement().getTagName().toLowerCase());
		assertEquals("Proposal Number:", element.getWebElement().getText());
	}
}
