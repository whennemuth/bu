package edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape;

import org.openqa.selenium.WebElement;

/**
 * This class contains all the parameters for the constructor of ComparableScreenScrapeLabel.
 * The setters allow call chaining.
 * 
 * @author wrh
 *
 */
public class ComparableParameters {
	/**  */
	private WebElement webElement;
	/** pattern of the label */
	private ScreenScrapeComparePattern pattern;
	/** label The value the web element this label is based on was located by */
	private String label;
	/** The actual text of the web element this label is based on */
	private String text;
	/**
	 * Invoke the default comparison method if implementors of this class 
	 * find two unequal labels to compare as zero using their custom compare method
	 */
	private boolean useDefaultMethodIfIndeterminate;
	/** Ignore case when comparing */
	private boolean ignorecase;
	/**
	 * Keep all characters (except whitespace) of the scraped value once the label portion is removed
	 * and don't remove anything like colons, hyphens, etc.
	 */
	private boolean acceptRawValue;
	public WebElement getWebElement() {
		return webElement;
	}
	public ComparableParameters setWebElement(WebElement webElement) {
		this.webElement = webElement;
		return this;
	}
	public ScreenScrapeComparePattern getPattern() {
		return pattern;
	}
	public ComparableParameters setPattern(ScreenScrapeComparePattern pattern) {
		this.pattern = pattern;
		return this;
	}
	public String getLabel() {
		return label;
	}
	public ComparableParameters setLabel(String label) {
		this.label = label;
		return this;
	}
	public String getText() {
		return text;
	}
	public ComparableParameters setText(String text) {
		this.text = text;
		return this;
	}
	public boolean isUseDefaultMethodIfIndeterminate() {
		return useDefaultMethodIfIndeterminate;
	}
	public ComparableParameters setUseDefaultMethodIfIndeterminate(boolean useDefaultMethodIfIndeterminate) {
		this.useDefaultMethodIfIndeterminate = useDefaultMethodIfIndeterminate;
		return this;
	}
	public boolean isIgnorecase() {
		return ignorecase;
	}
	public ComparableParameters setIgnorecase(boolean ignorecase) {
		this.ignorecase = ignorecase;
		return this;
	}	
}
