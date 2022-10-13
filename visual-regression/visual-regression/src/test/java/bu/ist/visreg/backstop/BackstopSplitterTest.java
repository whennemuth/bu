package bu.ist.visreg.backstop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import bu.ist.visreg.basket.Basket;
import bu.ist.visreg.basket.BasketItem;
import bu.ist.visreg.basket.filesystem.FileBasketItem;
import bu.ist.visreg.util.FileUtils;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BackstopSplitterTest {

	private AbstractBackstopSplitter splitter = new AbstractBackstopSplitter() {
		@Override public BasketItem pieceToBasketItem(BasketItem unsplitBasketItem, String json, String pathname) {
			return unsplitBasketItem.getSplitItem(json, pathname);
		}				
	};
	
	private BasketItem getBasketItemPartialMock(String jsonFileName) {
		String json = FileUtils.getClassPathResourceContent("job-definitions/" + jsonFileName);
		return new BasketItem(null, "/some/path/name/", json) {
			@Override public BasketItem getSplitItem(String json, String pathname) {
				String newPathName = getExtendedPathname(pathname);
				return new FileBasketItem(basket, newPathName, json);
			}
			// These shouldn't get called
			@Override public void commitBasketMove(Basket nextBasket) throws Exception { }
			@Override public boolean persist() throws Exception { return false; }
			@Override public boolean delete() { return false; }			
		};
	}
	
	@Test
	public void testSplitIntoPieces1() {
		BasketItem unsplitBasketItem = getBasketItemPartialMock("JobDefinitionMixedValidity.json");				
		try {
			List<BasketItem> items = splitter.splitIntoPieces(unsplitBasketItem);
			assertFalse(unsplitBasketItem.isValid());			
			assertEquals(1, items.size());
			BasketItem basketItem = items.get(0);
			assertTrue(basketItem.isValid());			
			assertEquals(2, unsplitBasketItem.getInvalidMessages().size());
			assertEquals("MyJob.label1.viewport missing parameter: width", unsplitBasketItem.getInvalidMessages().get(0));
			assertEquals("MyJob.label1.viewport missing parameter: height", unsplitBasketItem.getInvalidMessages().get(1));
		} 
		catch (Exception e) {
			e.printStackTrace(System.out);
			fail("Not expecting exception");
		}
	}
	
	@Test
	public void testSplitIntoPieces2() {
		BasketItem unsplitBasketItem = getBasketItemPartialMock("JobDefinitionOneInvalidAttribute.json");				
		try {
			List<BasketItem> items = splitter.splitIntoPieces(unsplitBasketItem);
			assertFalse(unsplitBasketItem.isValid());			
			assertTrue(items.isEmpty());
			assertEquals(2, unsplitBasketItem.getInvalidMessages().size());
			assertEquals("MyJob.viewport missing parameter: label", unsplitBasketItem.getInvalidMessages().get(0));
			assertEquals("MyJob.viewport missing parameter: height", unsplitBasketItem.getInvalidMessages().get(1));		
		} 
		catch (Exception e) {
			e.printStackTrace(System.out);
			fail("Not expecting exception");
		}
	}
	
	@Test
	public void testSplitIntoPieces3() {
		BasketItem unsplitBasketItem = getBasketItemPartialMock("JobDefinitionOneInvalidScenario.json");				
		try {
			List<BasketItem> items = splitter.splitIntoPieces(unsplitBasketItem);
			assertFalse(unsplitBasketItem.isValid());			
			assertTrue(items.isEmpty());
			assertEquals(2, unsplitBasketItem.getInvalidMessages().size());
			assertEquals("MyJob.label1.viewport missing parameter: label", unsplitBasketItem.getInvalidMessages().get(0));
			assertEquals("MyJob.label1.viewport missing parameter: width", unsplitBasketItem.getInvalidMessages().get(1));		
		} 
		catch (Exception e) {
			e.printStackTrace(System.out);
			fail("Not expecting exception");
		}
	}

}
