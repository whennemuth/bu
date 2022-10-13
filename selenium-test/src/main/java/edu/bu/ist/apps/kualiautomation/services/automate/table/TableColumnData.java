package edu.bu.ist.apps.kualiautomation.services.automate.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.element.AbstractWebElement;
import edu.bu.ist.apps.kualiautomation.util.Utils;

/**
 * This class computes the closest among a list of choices those web elements that are closest to a specified 
 * label web element with respect to their vertical proximity to that web element as members of a table.
 * Typically, the label web element would be a table column header.
 * <pre style="font: inherit">
 * 
 * 1) Assume the label web element exists in a table. 
 * 2) Provide functionlity to pick from webElements only items that exist somewhere in the same table column. 
 * 3) Provide additional functionality to pick the web element closest to (highest) to the label.
 * </pre>
 * @author wrh
 *
 */
public class TableColumnData {
	
	private WebElement labelElement;
	private List<WebElement> webElements;
	private String javascript;
	private JavascriptExecutor executor;
	
	public TableColumnData(WebDriver driver, WebElement labelElement, List<WebElement> webElements, String javascriptResourceURL) {
		this.labelElement = labelElement;
		this.webElements = webElements;
		this.javascript = Utils.getClassPathResourceContent(javascriptResourceURL);
		this.executor = (JavascriptExecutor) driver;
	}

	@SuppressWarnings("unchecked")
	public List<TableColumnCell> getElementsInSameColumnAsLabel() {
		
		List<Map<String, Object>> datamaps = (List<Map<String, Object>>) executor.executeScript(
				javascript, 
				"column", 
				AbstractWebElement.unwrap(labelElement),
				AbstractWebElement.unwrap(webElements));
		
		List<TableColumnCell> filtered = TableColumnCell.getInstances(datamaps);
		
		Collections.sort(filtered);
		
		return filtered;
	}
	
	/**
	 * @return The web elements closest to the label web element in the same table column.
	 * If the returned list has multiple items then each of them have the same parent table cell.
	 */
	public List<WebElement> getFirstElementsBelowLabelInSameColumn() {
		List<TableColumnCell> cells = getElementsInSameColumnAsLabel();
		List<TableColumnCell> filtered = new ArrayList<TableColumnCell>();
		for (TableColumnCell cell : cells) {
			if(filtered.isEmpty()) {
				filtered.add(cell);
				continue;
			}
			if(cell.compareTo(filtered.get(0)) != 0) {
				break;
			}
		}
		
		List<WebElement> results = new ArrayList<WebElement>();
		for(TableColumnCell cell : filtered) {
			results.add(cell.getOriginalField());
		}
		return results;
	}
	
	/**
	 * This class is a bean to take in meta data about the table cells returned by a javascript query.
	 * 
	 * @author wrh
	 *
	 */
	public static class TableColumnCell implements Comparable<Object> {

		private WebElement originalField;
		private Integer rowIndex;
		private Integer commonColumnIndex;
		private Integer labelColumnIndex;
		private Integer labelRowIndex;
		private WebElement labelLevelCell;
		private Integer labelLevelRowIndex;
		private Integer labelLevelColumnIndex;
		
		public TableColumnCell(Map<String, Object> data) {
			originalField = (WebElement) data.get("originalField");
			commonColumnIndex = getInteger("commonColumnIndex", data);
			rowIndex = getInteger("rowIndex", data);
			labelColumnIndex = getInteger("labelColumnIndex", data);
			labelRowIndex = getInteger("labelRowIndex", data);
			labelLevelCell = (WebElement) data.get("labelLevelCell");
			labelLevelColumnIndex = getInteger("labelLevelColumnIndex", data);
			labelLevelRowIndex = getInteger("labelLevelRowIndex", data);
		}
		
		public WebElement getOriginalField() {
			return originalField;
		}
		public Integer getRowIndex() {
			return rowIndex;
		}
		public Integer getCommonColumnIndex() {
			return commonColumnIndex;
		}
		public Integer getLabelColumnIndex() {
			return labelColumnIndex;
		}
		public Integer getLabelRowIndex() {
			return labelRowIndex;
		}
		public WebElement getLabelLevelCell() {
			return labelLevelCell;
		}
		public Integer getLabelLevelRowIndex() {
			return labelLevelRowIndex;
		}
		public Integer getLabelLevelColumnIndex() {
			return labelLevelColumnIndex;
		}
		private Integer getInteger(String fieldname, Map<String, Object> data) {
			Object val = data.get(fieldname);
			if(val == null)
				return null;
			Integer i = null;
			try {
				i = Integer.valueOf(val.toString());
			} 
			catch (NumberFormatException e) {
				return null;
			}
			return i;
		}
		
		public static List<TableColumnCell> getInstances(List<Map<String, Object>> datamaps) {
			List<TableColumnCell> cells = new ArrayList<TableColumnCell>();
			if(datamaps != null) {
				for(Map<String, Object> map : datamaps) {
					cells.add(new TableColumnCell(map));
				}
			}
			return cells;
		}

		public boolean inSameRowAs(TableColumnCell othercell) {
			return this.getRowIndex() == othercell.getRowIndex();
		}		
		public boolean inHigherRowThan(TableColumnCell othercell) {
			return this.getRowIndex() < othercell.getRowIndex();
		}
		public boolean inHigherLabelLevelRowThan(TableColumnCell othercell) {
			return this.getLabelLevelRowIndex() < othercell.getLabelLevelRowIndex();
		}		
		public boolean inLabelLevelColumnToTheLeftOf(TableColumnCell othercell) {
			return this.getCommonColumnIndex() < othercell.getCommonColumnIndex();
		}
		public boolean isBelowLabel() {
			return this.getLabelLevelRowIndex() > this.getLabelRowIndex();
		}
		public boolean isAboveLabel() {
			return !isBelowLabel();
		}
		
		@Override
		public int compareTo(Object other) {
			// Returns a negative integer, zero, or a positive integer as 
			// the first argument is less than, equal to, or greater than the second.
			if(other == null || other instanceof TableColumnCell == false) {
				return -1;
			}
			TableColumnCell othercell = (TableColumnCell) other;
			
			// Sometimes the row the label is in is not the top row, so check for this first.
			if(this.isBelowLabel() && othercell.isAboveLabel()) {
				return -1;
			}
			if(othercell.isBelowLabel() && this.isAboveLabel()) {
				return 1;
			}
			
			// The label should be in the top row of the table	
			if(this.inHigherLabelLevelRowThan(othercell)) {
				return -1;
			}
			if(othercell.inHigherLabelLevelRowThan(this)) {
				return 1;
			}
			if(this.inLabelLevelColumnToTheLeftOf(othercell)) {
				return -1;
			}
			if(othercell.inLabelLevelColumnToTheLeftOf(this)) {
				return 1;
			}
			if(this.inSameRowAs(othercell)) {
				return 0;
			}
			return this.inHigherRowThan(othercell) ? -1 : 1;
		}
	};
}
