package edu.bu.ist.apps.kualiautomation.services.automate.locate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;
import edu.bu.ist.apps.kualiautomation.entity.Cycle;
import edu.bu.ist.apps.kualiautomation.entity.LabelAndValue;
import edu.bu.ist.apps.kualiautomation.entity.Suite;
import edu.bu.ist.apps.kualiautomation.services.automate.CycleRunner;
import edu.bu.ist.apps.kualiautomation.services.automate.RunLog;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

/**
 * This test runs through a mock cycle where the CycleRunner starts off on a page with a link that is first clicked
 * to open up a separate window. The test applies a value to an element(s) on that page, closes it and applies a 
 * value to the original page.
 * 
 * @author wrh
 *
 */
@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NewWindowLocatorTest extends AbstractJettyBasedTest {
	
	private CycleRunner runner;
	private String url = "http://localhost:8080/new-window-test1";
	
	@Mock private LabelAndValue lvTextbox1;
	@Mock private LabelAndValue lvTextbox2;
	@Mock private LabelAndValue lvLink1;
	@Mock private LabelAndValue lvLink2;
	@Mock Suite suite;
	@Mock Cycle cycle;

	@Override
	public void setupBefore() {
		
		when(lvTextbox1.getElementType()).thenReturn(ElementType.TEXTBOX.name());
		when(lvTextbox1.getElementTypeEnum()).thenReturn(ElementType.TEXTBOX);
		when(lvTextbox1.getLabel()).thenReturn(null);
		when(lvTextbox1.getIdentifier()).thenReturn("text1");
		when(lvTextbox1.getValue()).thenReturn("Apples");
		
		when(lvTextbox2.getElementType()).thenReturn(ElementType.TEXTBOX.name());
		when(lvTextbox2.getElementTypeEnum()).thenReturn(ElementType.TEXTBOX);
		when(lvTextbox2.getLabel()).thenReturn(null);
		when(lvTextbox2.getIdentifier()).thenReturn("text2");
		when(lvTextbox2.getValue()).thenReturn("Oranges");
		
		when(lvLink1.getElementType()).thenReturn(ElementType.HYPERLINK.name());
		when(lvLink1.getElementTypeEnum()).thenReturn(ElementType.HYPERLINK);
		when(lvLink1.getLabel()).thenReturn("link1");
		
		when(lvLink2.getElementType()).thenReturn(ElementType.HYPERLINK.name());
		when(lvLink2.getElementTypeEnum()).thenReturn(ElementType.HYPERLINK);
		when(lvLink2.getLabel()).thenReturn("link2");
		
		
		when(suite.getLabelAndValues()).thenReturn(new LinkedHashSet<LabelAndValue>(Arrays.asList(new LabelAndValue[]{ 
				lvTextbox1, 
				lvLink1,
				lvTextbox2,
				lvLink2
		})));
		
		when(cycle.getSuites()).thenReturn(new HashSet<Suite>(Arrays.asList(new Suite[]{ suite })));
		
		driver.get(url);
		runner = new CycleRunner(driver, cycle);
	}

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("new-window-test1", "<html><body>"
				+ "<input type='text' id='text1'>"
				+ "<a id='link1' target='_blank' href='new-window-test2'>link 1</a>"
				+ "</body></html>");
		handlers.put("new-window-test2", "<html><body>"
				+ "<input type='text' id='text2'>"
				+ "<a id='link2' target='#' onclick='window.close();'>link 2</a>"
				+ "</body></html>");
	}

	@Test
	public void test01() {
		RunLog runlog = runner.run();
		
		String log = runlog.getResults();
		
		assertNotNull(log);
		
		// Assert the first hyperlink was found and clicked.
		int linkClick1 = log.toLowerCase().indexOf("clicking element");
		assertTrue(linkClick1 > -1);
		
		// Assert the second hyperlink was found and clicked.
		int linkClick2 = log.toLowerCase().indexOf("clicking element", linkClick1+1);
		assertTrue(linkClick2 > -1);

	}

}
