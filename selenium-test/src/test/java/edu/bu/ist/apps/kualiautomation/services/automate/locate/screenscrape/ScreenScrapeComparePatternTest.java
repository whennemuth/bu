package edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.junit.Test;

public class ScreenScrapeComparePatternTest {

	/**
	 * Test the pattern match against a string for the only match to be found is the entire string, with no
	 * internal matches. Also test that any special regex characters found in the label are escaped so they 
	 * retain literal meaning.
	 */
	@Test
	public void testWholeStringMatch() {
		
		// 1) No characters to escape
		Matcher m = ScreenScrapeComparePattern.LABELLED_NUMBER.getMatcher("mylabel: 12345", "mylabel", true);
		String regex = m.pattern().toString();
		assertEquals("(?i)(?<!\\w)mylabel\\s*[:\\-]?\\s*\\d+(?!\\w)", regex);
		List<String> found = getMatches(m);
		assertEquals(1, found.size());
		assertEquals("mylabel: 12345", found.get(0));
		
		// 2) All characters to escape
		String label = "[].$^()*\\{}-?";
		String text = label + "   myvalue";
		m = ScreenScrapeComparePattern.LABELLED_WORD.getMatcher(text, label, true);
		regex = m.pattern().toString();
		String expectedRegex = 
				  "(?i)"
				+ "(?<!\\w)"
				+ "\\[\\]\\.\\$\\^\\(\\)\\*\\\\\\{\\}\\-\\?"
				+ "\\s*[:\\-]?\\s*"
				+ "[A-Za-z]+"
				+ "(?!\\w)";		
		assertEquals(expectedRegex, regex);
		found = getMatches(m);
		assertEquals(1, found.size());
		assertEquals(text, found.get(0));
		
		// 3) Some characters to escape
		label = "[ ] . $ mylabel ^ ( ) * \\ { } - ?";
		text = label + "A1B2C3";
		m = ScreenScrapeComparePattern.LABELLED_BLOCK.getMatcher(text, label, true);
		regex = m.pattern().toString();
		expectedRegex = 
				  "(?i)"
				+ "(?<!\\w)"
				+ "\\[ \\] \\. \\$ mylabel \\^ \\( \\) \\* \\\\ \\{ \\} \\- \\?"
				+ "\\s*[:\\-]?\\s*"
				+ "\\S+"
				+ "(?!\\w)";
		assertEquals(expectedRegex, regex);
		found = getMatches(m);
		assertEquals(1, found.size());
		assertEquals(text, found.get(0));
		
		// 4) Not ignoring case
		m = ScreenScrapeComparePattern.LABELLED_NUMBER.getMatcher("mylabel: 12345", "MYLABEL", false);
		regex = m.pattern().toString();
		assertEquals("(?<!\\w)MYLABEL\\s*[:\\-]?\\s*\\d+(?!\\w)", regex);
		found = getMatches(m);
		assertTrue(found.isEmpty());
	}
	
	/**
	 * Test the pattern match against a string that should yield multiple results.
	 */
	@Test
	public void testInnerStringMatch() {
		String label = "[mylabel]";
		
		// 1) Declare label and value pairs to find in the main string
		String[] occurrences = new String[]{
				"[mylabel] : 123",
				"[mylabel]:456",
				"[mylabel]789",
				"[mylabel] - 012",
				"[mylabel]   -   345",
				"[mylabel] 678",
				"[mylabel]  	901",
				"[MYLABEL]   : 3434"
		};
		
		// 2) Create the main String
		String text = "This is a test to find multiple matches. Here is the first match: "
				+ occurrences[0]
				+ " here is the second match: "
				+ occurrences[1]
				+ " here is the third match: "
				+ occurrences[2]
				+ " here is the fourth match: "
				+ occurrences[3]
				+ " here is the fifth match: "
				+ occurrences[4]
				+ " here is the sixth match: "
				+ occurrences[5]
				+ " here is the seventh match: "
				+ occurrences[6];
		
		// 3) Get the matches
		Matcher m = ScreenScrapeComparePattern.LABELLED_NUMBER.getMatcher(text, label, false);
		String regex = m.pattern().toString();
		assertEquals("(?<!\\w)\\[mylabel\\]\\s*[:\\-]?\\s*\\d+(?!\\w)", regex);
		List<String> found = getMatches(m);
		
		// 4) Assert expected results
		assertEquals(7, found.size());		
		assertEquals(occurrences[0], found.get(0));		
		assertEquals(occurrences[1], found.get(1));		
		assertEquals(occurrences[2], found.get(2));		
		assertEquals(occurrences[3], found.get(3));		
		assertEquals(occurrences[4], found.get(4));		
		assertEquals(occurrences[5], found.get(5));		
		assertEquals(occurrences[6], found.get(6));		
	}
	
	private List<String> getMatches(Matcher m) {
		List<String> found = new ArrayList<String>();
		while(m.find()) {
			found.add(m.group());
		}
		return found;
	}
}
