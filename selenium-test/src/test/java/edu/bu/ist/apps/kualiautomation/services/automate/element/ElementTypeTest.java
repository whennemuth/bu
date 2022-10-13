package edu.bu.ist.apps.kualiautomation.services.automate.element;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openqa.selenium.WebElement;

@RunWith(MockitoJUnitRunner.class)
public class ElementTypeTest {

	@Mock private WebElement textbox1;
	@Mock private WebElement textbox2;
	@Mock private WebElement password;
	@Mock private WebElement radio;
	@Mock private WebElement checkbox;
	@Mock private WebElement select;
	@Mock private WebElement textarea;
	@Mock private WebElement hyperlink;
	@Mock private WebElement button1;
	@Mock private WebElement button2;
	@Mock private WebElement submitButton;
	@Mock private WebElement buttonImage;
	@Mock private WebElement other1;
	@Mock private WebElement other2;
	

	
	@Before
	public void setUp() throws Exception {
		
		when(textbox1.getTagName()).thenReturn("input");
		when(textbox1.getAttribute("type")).thenReturn("TEXT");
		
		when(textbox2.getTagName()).thenReturn("input");
		
		when(password.getTagName()).thenReturn("input");
		when(password.getAttribute("type")).thenReturn("PASSWORD");
		
		when(radio.getTagName()).thenReturn("input");
		when(radio.getAttribute("type")).thenReturn("RADIO");
		
		when(checkbox.getTagName()).thenReturn("input");
		when(checkbox.getAttribute("type")).thenReturn("CHECKBOX");
		
		when(select.getTagName()).thenReturn("select");
		
		when(textarea.getTagName()).thenReturn("textarea");

		when(hyperlink.getTagName()).thenReturn("a");
		
		when(button1.getTagName()).thenReturn("input");
		when(button1.getAttribute("type")).thenReturn("BUTTON");
		
		when(submitButton.getTagName()).thenReturn("input");
		when(submitButton.getAttribute("type")).thenReturn("SUBMIT");
		
		when(button2.getTagName()).thenReturn("button");
		
		when(buttonImage.getTagName()).thenReturn("input");
		when(buttonImage.getAttribute("type")).thenReturn("image");
		
		when(other1.getTagName()).thenReturn("div");
		
		when(other2.getTagName()).thenReturn("input");
		when(other2.getAttribute("type")).thenReturn("bogus");
	}
	
	@Test
	public void testGetInstance() {
		
		ElementType et = ElementType.getInstance(textbox1);
		assertEquals(ElementType.TEXTBOX, et);
		
		et = ElementType.getInstance(textbox2);
		assertEquals(ElementType.TEXTBOX, et);
		
		et = ElementType.getInstance(password);
		assertEquals(ElementType.PASSWORD, et);
		
		et = ElementType.getInstance(radio);
		assertEquals(ElementType.RADIO, et);
		
		et = ElementType.getInstance(checkbox);
		assertEquals(ElementType.CHECKBOX, et);
		
		et = ElementType.getInstance(select);
		assertEquals(ElementType.SELECT, et);
		
		et = ElementType.getInstance(textarea);
		assertEquals(ElementType.TEXTAREA, et);
		
		et = ElementType.getInstance(hyperlink);
		assertEquals(ElementType.HYPERLINK, et);
		
		et = ElementType.getInstance(button1);
		assertEquals(ElementType.BUTTON, et);
		
		et = ElementType.getInstance(button2);
		assertEquals(ElementType.BUTTON, et);
		
		et = ElementType.getInstance(submitButton);
		assertEquals(ElementType.BUTTON, et);
		
		et = ElementType.getInstance(other1);
		assertEquals(ElementType.OTHER, et);
		
		et = ElementType.getInstance(other2);
		assertEquals(ElementType.OTHER, et);
		
	}

}
