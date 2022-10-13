package bu.ist.visreg.basket.s3;

import java.util.Map;

import bu.ist.visreg.basket.Basket;
import bu.ist.visreg.basket.BasketItemSplitter;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3Basket extends Basket {

	private String bucketPath;
	private S3Object subfolder;
	private S3Bucket bucket;
	
	public S3Basket(BasketEnum basketEnum, S3BasketSystem parent) {
		super.basketEnum = basketEnum;
		super.parent = parent;
		this.bucket = parent.getBucket();
	}

	@Override
	public void createIfNotExists() throws Exception {
		getIdentifier();
		
		subfolder = bucket.getSubFolder(basketEnum.getBasketRelativeLocation());
		if(subfolder == null) {
			bucket.createSubfolder(basketEnum.getBasketRelativeLocation());
			subfolder = bucket.getSubFolder(basketEnum.getBasketRelativeLocation());
		}
	}

	@Override
	public void load(BasketItemSplitter splitter) throws Exception {
		Map<String, S3Object> map = bucket.getS3Objects();
		for(S3Object o : map.values()) {
			if(this.contains(o)) {
				String content = bucket.downloadAsString(o.key());				
				loadOneBasketItem(
						new S3BasketItem(this, o.key(), content), 
						splitter);				
			}
		}
	}

	@Override
	public String getIdentifier() {
		if(subfolder != null) {
			return subfolder.key();
		}
		if(bucketPath == null) {
			bucketPath = parent.getRootLocation() + "/" + super.basketEnum.getBasketRelativeLocation();
		}
		return bucketPath;
	}
	
	public S3Bucket getBucket() {
		return bucket;
	}

	/**
	 * This basket, itself a subfolder within an S3 bucket, contains the provided s3 object if that object
	 * starts with the same s3 path.
	 * @param o
	 * @return
	 */
	private boolean contains(S3Object o) {
		if( ! o.key().startsWith(subfolder.key())) return false;
		if( ! (o.key().length() > subfolder.key().length())) return false;
		return true;
	}

}
