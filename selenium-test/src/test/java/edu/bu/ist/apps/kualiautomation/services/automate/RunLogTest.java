package edu.bu.ist.apps.kualiautomation.services.automate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.entity.LabelAndValue;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

@RunWith(MockitoJUnitRunner.class)
public class RunLogTest {

	@Mock private Element element1;
	@Mock private Element element2;
	@Mock private Element element3;
	@Mock private WebElement textbox1;
	@Mock private WebElement textbox2;
	@Mock private WebElement button1;
	@Mock private LabelAndValue lvTextbox1;
	@Mock private LabelAndValue lvTextbox2;
	@Mock private LabelAndValue lvButton1;
	
	private RunLog runlog;
	
	@Before
	public void setUp() throws Exception {
		
		when(textbox1.getTagName()).thenReturn("input");
		when(textbox1.getAttribute("type")).thenReturn("text");
		when(textbox1.getText()).thenReturn("");
		when(textbox1.isDisplayed()).thenReturn(true);
		when(textbox1.isEnabled()).thenReturn(true);
		
		when(textbox2.getTagName()).thenReturn("input");
		when(textbox2.getAttribute("type")).thenReturn("text");
		when(textbox2.getText()).thenReturn("");
		when(textbox2.isDisplayed()).thenReturn(true);
		when(textbox2.isEnabled()).thenReturn(true);
		
		when(button1.getTagName()).thenReturn("input");
		when(button1.getAttribute("type")).thenReturn("button");
		when(button1.getText()).thenReturn("");
		when(button1.getAttribute("value")).thenReturn("submit");
		when(button1.isDisplayed()).thenReturn(true);
		when(button1.isEnabled()).thenReturn(true);
		
		when(lvTextbox1.getElementType()).thenReturn(ElementType.TEXTBOX.name());
		when(lvTextbox1.getElementTypeEnum()).thenReturn(ElementType.TEXTBOX);
		when(lvTextbox1.getLabel()).thenReturn("label for textbox 1");
		when(lvTextbox1.getIdentifier()).thenReturn("text");
		when(lvTextbox1.getValue()).thenReturn("Apples");
		
		when(lvTextbox2.getElementType()).thenReturn(ElementType.TEXTBOX.name());
		when(lvTextbox2.getElementTypeEnum()).thenReturn(ElementType.TEXTBOX);
		when(lvTextbox2.getLabel()).thenReturn("label for textbox 2");
		when(lvTextbox2.getIdentifier()).thenReturn("text");
		when(lvTextbox2.getValue()).thenReturn("Oranges");
		
		when(lvButton1.getElementTypeEnum()).thenReturn(ElementType.BUTTON);
		when(lvButton1.getElementType()).thenReturn(ElementType.BUTTON.name());
		when(lvButton1.getLabel()).thenReturn("submit");
		when(lvButton1.getIdentifier()).thenReturn("");
		
		when(element1.getElementType()).thenReturn(ElementType.TEXTBOX);
		when(element1.getWebElement()).thenReturn(textbox1);
		when(element1.isInteractive()).thenReturn(true);
		
		when(element2.getElementType()).thenReturn(ElementType.TEXTBOX);
		when(element2.getWebElement()).thenReturn(textbox2);
		when(element2.isInteractive()).thenReturn(true);
		
		when(element3.getElementType()).thenReturn(ElementType.BUTTON);
		when(element3.getWebElement()).thenReturn(button1);
		when(element3.isInteractive()).thenReturn(true);
		
		runlog = new RunLog(true);
	}

	private String getStringResults() throws IOException {
		ByteArrayOutputStream out = null;
		BufferedOutputStream buf = null;
		try {
			out = new ByteArrayOutputStream();
			buf = new BufferedOutputStream(out);
			runlog.printResults(buf);
			buf.close();
			return out.toString();
		} 
		finally {
			if(buf != null) {
				buf.close();
			}
		}
	}
	
	private void assertRetvalParts(String[] assertArray, String retval) {
		assertNotNull(retval);
		assertFalse(retval.isEmpty());
		
		String[] parts = retval.split("[\\r\\n]+");
		assertEquals(assertArray.length, parts.length);
		assertEquals(assertArray[0], parts[0]);
		assertEquals(assertArray[1], parts[1]);
		assertTrue(parts[2].matches("Started: \\d{2}:\\d{2}:\\d{2}"));
		assertTrue(parts[3].matches("Ended: \\d{2}:\\d{2}:\\d{2} \\(\\d+ seconds\\)"));
		assertTrue(parts[3].startsWith("Ended: "));
		for(int i=4; i<parts.length; i++) {
			assertEquals(assertArray[i], parts[i]);
		}
	}
	
	@Test
	public void testLog() throws IOException {
		runlog.log(element1, lvTextbox1);
		runlog.log(element2, lvTextbox2);
		runlog.log(element3, lvButton1);
		String retval = getStringResults();		
		String[] assertArray = new String[] {
			"Cycle Log Results:",
			"---------------------------------",
			"Started: 00:39:58",
			"Ended: 00:39:58 (0 seconds)",
			"Applying to element ",
			"   [input type=\"text\", text=\"\", value=\"null\" (criteria: type=\"TEXTBOX\", label=\"label for textbox 1\", identifier=\"text)] ",
			"   value: \"Apples\"",
			"Applying to element ",
			"   [input type=\"text\", text=\"\", value=\"null\" (criteria: type=\"TEXTBOX\", label=\"label for textbox 2\", identifier=\"text)] ",
			"   value: \"Oranges\"",
			"Clicking element ",
			"   [input type=\"button\", text=\"\", value=\"submit\" (criteria: type=\"BUTTON\", label=\"submit\", identifier=\")]"
		};
		
		assertRetvalParts(assertArray, retval);
	}

	@Test
	public void testElementNotFound() throws IOException {
		runlog.elementNotFound(lvTextbox1);
		runlog.elementNotFound(lvTextbox2);
		runlog.elementNotFound(lvButton1);
		String retval = getStringResults();
		
		String[] assertArray = new String[] {
			"Cycle Log Results:",
			"---------------------------------",
			"Started: 00:42:28",
			"Ended: 00:42:28 (0 seconds)",
			"Cannot find element ",
			"   (criteria: type=\"TEXTBOX\", label=\"label for textbox 1\", identifier=\"text)",
			"Cannot find element ",
			"   (criteria: type=\"TEXTBOX\", label=\"label for textbox 2\", identifier=\"text)",
			"Cannot find element ",
			"   (criteria: type=\"BUTTON\", label=\"submit\", identifier=\")"
		};
		
		assertRetvalParts(assertArray, retval);
	}

	@Test
	public void testMultipleElementCandidates() throws IOException {
		
		runlog.multipleElementCandidates(lvTextbox1, Arrays.asList(new Element[] {element1, element2, element3}));
		String retval = getStringResults();
		
		String[] assertArray = new String[] {
			"Cycle Log Results:",
			"---------------------------------",
			"Started: 01:02:08",
			"Ended: 01:02:08 (0 seconds)",
			"More than one element was found for the specified search criteria ",
			"   [input type=\"text\", text=\"\", value=\"null\" (criteria: type=\"TEXTBOX\", label=\"label for textbox 1\", identifier=\"text)], ",
			"   [input type=\"text\", text=\"\", value=\"null\" (criteria: type=\"TEXTBOX\", label=\"label for textbox 1\", identifier=\"text)], ",
			"   [input type=\"button\", text=\"\", value=\"submit\" (criteria: type=\"TEXTBOX\", label=\"label for textbox 1\", identifier=\"text)]"			
		};
		
		assertRetvalParts(assertArray, retval);
	}

	@Test
	public void testValueApplicationError() throws IOException {
		runlog.log(element1, lvTextbox1);
		runlog.valueApplicationError(lvTextbox2, element2);
		runlog.elementNotFound(lvButton1);
		String retval = getStringResults();
		
		String[] assertArray = new String[] {
			"Cycle Log Results:",
			"---------------------------------",
			"Started: 01:03:55",
			"Ended: 01:03:55 (0 seconds)",
			"Applying to element ",
			"   [input type=\"text\", text=\"\", value=\"null\" (criteria: type=\"TEXTBOX\", label=\"label for textbox 1\", identifier=\"text)] ",
			"   value: \"Apples\"",
			"Error encountered for element ",
			"   (criteria: type=\"TEXTBOX\", label=\"label for textbox 2\", identifier=\"text)]",
			"Cannot find element ",
			"   (criteria: type=\"BUTTON\", label=\"submit\", identifier=\")"	
		};
		
		assertRetvalParts(assertArray, retval);
	}

}
