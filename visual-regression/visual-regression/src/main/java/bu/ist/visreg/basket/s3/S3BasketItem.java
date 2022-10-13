package bu.ist.visreg.basket.s3;

import bu.ist.visreg.basket.Basket;
import bu.ist.visreg.basket.BasketItem;

public class S3BasketItem extends BasketItem {

	private S3Bucket bucket;

	public S3BasketItem(Basket basket, String pathname, String content) {
		super(basket, pathname, content);
		this.bucket = ((S3Basket) basket).getBucket();
	}

	@Override
	public void commitBasketMove(Basket nextBasket) throws Exception {
		super.pathname = bucket.moveObject(super.pathname, nextBasket.getIdentifier());
	}

	@Override
	public boolean persist() throws Exception {
		try {
			bucket.uploadFileFromString(getPathname(), getContent());
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean delete() {
		try {
			bucket.deleteObject(pathname);
		} 
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public BasketItem getSplitItem(String json, String id) {
		if(id.equals(pathname)) {
			// The id IS the existing pathname. So, just change the content of this basket item to json and return it.
			return new S3BasketItem(basket, pathname, json);
		}
		else {
			// The id designates a portion of a new pathname to be based on the existing one.
			// Build the new pathname and return a corresponding basket item with json as the content.
			String newPathName = getExtendedPathname(id);
			S3BasketItem item = new S3BasketItem(basket, newPathName, json);
			return item;
		}
	}

}
