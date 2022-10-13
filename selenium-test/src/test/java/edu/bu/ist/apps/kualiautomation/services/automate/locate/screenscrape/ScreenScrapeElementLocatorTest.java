package edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape;

import java.util.List;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;
import edu.bu.ist.apps.kualiautomation.ElementsAssertion;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape.ScreenScrapeComparePattern;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape.ScreenScrapeElementLocator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScreenScrapeElementLocatorTest extends AbstractJettyBasedTest {

	static {
		javascriptIgnoreExceptions = true;
//		javascriptEnabled = false;
	}

	private ScreenScrapeElementLocator locator;
	
	@Override
	public void setupBefore() { 
		locator = new ScreenScrapeElementLocator(driver, null);
	}

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("screen-scrape-1", "ScreenScrapeTestPage1.htm");
		handlers.put("subaward-saved-1", "SubawardSaved.htm");
		handlers.put("SubawardSaved_files", "SubawardSaved_files");
	}

	/**
	 * Test for screen scraping a simple page with basic divs and a table.
	 */
	@Test
	public void assert01SimpleLabel() {
		
		String url = "http://localhost:8080/screen-scrape-1";
		
		@SuppressWarnings("unused")
		List<Element> elements = (new ElementsAssertion(locator)
				.setUrl(url)
				.setElementType(ElementType.SCREENSCRAPE)
				.setLabel("pears:")
				.addAttributeValue(ScreenScrapeComparePattern.LABELLED_BLOCK.name())
				.setNumResults(1)
				.setTextAssertion("bannanas"))
			.findAndAssertElements();
		
		elements = (new ElementsAssertion(locator)
				.setUrl(url)
				.setElementType(ElementType.SCREENSCRAPE)
				.setLabel("pears")
				.addAttributeValue(ScreenScrapeComparePattern.LABELLED_BLOCK.name())
				.setNumResults(0))
			.findAndAssertElements();
		
		elements = (new ElementsAssertion(locator)
				.setUrl(url)
				.setElementType(ElementType.SCREENSCRAPE)
				.setLabel("apples")
				.addAttributeValue(ScreenScrapeComparePattern.LABELLED_BLOCK.name())
				.setNumResults(1)
				.setTextAssertion("oranges"))
			.findAndAssertElements();
		
		// The label and value are separated by at least one blank line, which is a disqualifying condition.
		elements = (new ElementsAssertion(locator)
				.setUrl(url)
				.setElementType(ElementType.SCREENSCRAPE)
				.setLabel("mangos")
				.addAttributeValue(ScreenScrapeComparePattern.LABELLED_BLOCK.name())
				.setNumResults(0))
			.findAndAssertElements();
		
		elements = (new ElementsAssertion(locator)
				.setUrl(url)
				.setElementType(ElementType.SCREENSCRAPE)
				.setLabel("mylabel1")
				.addAttributeValue(ScreenScrapeComparePattern.LABELLED_BLOCK.name())
				.setNumResults(1)
				.setTextAssertion("myvalue1"))
			.findAndAssertElements();
		
		elements = (new ElementsAssertion(locator)
				.setUrl(url)
				.setElementType(ElementType.SCREENSCRAPE)
				.setLabel("mylabel2b")
				.addAttributeValue(ScreenScrapeComparePattern.LABELLED_BLOCK.name())
				.setNumResults(1)
				.setTextAssertion("myvalue2"))
			.findAndAssertElements();
		
		elements = (new ElementsAssertion(locator)
				.setUrl(url)
				.setElementType(ElementType.SCREENSCRAPE)
				.setLabel("mylabel3a")
				.addAttributeValue(ScreenScrapeComparePattern.LABELLED_BLOCK.name())
				.setNumResults(1)
				.setTextAssertion("myvalue3a"))
			.findAndAssertElements();
		
		elements = (new ElementsAssertion(locator)
				.setUrl(url)
				.setElementType(ElementType.SCREENSCRAPE)
				.setLabel("mylabel3b")
				.addAttributeValue(ScreenScrapeComparePattern.LABELLED_BLOCK.name())
				.setNumResults(1)
				.setTextAssertion("myvalue3b"))
			.findAndAssertElements();

	}
	
	/**
	 * Test for screen scraping a real-case html page from the kuali-coeus application.
	 */
	@Test
	public void assert02ComplexHtml() {
		
		String url = "http://localhost:8080/subaward-saved-1";

		@SuppressWarnings("unused")
		List<Element> elements = (new ElementsAssertion(locator)
				.setUrl(url)
				.setElementType(ElementType.SCREENSCRAPE)
				.setLabel("Subaward ID")
				.addAttributeValue(ScreenScrapeComparePattern.LABELLED_NUMBER.name())
				.setNumResults(1)
				.setTextAssertion("89")
				.setValueAssertion("89"))
			.findAndAssertElements();
		
//		elements = (new ElementsAssertion(locator)
//				.setUrl(url)
//				.setElementType(ElementType.SCREENSCRAPE)
//				.setLabel("Document ID:Status:")
//				.addAttributeValue(ScreenScrapeComparePattern.LABELLED_NUMBER.name())
//				.setNumResults(1)
//				.setTextAssertion("10828"))
//			.findAndAssertElements();		
//		
//		elements = (new ElementsAssertion(locator)
//					.setUrl(url)
//					.setElementType(ElementType.SCREENSCRAPE)
//					.setLabel("Requisitioner Unit:")
//					.addAttributeValue(ScreenScrapeComparePattern.LABELLED_WORD.name())
//					.setNumResults(1)
//					.setTextAssertion("University"))
//				.findAndAssertElements();
//		
//		elements = (new ElementsAssertion(locator)
//					.setUrl(url)
//					.setElementType(ElementType.SCREENSCRAPE)
//					.setLabel("Document")
//					.addAttributeValue(ScreenScrapeComparePattern.LABELLED_WORD.name())
//					.setNumResults(14)
//					.setAnyTextAssertions(new String[]{"Overview", "Number", "was", "ID"}))
//				.findAndAssertElements();

	}
}
