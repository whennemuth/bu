package edu.bu.ist.apps.kualiautomation.services.automate.locate.label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;

/**
 * The results of any single search attempt are in the form of a list of WebElement instances, or
 * a "batch". This class handles a group of these as another list or "batches". It finds the
 * smallest batch and any of the same size and returns their combined content in a new list. 
 * @param batches
 * @return
 */
class LabelledElementBatches {
	
	private List<Batch> batches = new ArrayList<Batch>();
	
	public void add(Element label, List<WebElement> batch) {
		if(batch == null || batch.isEmpty())
			return;
		batches.add(new Batch(label, batch));
	}
	
	public void add(Batch batch) {
		batches.add(batch);
	}
	
	public void clear() {
		batches.clear(); 
	}
	
	/**
	 * Return the batches, but sort them so that the batch labels with the least web elements are at the top
	 * @return
	 */
	public List<Batch> getBatches() {
		//  Returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
		Collections.sort(batches, new Comparator<Batch>(){
			@Override public int compare(Batch batch1, Batch batch2) {
				if(batch1.getBatch().size() < batch2.getBatch().size())
					return -1;
				if(batch1.getBatch().size() > batch2.getBatch().size())
					return 1;
				return 0;
			}});
		return batches;
	}
	
	/**
	 * @return True if one label-based search returned a single match when all the rest returned more.
	 */
	public boolean singleOneToOneMatch() {
		int count = 0;
		for(Batch batch : batches) {
			if(batch.size() == 1)
				count++;
		}
		return count == 1;
	}
	
	/**
	 * @return True if one or more label searches returned a single result.
	 */
	public boolean containsOneToOneMatch() {
		for(Batch batch : batches) {
			if(batch.size() == 1)
				return true;
		}
		return false;
	}
	
	/**
	 * @return True if one or more label searches returned one or more results.
	 */
	public boolean containsAnyMatch() {
		for(Batch batch : batches) {
			if(!batch.isEmpty())
				return true;
		}
		return false;
	}
	
	/**
	 * Load into a list those WebElement instances that are members of one or more batches that 
	 * are tied for smallest in size.Flattens the smallest batches into a single list.
	 */
	public void loadSmallestResultBatches(List<WebElement> list) {		
		list.addAll(getSmallestResultBatches());
	}
	
	/**
	 * Load into a list those WebElement instances that are the only search result for their
	 * corresponding label WebElement instance.
	 * @param list
	 */
	public void loadOneToOneResultBatches(List<WebElement> list) {
		for(Batch batch : batches) {
			if(batch.size() == 1)
				list.addAll(batch.getBatch());
		}
	}

	public void loadAllResultBatches(List<WebElement> list) {
		for(Batch batch : batches) {			
			list.addAll(batch.getBatch());
		}		
	}
	
	/**
	 * Get a list of those WebElement instances that are members of one or more batches that 
	 * are tied for smallest in size. Flattens the smallest batches into a single list.
	 */
	public List<WebElement> getSmallestResultBatches() {
		
		List<WebElement> list = new ArrayList<WebElement>();
		
		// Sort the results so that those that returned the least results are at the top.
		Collections.sort(batches, new Comparator<Batch>(){
			@Override public int compare(Batch o1, Batch o2) {
				return new Integer(o1.size()).compareTo(new Integer(o2.size()));
			}});
		
		// Add the the result or results of smallest size.
		for (ListIterator<Batch> iterator = batches.listIterator(); iterator.hasNext();) {
			if(iterator.hasPrevious()) {
				 List<WebElement> previous = batches.get(iterator.previousIndex()).getBatch();
				 List<WebElement> flds = (List<WebElement>) iterator.next().getBatch();
				 if(flds.size() > previous.size() && !previous.isEmpty()) {
					 break;
				 }
				 list.addAll(flds);
			}
			else {
				list.addAll((List<WebElement>) iterator.next().getBatch());				
			}
		}
		
		return list;
	}
	
	/**
	 * Convenience bean for representing a "batch" of label-based search results (list of WebElement instances).
	 * @author wrh
	 *
	 */
	public static class Batch implements Comparable {
		private Element label;
		private List<WebElement> batch;
		public Batch(Element label, List<WebElement> batch) {
			this.label = label;
			this.batch = batch;
		}
		public Element getLabel() {
			return label;
		}
		public List<WebElement> getBatch() {
			return batch;
		}
		public int size() {
			if(batch == null || batch.isEmpty())
				return 0;
			return batch.size();
		}
		public boolean isEmpty() {
			if(batch == null)
				return true;
			return batch.isEmpty();
		}
		public boolean isOneToOneMatch() {
			return size() == 1;
		}
		@Override
		public int compareTo(Object o) {
			// TODO Auto-generated method stub
			return 0;
		}
	}
}