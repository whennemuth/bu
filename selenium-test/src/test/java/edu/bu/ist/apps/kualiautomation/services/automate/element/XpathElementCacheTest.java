package edu.bu.ist.apps.kualiautomation.services.automate.element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class XpathElementCacheTest extends AbstractJettyBasedTest {

	@Override
	public void setupBefore() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("inner", ""
				+ "<html><body leftmargin=10 rightmargin=10>"
				+ "    <h3>This is the inner page</h3>"
				+ "</body></html>");
		handlers.put("outer", ""
				+ "<html><body leftmargin=50 rightmargin=50>"
				+ "    <p><h3>This is the outer page</h3></p>"
				+ "    <iframe src='http://localhost:8080/inner' style='width:50%; height:50%;'></iframe>"
				+ "</body></html>");
	}
	
	private List<WebElement> getHyperlinks(int size, String id) {
		List<WebElement> webElements = new ArrayList<WebElement>();
		for(int i=1; i<=size; i++) {
			WebElement we = Mockito.mock(WebElement.class);
			when(we.getTagName()).thenReturn("a");
			when(we.getText()).thenReturn(id + String.valueOf(i));
			when(we.getAttribute("id")).thenReturn(id + String.valueOf(i));
			webElements.add(we);
		}
		return webElements;
	}

	@Test
	public void test01XpathAndContext() {
		boolean inFrame = false;
		String xpath = ElementType.HYPERLINK.getXpathSelector();
		driver.get("http://localhost:8080/outer");
		
		// 1) Add 3 brand new results to the cache
		XpathElementCache.put(driver, xpath, false, getHyperlinks(3, "test1-"));
		assertEquals(1, XpathElementCache.size());
		assertEquals(3, XpathElementCache.get(driver, xpath, inFrame).size());

		// 2) Should not be able to add the same results for the same search to the cache
		WebElement iframe = driver.findElement(By.tagName("iframe"));
		assertNotNull(iframe);
		(new WebDriverWait(driver, 5)).until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframe));
		inFrame = true;
		
		// 3) Should be able to add the same search against a frame to the cache.
		XpathElementCache.put(driver, xpath, inFrame, getHyperlinks(4, "test2-"));
		assertEquals(2, XpathElementCache.size());
		assertEquals(4, XpathElementCache.get(driver, xpath, inFrame).size());
		
		// 4) Should not be able to add the same search against the same frame to the cache.
		XpathElementCache.put(driver, xpath, inFrame, getHyperlinks(4, "test2-"));
		assertEquals(2, XpathElementCache.size());
		assertEquals(4, XpathElementCache.get(driver, xpath, inFrame).size());
	
		// 5) Should be able to add a new search to the same frame to the cache
		xpath = "something new";
		XpathElementCache.put(driver, xpath, inFrame, getHyperlinks(5, "test3-"));
		assertEquals(3, XpathElementCache.size());
		assertEquals(5, XpathElementCache.get(driver, xpath, inFrame).size());

		// 6) Should not be able to add the same search to the same frame to the cache
		XpathElementCache.put(driver, xpath, inFrame, getHyperlinks(5, "test3-"));
		assertEquals(3, XpathElementCache.size());
		assertEquals(5, XpathElementCache.get(driver, xpath, inFrame).size());
		
		// 7) Should be able to get out of the frame and add the last search attempted in the frame to the cache
		driver.switchTo().defaultContent();
		inFrame = false;
		XpathElementCache.put(driver, xpath, inFrame, getHyperlinks(6, "test4-"));
		assertEquals(4, XpathElementCache.size());
		assertEquals(6, XpathElementCache.get(driver, xpath, inFrame).size());

		// 8) Should not be able to repeat the last search if nothing else has changed.
		XpathElementCache.put(driver, xpath, inFrame, getHyperlinks(6, "test4-"));
		assertEquals(4, XpathElementCache.size());
		assertEquals(6, XpathElementCache.get(driver, xpath, inFrame).size());

	}

}
