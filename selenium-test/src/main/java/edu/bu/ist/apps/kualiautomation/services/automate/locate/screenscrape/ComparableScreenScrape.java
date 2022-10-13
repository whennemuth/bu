package edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.locate.label.ComparableLabel;

/**
 * This class represents the innerText of an html element, which includes the innerText of all its descendent elements.
 * One of more occurrences of a particular combined label and value substring exist within the text, where the label
 * is a fixed string and the value matches a pattern.
 * An instance of this class is comparable because the innerText values are the result of a search and their are
 * criteria for evaluating one match better than another.
 * For example, considering the following two html elements:
 * 
 * <p><pre>
 * <b>1) html:</b>
 *     {@code <div> outer text 
 *         <div>
 *             <span>mylabel</span>
 *             <span>myvalue</span>
 *         </div>
 *         more outer text
 *     </div>}  
 *    <b>innerText:</b>
 *     {@code "outer text mylabel myvalue more outer text:}
 * 
 * <b>2) html:</b>
 *     {@code <span>mylabel</span> <span>myvalue</span> }
 *    <b>innerText:</b>
 *     {@code "mylabel myvalue"}
 * </pre></p>
 * 
 * bother the innerText values contain the {@code "mylabel myvalue"} string, but descendents are preferred so as to limit
 * extraneous content, and example 2 html could be the innerHTML of example 1 html and is a descendent, and would win out
 * in a comparison between the two. Other criteria also apply, but this is the most common one. 
 * 
 * @author wrh
 *
 */
public class ComparableScreenScrape extends ComparableLabel {

	private ScreenScrapeComparePattern pattern;
	private String cleanlabel;
	private String cleantext;
	private List<String> matches;
	private List<String> scrapedValues;
	private boolean caseMatch;
	
	/** Temporary local variable for another instance being comparedTo (remains null outside of customCompareTo()) */
	private ComparableScreenScrape other;
	
	/**
	 * Restrict this super constructor
	 */
	private ComparableScreenScrape(String label, String text, boolean useDefaultMethodIfIndeterminate) {
		super(label, text, useDefaultMethodIfIndeterminate);
	}

	/**
	 * Restrict this super constructor
	 */
	private ComparableScreenScrape(WebElement webElement, String label, String text,
			boolean useDefaultMethodIfIndeterminate) {
		super(webElement, label, text, useDefaultMethodIfIndeterminate);
	}

	/**
	 * Constructor
	 * @param parms
	 */
	public ComparableScreenScrape(ComparableParameters parms) {
		
		super(parms.getLabel(), parms.getText(), parms.isUseDefaultMethodIfIndeterminate());
		
		this.webElement = parms.getWebElement();
		this.pattern = parms.getPattern();
		
		this.cleanlabel = cleanLines(getRawLabel());
		this.cleantext = cleanLines(getRawText());
	}

	@Override
	protected int customCompareTo(ComparableLabel otherComparable) {
		
		try {
			other = (ComparableScreenScrape) otherComparable;			
			other.other = this;
					
			qualify();
			
			if(this.disqualified && other.disqualified)
				return ITS_A_DRAW;
			
			if(this.disqualified)
				return OTHER_LABEL_IS_BETTER;
			
			if(other.disqualified)
				return THIS_LABEL_IS_BETTER;
			
			if(cleantext.equals(other.cleantext))
				return ITS_A_DRAW;
			
			if(matches.size() == other.matches.size()) {
				String regex = pattern.getRegex(cleanlabel, true);
				String thisChaff = cleanLines(cleantext.replaceAll(regex, ""));
				String otherChaff = cleanLines(other.cleantext.replaceAll(regex, ""));
				if(thisChaff.length() == otherChaff.length()) {
					if(!ignorecase) {
						if(this.caseMatch && !other.caseMatch)
							return THIS_LABEL_IS_BETTER;
						if(other.caseMatch && !this.caseMatch)
							return OTHER_LABEL_IS_BETTER;
					}
					return ITS_A_DRAW;
				}
				else {
					// The following comparison is somewhat arbitrary - we are "separating the wheat from the chaff".
					// The content in which the matches were found has the matching content removed 
					// and whoever has the least remaining "chaff" characters left is the "winner".
					return thisChaff.length() < otherChaff.length() ?
							THIS_LABEL_IS_BETTER : 
							OTHER_LABEL_IS_BETTER; 
				}
			}
			else {
				return matches.size() < other.matches.size() ? 
						THIS_LABEL_IS_BETTER : // Probably a descendent of other
						OTHER_LABEL_IS_BETTER; // Probably an ancestor of other
			}
		} 
		finally {
			other.other = null;
			other = null;
		}
	}
	
	/**
	 * Determine if the label match qualifies with a sufficiently matching value string immediately to its right. 
	 */
	private void qualify() {
		
		if(matches != null || disqualified) {
			if(other.matches == null && !other.disqualified) {
				other.qualify();
			}
			return; // Has already run once.
		}
		
		if(!cleanlabel.equals(other.cleanlabel)) {
			// If this error is thrown, then it means a programming error has been made.
			throw new IllegalStateException("compareTo() method called against two ComparableScreenScrapeLabel "
					+ "instances with unequal labels");
		}
		
		if(!cleantext.toLowerCase().contains(cleanlabel.toLowerCase())) {
			// Should never happen as a regex found the label, so it must be contained.
			disqualified = true;
			return;
		}
		
		scrape();		
		
		other.qualify();
	}

	/**
	 * "Scrape" the labelled value away from the label.
	 */
	public void scrape() {
		// Up to now we know the screenScrape was a match because the label was found by xpath.
		// However if the characters that immediately follow it do not match what is expected, then disqualify.
		matches = pattern.getMatches(cleantext, cleanlabel, false);
		
		if(matches.isEmpty()) {
			// Exact case match for label failed, so try ignorecase
			matches = pattern.getMatches(cleantext, cleanlabel, true);
			if(matches.isEmpty()) {
				disqualified = true;
			}
		}
		else {
			caseMatch = true;
		}		
	}
	
	/**
	 * Replace in a string all multiple consecutive occurrences of spaces and tabs with a single space, but not returns.
	 * 
	 * @param s
	 * @return
	 */
	private String cleanLines(String lines) {
		return lines.trim().replaceAll("[\\x20\\t]+", " ");
	}
	
	/**
	 * Get a "clone", but with a different text value set.
	 * @param newText
	 * @return
	 */
	public ComparableScreenScrape changeText(String newText) {
		return new ComparableScreenScrape(
			new ComparableParameters()
				.setLabel(getRawLabel())
				.setPattern(pattern)
				.setText(newText)
				.setUseDefaultMethodIfIndeterminate(useDefaultMethodIfIndeterminate)
				.setWebElement(webElement)
				.setIgnorecase(ignorecase)
		);
	}

	public ComparableScreenScrape setPattern(ScreenScrapeComparePattern pattern) {
		return new ComparableScreenScrape(
				new ComparableParameters()
					.setLabel(getRawLabel())
					.setPattern(pattern)
					.setText(getText())
					.setUseDefaultMethodIfIndeterminate(useDefaultMethodIfIndeterminate)
					.setWebElement(webElement)
					.setIgnorecase(ignorecase)
			);
	}

	public List<String> getMatches() {
		return matches;
	}

	/**
	 * Return an adapted version of the webElement that returns the "scraped" value from instead of the whole getText() value.
	 */
	@Override
	public WebElement getWebElement() {
		if(scrapedValues == null && matches != null && !matches.isEmpty())
			getScrapedValues();
		
		if(scrapedValues == null || scrapedValues.isEmpty())
			return null;
//			return webElement;
		else
			return new ScreenScrapeWebElement(webElement, this);
	}

	public boolean hasWebElement() {
		return webElement != null;
	}
	
	/**
	 * Remove all non-alpha-numeric characters from both edges of a string.
	 * @param s
	 * @return
	 */
	private String trimNonAlphaNumFromEdges(String s) {
		if(s != null && !s.isEmpty()) {
			String first = s.substring(0, 1);
			if(first.matches("[^\\dA-Za-z]")) {
				return trimNonAlphaNumFromEdges(s.substring(1));
			}
			else {
				String last = s.substring(s.length()-1);
				if(last.matches("[^\\dA-Za-z]")) {
					return trimNonAlphaNumFromEdges(s.substring(0, s.length()-1));
				}
			}
		}
		return s;
	}
	
	/**
	 * The "scraped" value we are trying to screen scrape for is found in this list. 
	 * The list should have only one entry, though there may be more if the criteria for finding it within the 
	 * html page was not sufficiently specific or the most specific criteria possible yet produces multiple results.
	 * 
	 * @return
	 */
	public List<String> getScrapedValues() {
		if(scrapedValues != null)
			return scrapedValues;
		
		scrapedValues = new ArrayList<String>(matches.size());
		for(String match : matches) {
			int idx = match.indexOf(getRawLabel()) + getRawLabel().length();
			String value = match.substring(idx).trim();
			if(ScreenScrapeComparePattern.LABELLED_BLOCK.equals(pattern)) {
				value = value.split("\\s+")[0].replaceAll("[\\s]", "");
				value = trimNonAlphaNumFromEdges(value);
			}
			else {
				value = value.replaceAll("[\\:\\-\\s]", ""); 
			}
			if(!value.isEmpty())
				scrapedValues.add(value);
		}
		return scrapedValues;
	}
}
