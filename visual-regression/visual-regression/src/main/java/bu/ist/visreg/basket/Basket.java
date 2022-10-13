package bu.ist.visreg.basket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class Basket {
	
	protected BasketSystem parent;
	protected BasketEnum basketEnum;
	protected List<BasketItem> basketItems = new LinkedList<BasketItem>();
	protected int rejectedBasketItems;

	public abstract void createIfNotExists() throws Exception;
	
	public abstract void load(BasketItemSplitter splitter) throws Exception;
	
	public abstract String getIdentifier();

	public Basket getNextBasket(BasketItem item) {
		return parent.getBasket(getEnum().getNextBasket(item));
	}
	
	public BasketEnum getEnum() {
		return basketEnum;
	}
	
	public List<BasketItem> getBasketItems() {
		return Collections.unmodifiableList(basketItems);
	}

	public void addBasketItem(BasketItem basketItem) {
		basketItems.add(basketItem);
	}

	public void removeBasketItem(BasketItem basketItem) {
		basketItems.remove(basketItem);
	}

	public boolean isInbox() {
		return BasketEnum.INBOX.equals(getEnum());
	}
	
	public BasketSystem getBasketSystem() {
		return parent;
	}

	public int getRejectedBasketItems() {
		return rejectedBasketItems;
	}
	
	protected void rejectBasketItem(BasketItem basketItem) throws Exception {
		basketItem.gotoErrorBasket();
		rejectedBasketItems++;
	}
	
	protected void loadOneBasketItem(BasketItem bi, BasketItemSplitter splitter) throws Exception {
		List<BasketItem> subitems = new ArrayList<BasketItem>();
		if(isInbox()) {
			subitems.addAll(splitter.splitIntoPieces(bi));
		}
		
		if(bi.isValid()) {
			for(BasketItem subitem : subitems) {
				subitem.persist();
				addBasketItem(subitem);
			}
			if(subitems.size() > 1) {
				System.out.println("Deleting: " + bi.getPathname());
				bi.delete();
			}
		}
		else {
			rejectBasketItem(bi);
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((basketEnum == null) ? 0 : basketEnum.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Basket))
			return false;
		Basket other = (Basket) obj;
		if (basketEnum != other.basketEnum)
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}



	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("Basket [\n");
		s.append("  basketEnum=").append(basketEnum).append("\n")
		 .append("  basketItems [\n");
		for(BasketItem b : basketItems) {
			s.append("    ").append(b).append("\n");
		}
		s.append("  ]\n").append("]");		
		return s.toString();
	}



	public static enum BasketEnum {
		INBOX("inbox"),
		IN_PROCESS("processing"),
		ERROR("error"),
		COMPLETED("completed");
		
		private String name;
		
		private BasketEnum(String name) {
			this.name = name;
		}

		public String getBasketRelativeLocation() {
			return "jobs/" + name + "/";
		}

		public String getBasketName() {
			return name;
		}
		public BasketEnum getNextBasket(BasketItem item) {
			if(item.failed()) {
				return ERROR;
			}
			if( ! item.isValid()) {
				return ERROR;
			}
			switch(this) {
				case INBOX : return IN_PROCESS;
				case IN_PROCESS: return COMPLETED;
				default: return null;
			}
		}
		public static String concatenatedList() {
			StringBuilder s = new StringBuilder();
			for(BasketEnum name: BasketEnum.values()) {
				if(s.length() > 0) {
					s.append(", ");
				}
				s.append(name);
			}
			return s.toString();
		}
	}
}
