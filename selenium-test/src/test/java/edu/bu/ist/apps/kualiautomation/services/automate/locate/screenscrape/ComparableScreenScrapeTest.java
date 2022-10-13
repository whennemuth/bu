package edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import edu.bu.ist.apps.kualiautomation.services.automate.locate.label.ComparableLabel;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ComparableScreenScrapeTest {

	@Test
	public void testIllegalState() {
		try {
			ComparableParameters parms1 = new ComparableParameters()
					.setLabel("label1")
					.setText("label1:value1")
					.setUseDefaultMethodIfIndeterminate(false)
					.setPattern(ScreenScrapeComparePattern.LABELLED_NUMBER);
			
			ComparableParameters parms2 = new ComparableParameters()
					.setLabel("label2")
					.setText(parms1.getLabel())
					.setUseDefaultMethodIfIndeterminate(parms1.isUseDefaultMethodIfIndeterminate())
					.setPattern(parms1.getPattern());
			
			(new ComparableScreenScrape(parms1)).compareTo(new ComparableScreenScrape(parms2));
		}
		catch(IllegalStateException e) { 
			return; // The test passes.
		}
		
		fail("Expected IllegalStateException for comparison of two ComparableScreenScrapeLabel "
				+ "instances with unequal labels");
	}
		
	/**
	 * Test scenarios where one ComparableScreenScrape instance must be ranked the same as another
	 */
	@Test
	public void testParity() {
		// 2 disqualified instances are equal
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel", 
				"mylabel: myvalue", true)).assertIsEquivalentTo("mylabelmyvalue");
		
		// 2 disqualified instances are equivalent even if one has a label match and the other does not 
		// (neither has a value that is numeric).
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel:", 
				"mylabel: myvalue", true)).assertIsEquivalentTo("mylabelmyvalue");

		// 2 qualified instances with identical text values are equivalent.
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel", 
				"mylabel123", true)).assertIsEquivalentTo("mylabel123");		

		// 2 qualified instances with identical labels but differing text values are equivalent.
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel", 
				"mylabel123", true)).assertIsEquivalentTo("mylabel4567");		

		// 2 qualified instances with the same number of label+pattern matches, 
		// but labels differing in :,-,and space characters are equivalent
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel", 
				"mylabel: 123", true)).assertIsEquivalentTo("mylabel123");		

		// 2 qualified instances with the same number of label+pattern matches, 
		// but labels differing in :,-,and space characters and differing text values are equivalent
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel", 
				"mylabel: 123", true)).assertIsEquivalentTo("mylabel4567");		
		
		// 2 qualifed instances with nested label+value combinations are equivalent if the concatenated union
		// of all non-matching content are both of equal length when compared to each other
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel", 
				"abc mylabel 123 def", true)).assertIsEquivalentTo("ghi mylabel 456 jkl");
		
		// 2 qualifed instances with nested label+value combinations are equivalent if the concatenated union
		// of all non-matching content are both of equal length when compared to each other, even though
		// their labels differ in :,-,and space characters
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel", 
				"abc mylabel :  123 def", true)).assertIsEquivalentTo("ghi mylabel 456 jkl");
	}
	
	/**
	 * Test scenarios where one ComparableScreenScrape instance is "better" than another
	 */
	@Test
	public void testDisparity() {
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel:", 
				"mylabel: 12345", true)).assertIsBetterMatchThan("mylabel12345");
		
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel:", 
				"mylabel12345", true)).assertIsWorseMatchThan("mylabel: 12345");
		
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel:", 
				"mylabel: 12345", true)).assertIsBetterMatchThan("my two labels: mylabel: 12345 and mylabel:34567 end");
		
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel:", 
				"mylabel: alpha", true)).assertIsWorseMatchThan("my two labels: mylabel: 12345 and mylabel:34567 end");
		
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel:", 
				"mylabel: 12345", true)).assertIsBetterMatchThan("paddedmylabel: 12345padded");
		
		(new ComparableObj(ScreenScrapeComparePattern.LABELLED_NUMBER, "mylabel:", 
				"mylabel: 12345", true)).assertIsBetterMatchThan("padded mylabel: 12345 padded");
	}
	
	/**
	 * Test the comparison logic by putting comparable instances in a collection and asserting that it sorts as expected.
	 */
	@Test
	public void testSorting() {
	
		// 1) Create an array of ComparabelScreenScrape.getRawText() values in the order they should be found 
		// in a sorted collection of corresponding ComparabelScreenScrape instances. 
		String[] expectedOrder = new String[] {
				"mylabel: myvalue1",
				"chaff mylabel: myvalue2 chaff",
				"mylabel: myvalue3, mylabel: myvalue4",
				"chaff mylabel: myvalue3, mylabel: myvalue4",
				"chaff mylabel: myvalue3, mylabel: myvalue4 more chaff",
				"chaff mylabel: myvalue3 chaff mylabel: myvalue4 the most chaff",
				"disqualified",
		};
		
		// 2) Create a list of ComparableScreenScrape instances in an "unsorted" order by adding them in reverse
		List<ComparableLabel> actualOrder = new ArrayList<ComparableLabel>();
		for(int i=expectedOrder.length-1; i>=0; i--) {
			actualOrder.add(new ComparableScreenScrape(
					new ComparableParameters()
					.setLabel("mylabel:")
					.setPattern(ScreenScrapeComparePattern.LABELLED_BLOCK)
					.setText(expectedOrder[i])
					.setIgnorecase(true)
					.setUseDefaultMethodIfIndeterminate(false)));
		}
		
		// 3) Sort the collection back into the correct order
		Collections.sort(actualOrder);
		
		// 4) Assert the instances appear in the collection in the same order they appear in sortedTextvals
		for (ListIterator<ComparableLabel> iterator = actualOrder.listIterator(); iterator.hasNext();) {
			ComparableLabel c = (ComparableLabel) iterator.next();
			String expected = expectedOrder[iterator.previousIndex()];
			String actual = c.getRawText();
			assertEquals(expected, actual);
		}
	}
	
	@Test
	public void testGetValues() {
		
		ComparableScreenScrape scrape1 = new ComparableScreenScrape(
				new ComparableParameters()
				.setLabel("mylabel")
				.setPattern(ScreenScrapeComparePattern.LABELLED_BLOCK)
				.setText("chaff mylabel: myvalue1 chaff")
				.setIgnorecase(true)
				.setUseDefaultMethodIfIndeterminate(false));
		
		ComparableScreenScrape scrape2 = scrape1.changeText("chaff mylabel myvalue2 chaff");
		ComparableScreenScrape scrape3 = scrape1.changeText("chaff mylabelmyvalue3 chaff");
		ComparableScreenScrape scrape4 = scrape1.changeText("chaff mylabel - myvalue4a chaff mylabel myvalue4b chaff");
		ComparableScreenScrape scrape5 = scrape1.changeText("chaff mylabel -myvalue4a chaff mylabel myvalue4b: chaff");
		ComparableScreenScrape scrape6 = scrape1.changeText("chaff mylabel myvalue4a- chaff mylabel :myvalue4b chaff");
		ComparableScreenScrape scrape7 = scrape1.changeText("chaff mylabel :myva&lue4a- chaff mylabel :myva-lue4b$ chaff");
		
		// making sure each instance is processed in a compareTo method call. 
		// It does not matter what instance is compared to which other, along as they are all included.
		scrape1.compareTo(scrape2);
		scrape1.compareTo(scrape3);
		scrape3.compareTo(scrape4);
		scrape4.compareTo(scrape5);
		scrape5.compareTo(scrape6);
		scrape6.compareTo(scrape7);
		
		assertEquals(0, scrape1.getScrapedValues().size());
		assertEquals(1, scrape2.getScrapedValues().size());
		assertEquals("myvalue2", scrape2.getScrapedValues().get(0));		
		assertEquals(1, scrape3.getScrapedValues().size());
		assertEquals("myvalue3", scrape3.getScrapedValues().get(0));
		assertEquals(1, scrape4.getScrapedValues().size());
		assertEquals("myvalue4b", scrape4.getScrapedValues().get(0));
		assertEquals(2, scrape5.getScrapedValues().size());
		assertEquals("myvalue4a", scrape5.getScrapedValues().get(0));
		assertEquals("myvalue4b", scrape5.getScrapedValues().get(1));
		assertEquals(2, scrape6.getScrapedValues().size());
		assertEquals("myvalue4a", scrape6.getScrapedValues().get(0));
		assertEquals("myvalue4b", scrape6.getScrapedValues().get(1));
		assertEquals(2, scrape7.getScrapedValues().size());
		assertEquals("myva&lue4a", scrape7.getScrapedValues().get(0));
		assertEquals("myva-lue4b", scrape7.getScrapedValues().get(1));
		
		
		scrape1 = scrape1.setPattern(ScreenScrapeComparePattern.LABELLED_WORD).changeText("chaff mylabel: myvalue chaff");
		scrape2 = scrape2.setPattern(ScreenScrapeComparePattern.LABELLED_WORD).changeText("chaff mylabel myvalue chaff");
		scrape3 = scrape3.setPattern(ScreenScrapeComparePattern.LABELLED_WORD).changeText("chaff mylabelmyvalue chaff");
		scrape4 = scrape4.setPattern(ScreenScrapeComparePattern.LABELLED_WORD).changeText("chaff mylabel - myvalueA chaff mylabel myvalueB chaff");
		scrape1.compareTo(scrape2);
		scrape1.compareTo(scrape3);
		scrape3.compareTo(scrape4);
		
		assertEquals(1, scrape1.getScrapedValues().size());
		assertEquals("myvalue", scrape1.getScrapedValues().get(0));
		assertEquals(1, scrape2.getScrapedValues().size());
		assertEquals("myvalue", scrape2.getScrapedValues().get(0));		
		assertEquals(1, scrape3.getScrapedValues().size());
		assertEquals("myvalue", scrape3.getScrapedValues().get(0));
		assertEquals(2, scrape4.getScrapedValues().size());
		assertEquals("myvalueA", scrape4.getScrapedValues().get(0));
		assertEquals("myvalueB", scrape4.getScrapedValues().get(1));
		
	}
	
	/**
	 * Test that instances are disqualified due to the specified label being part of the string, but not
	 * in such a way as matches the expected pattern
	 */
	@Test
	public void testDisqualified() {
		
		ComparableScreenScrape scrape1 = new ComparableScreenScrape(
				new ComparableParameters()
				.setLabel("mylabel")
				.setPattern(ScreenScrapeComparePattern.LABELLED_NUMBER)
				.setText("chaffmylabel: 12345") // would qualify if it was "chaff mylabel: 12345"
				.setIgnorecase(true)
				.setUseDefaultMethodIfIndeterminate(false));
		
		ComparableScreenScrape scrape2 = scrape1.changeText("mylabelchaff: 12345"); // would qualify if "chaff" were removed.
		
		scrape1.compareTo(scrape2);
		
		assertTrue(scrape1.isDisqualified());
		assertTrue(scrape2.isDisqualified());
	}

	/**
	 * Utility class for asserting compareTo() results. "Better" items would be occur before
	 * "Worse" items in a sorted list of comparable items.
	 * 
	 * @author wrh
	 *
	 */
	private class ComparableObj {
		private ComparableScreenScrape thisComparable;
		public ComparableObj(ScreenScrapeComparePattern pattern, String label, String text, boolean ignorecase) {
			this.thisComparable = new ComparableScreenScrape(
					new ComparableParameters()
					.setPattern(pattern)
					.setLabel(label)
					.setText(text)
					.setIgnorecase(ignorecase)
					.setUseDefaultMethodIfIndeterminate(false));
		}
		private int compare(String otherText) {
			ComparableScreenScrape otherComparable = thisComparable.changeText(otherText);
			return thisComparable.compareTo(otherComparable);
		}
		public void assertIsEquivalentTo(String otherText) {
			assertEquals(0, compare(otherText));
		}
		public void assertIsBetterMatchThan(String otherText) {
			assertEquals(-1, compare(otherText));
		}
		public void assertIsWorseMatchThan(String otherText) {
			assertEquals(1, compare(otherText));
		}
	}
}
