package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.openqa.selenium.By;
import org.openqa.selenium.By.ByXPath;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClassBasedElementLocatorTest {

	private @Mock SearchContext ctx;
	private @Mock WebDriver driver;
	
	private @Mock WebElement wb1;
	private @Mock WebElement wb2;
	private @Mock WebElement wb3;
	private @Mock WebElement wb4;
	private @Mock WebElement wb5;
	private @Mock WebElement wb6;
	
	private ClassBasedElementLocator locator;
	private List<String> parameters1 = Arrays.asList(new String[]{"apples", "oranges", "pears", "bannanas"});
	private List<String> parameters2 = Arrays.asList(new String[]{"apples", "oranges", "pears"});
	private List<String> parameters3 = Arrays.asList(new String[]{"apples", "oranges"});
	private List<String> parameters4 = Arrays.asList(new String[]{"apples"});
	
	@Before
	public void setup() {
		
		when(driver.switchTo()).thenThrow(new NoSuchFrameException("testing"));
		
		setupStubber(new ArgMockLogic() { @Override public String getValue(String arg) {
			if("class".equalsIgnoreCase(arg)) {
				return "apples";
			}
			else {
				return null;
			}
		}}, wb1);
		
		setupStubber(new ArgMockLogic() { @Override public String getValue(String arg) {
			if("class".equalsIgnoreCase(arg)) {
				return "apples";
			}
			else {
				if(arg.equalsIgnoreCase("id"))
					return "oranges";
				else
					return null;
			}
		}}, wb2);
		
		setupStubber(new ArgMockLogic() { @Override public String getValue(String arg) {
			if("class".equalsIgnoreCase(arg)) {
				return "apples oranges";
			}
			else {
				if(arg.equalsIgnoreCase("name"))
					return "pears";
				if(arg.equalsIgnoreCase("placeholder"))
					return "bannanas";
				return null;
			}
		}}, wb3);
		
		setupStubber(new ArgMockLogic() { @Override public String getValue(String arg) {
			if("class".equalsIgnoreCase(arg)) {
				return "apples";
			}
			else {
				if(arg.equalsIgnoreCase("id"))
					return "oranges";
				if(arg.equalsIgnoreCase("name"))
					return "pears";
				if(arg.equalsIgnoreCase("placeholder"))
					return "bannanas";
				return null;
			}
		}}, wb4);
		
		setupStubber(new ArgMockLogic() { @Override public String getValue(String arg) {
			if("class".equalsIgnoreCase(arg)) {
				return "apples pears";
			}
			else {
				if(arg.equalsIgnoreCase("id"))
					return "oranges";
				if(arg.equalsIgnoreCase("name"))
					return "bannanas";
				return null;
			}
		}}, wb5);
		
		setupStubber(new ArgMockLogic() { @Override public String getValue(String arg) {
			if("class".equalsIgnoreCase(arg)) {
				return "apples oranges pears bannanas";
			}
			else {
				return null;
			}
		}}, wb6);
		
	}
	
	private void setupStubber(ArgMockLogic logic, WebElement webElement) {
		doAnswer(new Answer<String>(){
			@Override public String answer(InvocationOnMock invocation) throws Throwable {
				String retval = null;
				Object[] args = invocation.getArguments();
				if(args != null && args.length == 1) {
					retval = logic.getValue((String) args[0]);
				}
				return retval;
			}}).when(webElement).getAttribute(any(String.class));
		
		when(webElement.isDisplayed()).thenReturn(true);
		when(webElement.isEnabled()).thenReturn(true);
	}
	
	@Test
	public void testCustomLocate1() {
		when(ctx.findElements(any(By.class))).thenAnswer(new Answer<List<WebElement>>(){
			@Override public List<WebElement> answer(InvocationOnMock invocation) throws Throwable {
				By by = (By) invocation.getArguments()[0];
				if(by instanceof ByXPath)
					return Arrays.asList(new WebElement[]{wb1, wb2});
				else
					return new ArrayList<WebElement>();
			}});
		locator = new ClassBasedElementLocator(driver, ctx, null);
		List<Element> results = locator.locateAll(ElementType.OTHER, parameters1);
		assertTrue(results.isEmpty());	
		
		locator = new ClassBasedElementLocator(driver, ctx, null);
		results = locator.locateAll(ElementType.OTHER, parameters4);
		assertFalse(results.isEmpty());			
		assertEquals(2, results.size());
	}
	
	@Test
	public void testCustomLocate2() {
		when(ctx.findElements(any(By.class))).thenAnswer(new Answer<List<WebElement>>(){
			@Override public List<WebElement> answer(InvocationOnMock invocation) throws Throwable {
				By by = (By) invocation.getArguments()[0];
				if(by instanceof ByXPath)
					return Arrays.asList(new WebElement[]{wb2, wb3});
				else
					return new ArrayList<WebElement>();
			}});
		locator = new ClassBasedElementLocator(driver, ctx, null);
		List<Element> results = locator.locateAll(ElementType.OTHER, parameters1);
		assertFalse(results.isEmpty());		
		assertEquals(1, results.size());
		assertEquals("apples oranges", results.get(0).getWebElement().getAttribute("class"));
	}
	
	@Test
	public void testCustomLocate3() {
		when(ctx.findElements(any(By.class))).thenAnswer(new Answer<List<WebElement>>(){
			@Override public List<WebElement> answer(InvocationOnMock invocation) throws Throwable {
				By by = (By) invocation.getArguments()[0];
				if(by instanceof ByXPath)
					return Arrays.asList(new WebElement[]{wb3, wb4});
				else
					return new ArrayList<WebElement>();
			}});
		locator = new ClassBasedElementLocator(driver, ctx, null);
		List<Element> results = locator.locateAll(ElementType.OTHER, parameters1);
		assertFalse(results.isEmpty());		
		assertEquals(1, results.size());
		assertEquals("apples oranges", results.get(0).getWebElement().getAttribute("class"));
	}
	
	@Test
	public void testCustomLocate4() {
		when(ctx.findElements(any(By.class))).thenAnswer(new Answer<List<WebElement>>(){
			@Override public List<WebElement> answer(InvocationOnMock invocation) throws Throwable {
				By by = (By) invocation.getArguments()[0];
				if(by instanceof ByXPath)
					return Arrays.asList(new WebElement[]{wb3, wb5});
				else
					return new ArrayList<WebElement>();
			}});
		locator = new ClassBasedElementLocator(driver, ctx, null);
		List<Element> results = locator.locateAll(ElementType.OTHER, parameters1);
		assertFalse(results.isEmpty());		
		assertEquals(2, results.size());
	}
	
	@Test
	public void testCustomLocate5() {
		when(ctx.findElements(any(By.class))).thenAnswer(new Answer<List<WebElement>>(){
			@Override public List<WebElement> answer(InvocationOnMock invocation) throws Throwable {
				By by = (By) invocation.getArguments()[0];
				if(by instanceof ByXPath)
					return Arrays.asList(new WebElement[]{wb3, wb6});
				else
					return new ArrayList<WebElement>();
			}});
		locator = new ClassBasedElementLocator(driver, ctx, null);
		List<Element> results = locator.locateAll(ElementType.OTHER, parameters1);
		assertFalse(results.isEmpty());		
		assertEquals(1, results.size());
		assertEquals("apples oranges pears bannanas", results.get(0).getWebElement().getAttribute("class"));
	}
	
	@Test
	public void testCustomLocate6() {
		when(ctx.findElements(any(By.class))).thenAnswer(new Answer<List<WebElement>>(){
			@Override public List<WebElement> answer(InvocationOnMock invocation) throws Throwable {
				By by = (By) invocation.getArguments()[0];
				if(by instanceof ByXPath)
					return Arrays.asList(new WebElement[]{wb1, wb2, wb3, wb4, wb5, wb6});
				else
					return new ArrayList<WebElement>();
			}});
		locator = new ClassBasedElementLocator(driver, ctx, null);
		
		List<Element> results = locator.locateAll(ElementType.OTHER, parameters4);
		assertFalse(results.isEmpty());		
		assertEquals(6, results.size());
		
		locator = new ClassBasedElementLocator(driver, ctx, null);
		results = locator.locateAll(ElementType.OTHER, parameters3);
		assertFalse(results.isEmpty());		
		assertEquals(2, results.size());
		assertTrue(
				"apples oranges pears bannanas".equals(results.get(0).getWebElement().getAttribute("class"))
				||
				"apples oranges".equals(results.get(0).getWebElement().getAttribute("class")));
		assertTrue(
				"apples oranges pears bannanas".equals(results.get(1).getWebElement().getAttribute("class"))
				||
				"apples oranges".equals(results.get(1).getWebElement().getAttribute("class")));
		
		locator = new ClassBasedElementLocator(driver, ctx, null);
		results = locator.locateAll(ElementType.OTHER, parameters2);
		assertFalse(results.isEmpty());		
		assertEquals(1, results.size());
		assertEquals("apples oranges pears bannanas", results.get(0).getWebElement().getAttribute("class"));
		
		locator = new ClassBasedElementLocator(driver, ctx, null);
		results = locator.locateAll(ElementType.OTHER, parameters1);
		assertFalse(results.isEmpty());		
		assertEquals(1, results.size());
		assertEquals("apples oranges pears bannanas", results.get(0).getWebElement().getAttribute("class"));
	}

	// @Test
	public void testCompareTo() {
		// If you want to focus in tighter to how WebElement candidates for matching are managed test against this compareTo
	}

	public static abstract class ArgMockLogic {
		public abstract String getValue(String arg);
	}
}
