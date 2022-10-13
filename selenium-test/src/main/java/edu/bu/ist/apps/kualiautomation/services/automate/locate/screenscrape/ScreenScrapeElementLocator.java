package edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.element.AbstractWebElement;
import edu.bu.ist.apps.kualiautomation.services.automate.element.BasicElement;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;
import edu.bu.ist.apps.kualiautomation.services.automate.element.XpathElementCache;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.AbstractElementLocator;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.Locator;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.label.ComparableLabel;
import edu.bu.ist.apps.kualiautomation.util.Utils;

/**
 * This locator finds an html element(s) whose innerText best matches a pattern, where that pattern specifies a
 * static sequence of characters followed by a sequence of characters that match a selected pattern.
 * This would usually be a value, like and id, labelled on its left side. By "innerText" is meant what the 
 * user would see in the browser within the bounds of the html element in terms of text. 
 * For example, the innerText of:
 *     <div> employeeId: 35334545</div>
 * would be...
 *     "employeeId: 35334545"
 * and the innertText of:
 *     <div><span>organizationID:</span><div style="padding:5px;">45649389</div></div>
 * would be...
 *     "organizationID:45649389"
 * 
 * Unlike other locators, we are not searching for an element for user input, but are instead "screen scraping"
 * for a text value. The inner-most element that best matches is returned, but the textual match is captured so it
 * is available for use further down along the suite, like inserting it into a text field and clicking "search".
 * 
 * @author wrh
 *
 */
public class ScreenScrapeElementLocator extends AbstractElementLocator {

	private static final String XPATH_CONTAINS_IGNORECASE = 
			"//*[contains(string(normalize-space(translate(., "
			+ "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
			+ "'abcdefghijklmnopqrstuvwxyz'))), \"[INSERT-LABEL]\")]";

	private static final String XPATH_CONTAINS = 
			"//*[contains(string(normalize-space()), \"[INSERT-LABEL]\")]";
	
	private boolean ignorecase = true;
	
	public ScreenScrapeElementLocator(WebDriver driver, Locator parent) {
		super(driver, parent);
	}
	public ScreenScrapeElementLocator(WebDriver driver, boolean ignorecase, Locator parent) {
		super(driver, parent);
		this.ignorecase = ignorecase;
	}
	public ScreenScrapeElementLocator(WebDriver driver, SearchContext searchContext, boolean ignorecase, Locator parent){
		super(driver, searchContext, parent);
		this.ignorecase = ignorecase;
	}

	@Override
	protected List<WebElement> customLocate() {
		List<WebElement> located = new ArrayList<WebElement>();
		if(validParameters()) {
			
			located.addAll(method1());
			
			if(located.isEmpty()) {
				located.addAll(method2());
			}
		}
		
		return located;
	}
	
	private List<WebElement> method1() {
		List<WebElement> located = new ArrayList<WebElement>();
		if(validParameters()) {
			String tagname = "body";
			JavascriptExecutor executor = (JavascriptExecutor) searchContext;
			String body = (String) executor.executeScript(""
					+ "try { "
					+ "   var elmts = document.getElementsByTagName(arguments[0]); "
					+ "   if(elmts == null || elmts.length == 0) { "
					+ "      return null; "
					+ "   } "
					+ "   else { "
					+ "      var text = elmts[0].textContent || elmts[0].innerText;"
					+ "      return text; "
					+ "   } "
					+ "}"
					+ "catch(e) { "
					+ "   return 'ERROR: ' + e; "
					+ "}", 
					tagname);
			
			if(Utils.isEmpty(body)) {
				System.out.println("Cannot find " + tagname + " element!");
			}
			else if(body.startsWith("ERROR: ")) {
				System.out.println("JAVASCRIPT " + body);
			}
			else {
				String label = new String(parameters.get(0)).trim().replaceAll("\\s+", " ");
				String sPattern = parameters.get(1);
				ScreenScrapeComparePattern pattern = ScreenScrapeComparePattern.valueOf(sPattern);
				body = body.replaceAll("\\r", "").replaceAll("\\n", "");
				List<String> matches = pattern.getMatches(body, label, ignorecase);
				for(String text : matches) {
					
					ComparableParameters parms = new ComparableParameters()
							.setPattern(ScreenScrapeComparePattern.valueOf(sPattern))
							.setLabel(label)
							.setText(text)
							.setWebElement(null)
							.setIgnorecase(false) // caseless match will be invoked only if case match fails
							.setUseDefaultMethodIfIndeterminate(false);
							
					ComparableScreenScrape screenscrape = new ComparableScreenScrape(parms);
					screenscrape.scrape();
					
					if(screenscrape.hasWebElement()) {
						located.add(screenscrape.getWebElement());
					}
				}
			}
		}
		
		return located;		
	}
	
	private List<WebElement> method2() {
		String scope = "";	// global
		if(searchContext instanceof WebElement) {
			scope = ".";	// current scope within element.
		}
		
		String label = new String(parameters.get(0)).trim().replaceAll("\\s+", " ");
		String sPattern = parameters.get(1);
		String xpath = new String(scope);
		if(ignorecase)
			xpath = xpath + XPATH_CONTAINS_IGNORECASE.replace("[INSERT-LABEL]", label.toLowerCase());
		else
			xpath = xpath + XPATH_CONTAINS.replace("[INSERT-LABEL]", label);
		
		List<WebElement> elements = XpathElementCache.get(driver, xpath, super.skipFrameSearch);
		if(elements.isEmpty()) {
			elements.addAll(searchContext.findElements(By.xpath(xpath)));
			XpathElementCache.put(driver, xpath, super.skipFrameSearch, elements);
		}

		List<ComparableLabel> scraped = new ArrayList<ComparableLabel>();

		if(!elements.isEmpty()) {
			for (Iterator<WebElement> iterator = elements.iterator(); iterator.hasNext();) {
				
				WebElement we = iterator.next();
				
				ComparableParameters parms = new ComparableParameters()
						.setPattern(ScreenScrapeComparePattern.valueOf(sPattern))
						.setLabel(label)
						.setText(we.getText())
						.setWebElement(we)
						.setIgnorecase(false) // caseless match will be invoke only if case match fails
						.setUseDefaultMethodIfIndeterminate(false);
						
				scraped.add(new ComparableScreenScrape(parms));
			}
		}
		
		return ComparableLabel.getHighestRanked(scraped);		
	}
	
	private boolean validParameters() {
		if(Utils.isEmpty(elementType)) {
			message = "Invalid parameter: elementType cannot be null or empty";
			return false;
		}
		if(parameters.size() < 2) {
			message = "Invalid parameter: Expecting parameters list to have at least 2 entries";
			return false;
		}
		if(Utils.isEmpty(parameters.get(0))) {
			message = "Invalid parameter: First parameter (label) cannot be null or empty";
			return false;
		}
		if(Utils.isEmpty(parameters.get(1))) {
			message = "Invalid parameter: Second parameter (pattern) cannot be null or empty";
			return false;
		}

		return true;
	}
	@Override
	protected Element getElement(WebDriver driver, WebElement we) {
		Element e = new BasicElement(driver, we);
		e.setElementType(ElementType.SCREENSCRAPE);
		return e;
	}
}
