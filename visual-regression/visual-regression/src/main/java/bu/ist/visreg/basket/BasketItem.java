package bu.ist.visreg.basket;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bu.ist.visreg.basket.Basket.BasketEnum;

public abstract class BasketItem {

	protected boolean failed;
	protected List<String> invalidMessages = new ArrayList<String>();
	protected Basket basket;
	protected String pathname;
	protected String content;
	protected Pattern extensionPattern = Pattern.compile("\\.[^\\.]+$");
	
	
	public BasketItem(Basket basket, String pathname, String content) {
		this.basket = basket;
		this.pathname = pathname;
		this.content = content;
	}
	public void addInvalidMessage(String msg) {
		invalidMessages.add(msg);
	}
	public void addInvalidMessages(List<String> msgs) {
		invalidMessages.addAll(msgs);
	}
	public List<String> getInvalidMessages() {
		return invalidMessages;
	}
	public boolean isValid() {
		return invalidMessages.isEmpty();
	}
	public boolean failed() {
		return failed;
	}
	public void setFailed(boolean failed) {
		this.failed = failed;
	}
	public Basket getBasket() {
		return basket;
	}
	public void setBasket(Basket basket) {
		this.basket = basket;
	}
	public String getPathname() {
		return pathname;
	}
	public String getContent() {
		return content;
	}

	public boolean inProcessingBasket() {
		return basket.getEnum().equals(BasketEnum.IN_PROCESS);
	}
	
	public boolean inInboxBasket() {
		return basket.getEnum().equals(BasketEnum.INBOX);
	}
	
	protected String getExtendedPathname(String id) {
		String newPathName = null;
		Matcher m = extensionPattern.matcher(pathname);
		if(m.find()) {
			String fileExtension = m.toMatchResult().group();
			if(fileExtension != null && !fileExtension.isEmpty()) {
				String basename = pathname.substring(0, pathname.length()-fileExtension.length());
				newPathName = basename + "_" + id + fileExtension;
			}
		}
		else {
			newPathName = pathname + "_" + id;
		}
		return newPathName;
	}
	
	/**
	 * A Basket item undergoes a process or "journey" through a series of baskets.
	 * It moves from one basket to the next. This function moves a basket item from its current basket into the next.
	 * @return
	 * @throws Exception
	 */
	public boolean gotoNextBasket() throws Exception {
		Basket nextBasket = basket.getNextBasket(this);
		return gotoNextBasket(nextBasket);
	}
	
	public boolean gotoErrorBasket() throws Exception {
		Basket errorBasket = basket.getBasketSystem().getBasket(BasketEnum.ERROR);
		System.out.println("ERROR - Basket item: " + getPathname() + " is invalid. Moving to error basket.");
		return gotoNextBasket(errorBasket);
	}
	
	public boolean gotoNextBasket(Basket nextBasket) throws Exception {
		if(nextBasket != null && ! nextBasket.equals(basket)) {			
			basket.removeBasketItem(this);			
			nextBasket.addBasketItem(this);
			this.basket = nextBasket;
			
			try {
				commitBasketMove(nextBasket);
			} 
			catch (Exception e) {
				System.err.println(String.format(
						"ERROR! Failed to move %s to %s",
						pathname,
						nextBasket.getIdentifier()));
				throw e;
			}
			return true;
		}
		return false;		
	}
		
	/**
	 * The basket system has been updated to reflect items moving between baskets at this point, 
	 * but it is just a representation of the real thing, an underlying storage system (ie: files, 
	 * cloud storage, etc). So, move the actual item.
	 */	
	public abstract void commitBasketMove(Basket nextBasket) throws Exception;
	
	public abstract BasketItem getSplitItem(String json, String pathname);
	
	public abstract boolean persist() throws Exception;
	
	public abstract boolean delete();
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((basket == null) ? 0 : basket.hashCode());
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + ((pathname == null) ? 0 : pathname.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof BasketItem))
			return false;
		BasketItem other = (BasketItem) obj;
		if (basket == null) {
			if (other.basket != null)
				return false;
		} else if (!basket.equals(other.basket))
			return false;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (pathname == null) {
			if (other.pathname != null)
				return false;
		} else if (!pathname.equals(other.pathname))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "BasketItem [pathname=" + pathname + ", failed=" + failed + "]";
	}
	
}
