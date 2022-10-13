package edu.bu.ist.apps.kualiautomation.services.automate.locate.label;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.openqa.selenium.By;
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
import edu.bu.ist.apps.kualiautomation.util.Utils;

public class LabelElementLocator extends AbstractElementLocator {
	
	// Matches for an element with a text value that contains the provided value, both values normalized for whitespace.
//	private static final String XPATH_CONTAINS = 
//			"//*[text()[contains(normalize-space(translate(., "
//			+ "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
//			+ "'abcdefghijklmnopqrstuvwxyz')), \"[INSERT-LABEL]\")]]";
	
	/**
	 * Matches any element hierarchy of depth less than 2 (contains zero to one child, but no grandchildren).
	 * From there, the hierarchy is flattened with respect to its text (string() function) and matches if the
	 * result is normalized for whitespace and still is equal (ignoring case) to the specified value.
	 * 
	 * This allows us to find web elements that display a certain text value to the user where some of the
	 * words in that text may be bolded with <b> tags, or formatted with <font> or <span> tags. You could allow for
	 * a deeper hierarchy, but that would just return even more flattened ancestor results that would just have to be 
	 * filtered of later. However, this may be necessary if we find that label searches succeed this way to infrequently.
	 * 
	 * NOTE: Using of unicode matching to identify non-printing backspaces (&nbsp;), which are not caught by the
	 * normalize-space function.
	 */
	public static final String XPATH_CONTAINS = 
			"//*[(not(./*) or not(./*/*))]"
			+ "[contains(normalize-space(translate("
			+ "translate(string(.), '\u00a0', ' '), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"
			+ "), \"[INSERT-LABEL]\")]";
	
	private boolean labelCanBeHyperlink = true;
	
	private static final String[] DISALLOWED_TAGNAMES = new String[] {
		"option"
	};
	
	private LabelElementLocator() {
		super(null, null); // Restrict the default constructor
	}
	public LabelElementLocator(WebDriver driver, Locator parent){
		super(driver, parent);
	}
	public LabelElementLocator(WebDriver driver, SearchContext searchContext, Locator parent){
		super(driver, searchContext, parent);
	}
	
	public Element locate(String label) {
		return super.locateFirst(null, Arrays.asList(new String[]{label}));
	}
	
	@Override
	protected List<WebElement> customLocate() {
		
		List<WebElement> located = new ArrayList<WebElement>();
		String label = new String(parameters.get(0)).trim().toLowerCase();
		String cleanedLabel = ComparableLabel.getCleanedValue(label).toLowerCase();
		String scope = "";	// global
		if(searchContext instanceof WebElement) {
			scope = ".";	// current scope within element.
		}
		
		// 1) Some buttons are styled (CSS) to look like labels, so don't exclude these from label candidates.
		List<WebElement> elements = ElementType.BUTTON.findAll(searchContext, super.skipFrameSearch);
		
		// 2) Search for any web element whose immediate inner text contains the label.
		String xpath = scope + XPATH_CONTAINS.replace("[INSERT-LABEL]", cleanedLabel);
		List<WebElement> cached = XpathElementCache.get(driver, xpath, super.skipFrameSearch);
		if(cached.isEmpty()) {
			elements.addAll(AbstractWebElement.wrap(searchContext.findElements(By.xpath(xpath))));
			XpathElementCache.put(driver, xpath, super.skipFrameSearch, elements);
		}
		else {
			elements.addAll(cached);
		}
		applyFiltering(elements, label);

		// 3) Wrap the web elements in ComparableLabel instances for sorting so higher ranked results are on top.
		List<ComparableLabel> labels = new ArrayList<ComparableLabel>();
		for(WebElement elmt : elements) {
			if(!isDisallowedHyperlink(elmt)) {
				String text = null;
				if(isButton(elmt)) {
					text = elmt.getAttribute("value");
					if(Utils.isEmpty(text) && "button".equalsIgnoreCase(elmt.getTagName())) {
						text = elmt.getText();
					}
				}
				else {
					text = getText(driver, elmt);
				}
				labels.add(new BasicComparableLabel(elmt, label, text));
			}
		}
		
		// 4) Unwrap the highest ranked result(s) back into a WebElement collection.
		located.addAll(ComparableLabel.getHighestRanked(labels));
		
		// 5) In case of a draw, favor labels that are direct child nodes of others.
		if(located.size() > 1) {
			located.clear();
			labels = HierarchyBasedComparableLabel.getInstances(labels);
			located.addAll(ComparableLabel.getHighestRanked(labels));
			
			if(located.size() > 1) {
				List<ComparableLabel> retries = new ArrayList<ComparableLabel>();
				for(WebElement lbl : located) {
					retries.add(new BasicComparableLabel(lbl, label, lbl.getText()));
				}
				located.clear();
				located.addAll(ComparableLabel.getHighestRanked(retries));
			}
		}
		
		// We are only searching for labels, but if the search fails then the upcoming default search
		// will try to find fields. Prevent this by indicating the default search has already run.
		//defaultRan = true;
		defaultRanFor.add(currentFrameSrc);
		
		return located;
	}
	
	/**
	 * Remove from the list of located label web elements those that have a tagname that is not 
	 * qualified or are buttons whose value does not match the value the label search was based on.
	 * 
	 * @param elements
	 * @param label
	 */
	private void applyFiltering(List<WebElement> elements, String label) {
		for (Iterator<WebElement> iterator = elements.iterator(); iterator.hasNext();) {
			WebElement we = (WebElement) iterator.next();
			if(ignoreHidden && !we.isDisplayed()) {
				iterator.remove();
			}
			else if(ignoreDisabled && !we.isEnabled()) {
				iterator.remove();
			}
			else {
				for(String tagname : DISALLOWED_TAGNAMES) {
					if(tagname.equalsIgnoreCase(we.getTagName())) {
						iterator.remove();
					}
				}
				if(isButton(we)) {
					String val = we.getAttribute("value");
					if(Utils.isEmpty(val) && "button".equalsIgnoreCase(we.getTagName())) {
						val = we.getText();
					}
					if(Utils.isEmpty(val)) {
						iterator.remove();
					}
					else {
						val = val.trim();
						if(!val.toLowerCase().contains(label.toLowerCase())) {
							iterator.remove();
						}
					}
				}
			}
		}		
	}
	
	private boolean isButton(WebElement we) {
		return ElementType.getInstance(we).is(ElementType.BUTTON.name());
	}
	
	@Override
	protected Element getElement(WebDriver driver, WebElement we) {
		return new BasicElement(driver, we);
	}
	
	public boolean isDisallowedHyperlink(WebElement we) {
		return "a".equalsIgnoreCase(we.getTagName()) && labelCanBeHyperlink == false;
	}
	
	public boolean isLabelCanBeHyperlink() {
		return labelCanBeHyperlink;
	}
	
	public void setLabelCanBeHyperlink(boolean labelCanBeHyperlink) {
		this.labelCanBeHyperlink = labelCanBeHyperlink;
	}

}
