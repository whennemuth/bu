package edu.bu.ist.apps.kualiautomation.services.automate.locate.label;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import edu.bu.ist.apps.kualiautomation.services.automate.locate.label.ComparableLabel;

public class ComparableLabelTest {

	@Test
	public void testSorting() {
		String label = " This is a test ";
		
		// Matches in order of match quality
		String text1 = new String(label);
		String text2 = "  This    is    a    test  ";
		String text3 = new String(label) + " some more";
		String text4 = new String(label) + " some more content";
		String text5 = "*%! This is a test";
		String text6 = "*%! This is a";
		String text7 = "bogus";
		
		ComparableLabel lbl1 = new ComparableLabel(label, text1, true) {
			@Override protected int customCompareTo(ComparableLabel lbl) { return 0; } };
			
		ComparableLabel lbl2 = new ComparableLabel(label, text2, true) {
			@Override protected int customCompareTo(ComparableLabel lbl) { return 0; } };
			
		ComparableLabel lbl3 = new ComparableLabel(label, text3, true) {
			@Override protected int customCompareTo(ComparableLabel lbl) { return 0; } };
			
		ComparableLabel lbl4 = new ComparableLabel(label, text4, true) {
			@Override protected int customCompareTo(ComparableLabel lbl) { return 0; } };
			
		ComparableLabel lbl7 = new ComparableLabel(label, text5, true) {
			@Override protected int customCompareTo(ComparableLabel lbl) { return 0; } };
			
		ComparableLabel lbl8 = new ComparableLabel(label, text6, true) {
			@Override protected int customCompareTo(ComparableLabel lbl) { return 0; } };
			
		ComparableLabel lbl9 = new ComparableLabel(label, text7, true) {
			@Override protected int customCompareTo(ComparableLabel lbl) { return 0; } };
									
		List<ComparableLabel> sorted = new ArrayList<ComparableLabel>();
		// Add in order of lowest ranked first:
		sorted.add(lbl9);
		sorted.add(lbl8);
		sorted.add(lbl7);
		sorted.add(lbl4);
		sorted.add(lbl3);
		sorted.add(lbl2);
		sorted.add(lbl1);
		
		// Sort. This should put the list in order of highest ranked first.
		Collections.sort(sorted);
		
		// The first two should be tied for first
		assertTrue(text1.equals(sorted.get(0).getRawText()) || text2.equals(sorted.get(0).getRawText()));
		assertTrue(text1.equals(sorted.get(1).getRawText()) || text2.equals(sorted.get(1).getRawText()));
		// The middle 3 should be be in 3rd, 4th, and 5th place (no ties).
		assertEquals(text3, sorted.get(2).getRawText());
		assertEquals(text4, sorted.get(3).getRawText());
		assertEquals(text5, sorted.get(4).getRawText());
		// The last two should be tied for last.
		assertTrue(text6.equals(sorted.get(5).getRawText()) || text7.equals(sorted.get(5).getRawText()));
		assertTrue(text6.equals(sorted.get(6).getRawText()) || text7.equals(sorted.get(6).getRawText()));
		
		assertFalse(sorted.get(0).isDemoted());
		assertFalse(sorted.get(1).isDemoted());
		assertTrue(sorted.get(2).isDemoted());
		assertTrue(sorted.get(3).isDemoted());
		assertTrue(sorted.get(4).isDemoted());
		assertTrue(sorted.get(5).isDemoted());
		assertTrue(sorted.get(6).isDemoted());
	}

}
