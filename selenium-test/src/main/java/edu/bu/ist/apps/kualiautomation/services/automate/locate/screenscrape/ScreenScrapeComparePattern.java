package edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ScreenScrapeComparePattern {		
	LABELLED_NUMBER(
			"\\d+",
			"Labelled Number",
			"The best matching label value, followed by a number (all digits, no spaces)",
			"Subaward ID: 12345678"),
	LABELLED_WORD(
			"[A-Za-z]+",
			"Labelled Word", 
			"The best matching label value, followed by a single word (all alpha, no spaces)",
			"Alpha ID: ABCdef"),
	LABELLED_BLOCK(
			"\\S+",
			"Labelled Block",
			"The best matching label value, followed by the first block of non-whitespace characters",
			"AlphaNum ID: T:349-0LF");

	private String valueRegex;
	private String shortname;
	private String description;
	private String example;
	
	private ScreenScrapeComparePattern(String valueRegex, String shortname, String description, String example) {
		this.valueRegex = valueRegex;
		this.shortname = shortname;
		this.description = description;
		this.example = example;
	}
	public String getDescription() {
		return description;
	}		
	public String getExample() {
		return example;
	}
	public String getRegex(String replace) {
		return getRegex(replace, true);
	}
	public String getShortname() {
		return shortname;
	}
	public String getRegex(String label, boolean ignorecase) {
		// Match the label with all reserved regex operators escaped.
		String labelRegex = label.replaceAll("([\\[\\]\\(\\)\\-\\^\\$\\.\\{\\}\\*\\?\\\\])", "\\\\$1");
		// Match a colon or hyphen padded by any amount of spaces or tabs, or just spaces and/or tabs.
		String separator = "\\s*[:\\-]?\\s*";
		// Match the value as indicated by the enum type
		String noWordCharsBehind = "(?<!\\w)";
		String noWordCharsAhead = "(?!\\w)";
		
		String regex = noWordCharsBehind + labelRegex + separator + valueRegex + noWordCharsAhead;
		if(ignorecase)
			regex = "(?i)" + regex;
		
		return regex;
	}
	public Pattern getPattern(String label, boolean ignorecase) {
		return Pattern.compile(getRegex(label, ignorecase));
	}
	public Matcher getMatcher(String text, String label, boolean ignorecase) {
		return getPattern(label, ignorecase).matcher(text);
	}
	public List<String> getMatches(String text, String label) {
		return getMatches(text, label, true);
	}
	public List<String> getMatches(String text, String label, boolean ignorecase) {
		Matcher m = getMatcher(text, label, ignorecase);
		List<String> matches = new ArrayList<String>();
		while(m.find()) {
			if(containsBlankLine(m.group())) {
				/**
				 * The separator cannot contain a blank line. This is because both the label and value
				 * come from two html elements that we want immediately adjacent to one another. If they
				 * are not block elements with no text intervene, then blank lines would be found in between.
				 * This may not be valid because in some cases, text can still render horizontally where 
				 * the innerText value would indicate line breaks. 
				 * Screen scraping can currently match against horizontal innerText only.
				 * 
				 * TODO: If it is required to match a label in a table column in one row and its
				 * value in the same column in the following row, more functionality is needed.
				 * The horizontal-only innerText matching would have to be supplemented with a "vertical"
				 * approach - i.e: Create a "TableScreenScraper locator".
				 * The user would have to know they were scraping a table and pick the right option.
				 */
				continue;
			}
			matches.add(m.group());
		}
		return matches;
	}
	
	public static boolean containsBlankLine(String s) {
		String trimmed = s.replaceAll("\\r\\n", "\n").trim();
		return trimmed.split("\\n").length > 2;
	}

	public static List<Bean> toJson() {
		List<Bean> beans = new ArrayList<Bean>();
		for(ScreenScrapeComparePattern ss : ScreenScrapeComparePattern.values()) {
			beans.add(new Bean(ss.name(), ss.getShortname(), ss.getDescription()));
		}
		return beans;
	}
	
	/**
	 * Simple bean for json use.
	 * @author wrh
	 *
	 */
	public static class Bean {
		private String id;
		private String name;
		private String description;
		public Bean(String id, String name, String description) {
			this.id = id;
			this.name = name;
			this.description = description;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
	}
}