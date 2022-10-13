package bu.ist.visreg;

import static org.mockito.Mockito.when;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import bu.ist.visreg.backstop.AbstractBackstopSplitter;
import bu.ist.visreg.basket.BasketItem;
import bu.ist.visreg.basket.BasketItemSplitter;

/**
 * A BasketItem can be split into multiple smaller BasketItem instances.
 * This class produces a such a splitter, but one that returns mocks instead of real instances.
 * 
 * @author wrh
 *
 */
public class BackstopSplitterMocker {

	public static BasketItemSplitter getInstance() {
		return new AbstractBackstopSplitter() {
			@Override public BasketItem pieceToBasketItem(BasketItem bi, String json, String pathname) {
				BasketItem splitItem = bi.getSplitItem(json, pathname);
				BasketItem splitItemMock = Mockito.mock(BasketItem.class);
				when(splitItemMock.getContent()).thenReturn(splitItem.getContent());
				when(splitItemMock.getPathname()).thenReturn(splitItem.getPathname());
				when(splitItemMock.getBasket()).thenReturn(splitItem.getBasket());
				when(splitItemMock.delete()).thenAnswer(new Answer<BasketItem>() {
					@Override public BasketItem answer(InvocationOnMock invocation) throws Throwable {
						BasketItem bi = (BasketItem) invocation.getMock();
						System.out.println("Deleting: " + bi.getPathname());
						return null;
					}
					
				}).thenReturn(true);
				try {
					when(splitItemMock.persist()).thenAnswer(new Answer<BasketItem>() {
						@Override public BasketItem answer(InvocationOnMock invocation) throws Throwable {
							BasketItem bi = (BasketItem) invocation.getMock();
							System.out.println("Persisting: " + bi.getPathname());
							return null;
						}
						
					}).thenReturn(true);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				return splitItemMock;
			}				
		};
	}
}
