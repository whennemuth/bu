package bu.ist.visreg.backstop;

import bu.ist.visreg.basket.BasketItem;

/**
 * This concrete implementation of AbstractBackstopSplitter falls back on the splitting method inherent in BasketItem subclasses.
 * 
 * @author wrh
 *
 */
public class BasicBackstopSplitter extends AbstractBackstopSplitter {

	@Override
	public BasketItem pieceToBasketItem(BasketItem unsplitItem, String json, String pathname) {
		return unsplitItem.getSplitItem(json, pathname);
	}

}
