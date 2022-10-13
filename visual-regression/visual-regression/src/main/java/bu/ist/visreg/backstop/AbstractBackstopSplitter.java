package bu.ist.visreg.backstop;

import java.util.ArrayList;
import java.util.List;

import bu.ist.visreg.basket.BasketItem;
import bu.ist.visreg.basket.BasketItemSplitter;
import bu.ist.visreg.job.JobDefinition;

/**
 * A JobDefinition will contain multiple scenarios. It is close to a BackstopJson instance.
 * The JobDefinition can be converted into a BackstopJson instance, itself having those multiple scenarios.
 * However, if you want just one scenario per BackstopJson instance, one for each of the collection, then a class that 
 * "splits" up the collection of scenarios so multiple BackstopJson instances can be built is required.
 *   
 * @author wrh
 *
 */
public abstract class AbstractBackstopSplitter implements BasketItemSplitter {
	
	private List<BackstopJson> backstops = new ArrayList<BackstopJson>();
	private List<BackstopJson> invalidBackstops = new ArrayList<BackstopJson>();
	
	@Override
	public List<BasketItem> splitIntoPieces(BasketItem unsplitItem) throws Exception {
		
		backstops.clear();
		invalidBackstops.clear();
		JobDefinition def = JobDefinition.getInstance(unsplitItem.getContent());			
		backstops.addAll(def.getBackstops());
		invalidBackstops.addAll(def.getInvalidBackstops());
		
		List<BasketItem> basketItems = new ArrayList<BasketItem>(backstops.size());
		if(noneAreValid() && noneAreInvalid()) {
			return basketItems;
		}

		if(someAreValid()) {
			if(backstops.size() == 1) {
				// Replace the content of the JobDefinition file with what it should be after converted to a single backtopJson file.
				String json = backstops.get(0).toJson();
				BasketItem item = pieceToBasketItem(unsplitItem, json, unsplitItem.getPathname());
				// item.persist();
				basketItems.add(item);
			}
			else {
				// Break the JobDefinition into a collection of backstopJson files (one for each scenario).
				for(BackstopJson bs : backstops) {
					String newId = bs.getId() + "_" + bs.scenarios.get(0).getLabel();
					BasketItem item = pieceToBasketItem(unsplitItem, bs.toJson(), newId);
					// item.persist();
					basketItems.add(item);
				}
			}
		}
		
		if(someAreInvalid()) {
			for(BackstopJson invalid : invalidBackstops) {
				unsplitItem.addInvalidMessages(invalid.getInvalidMessages());
			}
		}
		return basketItems;
	}

	private boolean noneAreInvalid() {
		return invalidBackstops.isEmpty();
	}
	private boolean someAreInvalid() {
		return ! noneAreInvalid();
	}
	private boolean someAreValid() {
		return ! backstops.isEmpty();
	}
	private boolean noneAreValid() {
		return ! someAreValid();
	}
}
