package bu.ist.visreg.basket.s3;

import bu.ist.visreg.backstop.BasicBackstopSplitter;
import bu.ist.visreg.basket.Basket;
import bu.ist.visreg.basket.Basket.BasketEnum;
import bu.ist.visreg.basket.BasketItemSplitter;
import bu.ist.visreg.basket.BasketSystem;

/**
 * Processing of visual regression is to be tracked and sourced from an AWS S3 bucket.
 * All jobs within their baskets are stored as files within "subdirectories" of the s3 bucket.
 * 
 * Examples: https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/examples-s3-objects.html
 * 
 * @author wrh
 *
 */
public class S3BasketSystem extends BasketSystem {
	private S3Bucket bucket;
	
	public S3BasketSystem(S3Bucket bucket) throws Exception {
		this.bucket = bucket;
		super.rootLocation = bucket.getBucketName();
	}
	
	@Override
	public void load(BasketItemSplitter splitter) throws Exception {		
		
		for(Basket.BasketEnum be : BasketEnum.values()) {
			
			S3Basket basket = new S3Basket(be, this);
			
			basket.createIfNotExists();
			
			basket.load(splitter);
			
			addBasket(basket);
		}		
	}
	
	public S3Bucket getBucket() {
		return bucket;
	}    

	public static void main(String[] args) throws Exception {    	
    	S3Bucket bucket = S3Bucket.parseArgs(args);    	
    	S3BasketSystem bs = new S3BasketSystem(bucket);
    	bs.load(new BasicBackstopSplitter());
    	System.out.println(bs);
    }
}
