package edu.bu.ist.apps.kualiautomation.services.automate.locate.label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.WebElement;

/**
 * Two labels (Web elements with text content) have been found as the result of a search.
 * One is more valid a search result than the other or both are equally valid. Implementations of this
 * class make that determination. If implementors find all labels to be equally valid in their custom
 * logic, the default logic here is triggered if flagged to do so (useDefaultMethodIfIndeterminate).
 * 
 * @author wrh
 *
 */
public abstract class ComparableLabel implements Comparable<ComparableLabel> {

	protected String label;
	private String text;
	protected WebElement webElement;
	protected boolean demoted;
	protected boolean disqualified;
	protected boolean useDefaultMethodIfIndeterminate;
	// Defaults to true. Implementers must override this value.
	protected boolean ignorecase = true;
	
	protected static final int THIS_LABEL_IS_BETTER = -1;
	protected static final int OTHER_LABEL_IS_BETTER = 1;
	protected static final int ITS_A_DRAW = 0;
	
	/**
	 * Constructor for Comparable Label
	 * 
	 * @param label The value the web element this label is based on was located by.
	 * @param text The actual text of the web element this label is based on.
	 * @param useDefaultMethodIfIndeterminate Invoke the default comparison method if implementors of this class 
	 * find two unequal labels to compare as zero using their custom compare method.
	 */
	public ComparableLabel(String label, String text, boolean useDefaultMethodIfIndeterminate) {
		this.label = label;
		this.text = text;
		this.useDefaultMethodIfIndeterminate = useDefaultMethodIfIndeterminate;
	}
	
	/**
	 * Constructor for Comparable Label
	 * 
	 * @param webElement of the label
	 * @param label The value the web element this label is based on was located by.
	 * @param text The actual text of the web element this label is based on.
	 * @param useDefaultMethodIfIndeterminate Invoke the default comparison method if implementors of this class 
	 * find two unequal labels to compare as zero using their custom compare method.
	 */
	public ComparableLabel(WebElement webElement, String label, String text, boolean useDefaultMethodIfIndeterminate) {
		this(label, text, useDefaultMethodIfIndeterminate);
		this.webElement = webElement;
	}
	
	protected abstract int customCompareTo(ComparableLabel lbl);
	
	protected int requiredCompareTo(ComparableLabel lbl) {
		if (this == lbl)
			return ITS_A_DRAW;		
		if (lbl == null)
			return THIS_LABEL_IS_BETTER;
		if (!(lbl instanceof ComparableLabel))
			return THIS_LABEL_IS_BETTER;
		ComparableLabel other = (ComparableLabel) lbl;
		if (label == null) {
			if (other.label != null)
				return OTHER_LABEL_IS_BETTER;
		} 
		else if (other.label == null) {
			return THIS_LABEL_IS_BETTER;
		}
		
		return ITS_A_DRAW;
	}
	
	protected int defaultCompareTo(ComparableLabel lbl) {
		String thislabel = getLabel();
		String thistext = getText();
		String otherlabel = getCleanedValue(lbl.label);
		String othertext = getCleanedValue(lbl.text);
		if(ignorecase) {
			otherlabel = otherlabel.toLowerCase();
			othertext = othertext.toLowerCase();
		}
		int retval = ITS_A_DRAW;
		
		if(thistext.startsWith(thislabel) && othertext.startsWith(otherlabel)) {			
			if(thistext.length() < othertext.length()) {
				retval = THIS_LABEL_IS_BETTER;
			}
			else if(othertext.length() < thistext.length()) {
				retval = OTHER_LABEL_IS_BETTER;
			}
		}
		else if(thistext.startsWith(thislabel)) {
			retval = THIS_LABEL_IS_BETTER;
		}
		else if(othertext.startsWith(otherlabel)) {
			retval = OTHER_LABEL_IS_BETTER;
		}
		else {			
			if(thistext.contains(thislabel) && othertext.contains(otherlabel)) { 
				String trimmedThistext = trimLeftNonAlphaNumeric(thistext, thislabel);
				String trimmedOthertext = trimLeftNonAlphaNumeric(othertext, otherlabel);
				
				if(thistext.length() == trimmedThistext.length()) {
					// The text for this label contains the sought value, but it is prefixed by at least one alpha-numeric character and is hence not a valid match
					this.disqualified = true;
				}
				if(othertext.length() == trimmedOthertext.length()) {
					// The text for the other label contains the sought value, but it is prefixed by at least one alpha-numeric character and is hence not a valid match
					lbl.disqualified = true;
				}

				ComparableLabel newThis = new ComparableLabel(thislabel, trimmedThistext, true) {
					@Override protected int customCompareTo(ComparableLabel lbl) {
						return ITS_A_DRAW;
					}
				};
				newThis.demoted = this.demoted;
				newThis.disqualified = this.disqualified;
				
				ComparableLabel newOther = new ComparableLabel(otherlabel, trimmedOthertext, true) {
					@Override protected int customCompareTo(ComparableLabel lbl) {
						return ITS_A_DRAW;
					}
				};
				newOther.demoted = lbl.demoted;
				newOther.disqualified = lbl.disqualified;
				
				retval = newThis.compareTo(newOther);
			}
			else if(thistext.contains(thislabel)) {
				retval = THIS_LABEL_IS_BETTER;
			}
			else if(othertext.contains(otherlabel)) {
				retval = OTHER_LABEL_IS_BETTER;
			}
		}
		
		return retval;
	}

	/**
	 * Returns a negative integer, zero, or a positive integer as this object is less than, equal to, 
	 * or greater than the specified object. 
	 */
	@Override
	public int compareTo(ComparableLabel o) {
		
		int compared = requiredCompareTo(o);
		
		if(this.disqualified && o.disqualified)
			return 0;
		else if(o.disqualified)
			return THIS_LABEL_IS_BETTER;
		else if(this.disqualified)
			return OTHER_LABEL_IS_BETTER;
		
		if(compared == ITS_A_DRAW) {
			compared = customCompareTo(o);
			if(compared == ITS_A_DRAW && useDefaultMethodIfIndeterminate) {
				compared = defaultCompareTo(o);
			}
		}
		
		if(compared == THIS_LABEL_IS_BETTER)
			o.demoted = true;
		else if(compared == OTHER_LABEL_IS_BETTER)
			this.demoted = true;
		
		return compared;
	}
	
	public String getRawLabel() {
		return label;
	}
	
	public String getLabel() {
		if(ignorecase)
			return getCleanedValue(label).toLowerCase();
		else
			return getCleanedValue(label);
	}
	
	public String getRawText() {
		return text;
	}
	
	public String getText() {
		if(ignorecase)
			return getCleanedValue(text).toLowerCase();
		else
			return getCleanedValue(text);
	}
	
	public WebElement getWebElement() {
		return webElement;
	}

	/**
	 * Set during compare so you don't need run compare logic again to determine the 
	 * highest ranked (not demoted) of a sorted list.
	 */
	public boolean isDemoted() {
		return demoted;
	}

	/**
	 * The value has been compared and is found to be an invalid match (will be at the very bottom of the list)
	 * @return
	 */
	public boolean isDisqualified() {
		return disqualified;
	}

	/**
	 * Clean out all parts of a string that are not to be considered during a search to determine if an elements 
	 * text is a match for the search text. 
	 *    1) Trim and normalize whitespace
	 *    2) Remove colons.
	 * 
	 * @param s
	 * @return
	 */
	public static String getCleanedValue(String cleanable) {
		if(cleanable == null)
			return "";
		
		String s = new String(cleanable);
		// Normalize the spaces (trim from edges, and replace multiple contiguous whitespace with single space character)
		String cleaned = s.trim().replaceAll("\\s+", " ");
		// Trim off any sequence of colons and whitespace from the end of the text.
		cleaned = cleaned.replaceAll("(\\s*:\\s*)*$", "");
		return cleaned;
	}
	
	/**
	 * Trim off any non-alpha-numeric content from the beginning of a string if that trimmed content 
	 * ends where the specified value starts
	 * @param trimmable
	 * @param contained
	 * @return
	 */
	public static String trimLeftNonAlphaNumeric(String trimmable, String contained) {
		String s = new String(trimmable);
		int idx = s.indexOf(contained);
		String start = s.substring(0, idx);
		if(start.matches("[^a-zA-Z\\d]*")) {
			s = s.substring(idx);
		}
		return s;
	}

	public static List<WebElement> getHighestRanked(List<ComparableLabel> labels) {
		List<WebElement> highest = new ArrayList<WebElement>();
		if(labels == null || labels.isEmpty())
			return highest;
		
		if(labels.size() > 1) {
			Collections.sort(labels);
		}
		else {
			// Compare the one label to itself to run it through the disqualification logic
			labels.get(0).compareTo(labels.get(0));
		}
		
		for(ComparableLabel lbl : labels) {
			if(!lbl.isDemoted() && !lbl.isDisqualified() && lbl.getWebElement() != null) {
				highest.add(lbl.getWebElement());
			}
		}
		return highest;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ComparableLabel [getRawText()=")
				.append(getRawText()).append("]");
		return builder.toString();
	}
}
