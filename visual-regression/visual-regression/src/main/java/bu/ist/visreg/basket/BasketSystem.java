package bu.ist.visreg.basket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bu.ist.visreg.basket.Basket.BasketEnum;
import bu.ist.visreg.basket.filesystem.FileBasketSystem;
import bu.ist.visreg.basket.s3.S3BasketSystem;
import bu.ist.visreg.basket.s3.S3Bucket;

public abstract class BasketSystem {

	protected List<Basket> baskets = new ArrayList<Basket>();
	protected String rootLocation;
	
	public abstract void load(BasketItemSplitter splitter) throws Exception;
	
	public static BasketSystem getInstance(BasketType basketType, String rootLocation, BasketItemSplitter splitter) throws Exception {
		BasketSystem bs = null;
		switch(basketType) {
			case FILESYSTEM:
				bs = new FileBasketSystem(rootLocation);
				break;
			case S3:
				S3Bucket bucket = new S3Bucket(rootLocation, true);
				bs = new S3BasketSystem(bucket);
				break;
		}
				
		bs.load(splitter);
		
		return bs;
	}
	
	public Basket getBasket(BasketEnum be) {
		for (Basket basket : baskets) {
			if(basket.getEnum().equals(be)) {
				return basket;
			}
		}
		return null;
	}
	
	public String getRootLocation() {
		return rootLocation;
	}
	
	public List<Basket> getBaskets() {
		return Collections.unmodifiableList(baskets);
	}
	
	public void addBasket(Basket basket) {
		baskets.add(basket);
	};
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rootLocation == null) ? 0 : rootLocation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof BasketSystem))
			return false;
		BasketSystem other = (BasketSystem) obj;
		if (rootLocation == null) {
			if (other.rootLocation != null)
				return false;
		} else if (!rootLocation.equals(other.rootLocation))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("BasketSystem [\n");
		s.append("  rootLocation=").append(rootLocation).append("\n")
		 .append("  baskets=[\n");
		for(Basket b : baskets) {
			s.append("    ").append(b.toString().replaceAll("\\n", "\n    ")).append("\n");
		}
		s.append("  ]\n").append("]");
		return s.toString();
	}



	public static enum BasketType {
		S3, FILESYSTEM;
		public static BasketType getValue(String val) {
			for(BasketType location : BasketType.values()) {
				if(val == null) return null;
				if(val.equalsIgnoreCase(location.name())) {
					return location;
				}
			}
			return null;
		}
		public static String concatenatedList() {
			StringBuilder s = new StringBuilder();
			for(BasketType name: BasketType.values()) {
				if(s.length() > 0) {
					s.append(", ");
				}
				s.append(name);
			}
			return s.toString();
		}
	}
}
