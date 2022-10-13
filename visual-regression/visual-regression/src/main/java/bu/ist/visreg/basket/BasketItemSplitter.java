package bu.ist.visreg.basket;

import java.util.List;

/**
 * A BasketItem can be split into multiple smaller BasketItem instances.
 * This interface specifies two methods:
 *   - 1) One to implement functionality to "split" a BasketItem instance.
 *   - 2) One to form a new BasketItem instance out of any "split-off" content.
 * 
 * @author wrh
 *
 */
public interface BasketItemSplitter {
	
	public abstract List<BasketItem> splitIntoPieces(BasketItem unsplitItem) throws Exception;

	public abstract BasketItem pieceToBasketItem(BasketItem unsplitItem, String json, String pathname);

}
