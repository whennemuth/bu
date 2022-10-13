package edu.bu.ist.apps.kualiautomation.services.automate.table;

import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.element.AbstractWebElement;
import edu.bu.ist.apps.kualiautomation.util.Utils;

/**
 * This class represents a single table cell (td or th). It gathers information that is used to compare other
 * table cells to it to determine if those cells are in the same table row and what their relative position to
 * one another is. With this information, if having located multiple WebElement candidates in a search within 
 * the table, one can determine which one a label applies to (it would be the one closest to the right of the 
 * label).
 *  
 * @author wrh
 *
 */
public class TableCellData {
	
	private String id;
	private int dataId;
	private int rowIndex;
	private int columnIndex;
	private int depth;
	private TableCellData ancestorCell;
	private TableCellData descendentCell;
	private WebElement childWebElement;
	private WebElement webElement;
	private WebElement tableWebElement;
	private String tag;
	private String tableTag;
	private String first100Chars;
	private String childHTML;

	private int tableRows;
	private int tableColumns;
	private String javascript;
	private JavascriptExecutor executor;
		
	private TableCellData() { /* Restrict default constructor */ }
	
	/**
	 * Invoke a javascript function to return a Map containing data about each table cell that wraps childWebElement.
	 * There will be more than one member to this map if there is a nested table hierarchy involved.
	 * 
	 * @param dataId
	 * @param labelCell
	 * @param searchContext
	 * @param childWebElement
	 * @param JavascriptResourceURL
	 * @return
	 */
	public static TableCellData getInstance(
			int dataId,
			TableCellData labelCell,
			SearchContext searchContext, 
			WebElement childWebElement, 
			String JavascriptResourceURL) {
		
		TableCellData cell = new TableCellData();
		
		cell.dataId = dataId;
		cell.childWebElement = childWebElement;		
		cell.javascript = Utils.getClassPathResourceContent(JavascriptResourceURL);	
		cell.executor = (JavascriptExecutor) searchContext;

// WHY IS IT NOW TAKING SO LONG TO MAKE ONE SIMPLE JAVASCRIPT CALL? (3 SECONDS)
//		WebElement test = searchContext.findElement(By.xpath("//a[@title='return valueAddress Book Id=9034 ']"));
//		String childHTML = (String) cell.executor.executeScript(
//				"return arguments[0].outerHTML;", test);
//		System.out.println(childHTML);
		
		cell.childHTML = (String) cell.executor.executeScript(
				"return arguments[0].outerHTML;", 
				AbstractWebElement.unwrap(childWebElement));
		
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> cellsinfo = (List<Map<String, Object>>) cell.executor.executeScript(
				cell.javascript, 
				"cell", 
				AbstractWebElement.unwrap(childWebElement));
		
		if(cellsinfo == null || cellsinfo.isEmpty()) {
			return null; // The childWebElement was not found to have a table anywhere along its element ancestry.
		}
		else {
			cell.buildHierarchy(labelCell, cellsinfo);
			return cell;
		}
	}

	/**
	 * Build the TableCellData instance out of the data in cellsinfo.
	 * 
	 * @param ancestorCell The next cell to build further up the table hierarchy. 
	 * NOTE: a "descendent" is not necessarily a descendent of an ancestor, but will be a descendent of 
	 * the row of that ancestor.
	 * @param cellsinfo A list of meta-data maps output from a javascript function that describe table cells.
	 */
	private void buildHierarchy(TableCellData ancestorCell, List<Map<String, Object>> cellsinfo) {
		if(!cellsinfo.isEmpty()) {
			Map<String, Object> cellinfo = cellsinfo.get(0);
			List<Map<String, Object>> remaining = cellsinfo.subList(1, cellsinfo.size());
			this.id = String.valueOf(cellinfo.get("id"));
			this.columnIndex = Integer.valueOf(cellinfo.get("x").toString());
			this.rowIndex = Integer.valueOf(cellinfo.get("y").toString());
			this.depth = Integer.valueOf(cellinfo.get("depth").toString());
			this.webElement = (WebElement) cellinfo.get("cell");
			this.tableWebElement = (WebElement) cellinfo.get("table");
			this.tableRows = Integer.valueOf(cellinfo.get("tableRows").toString());
			this.tableColumns = Integer.valueOf(cellinfo.get("tableCols").toString());
			this.tag = String.valueOf(cellinfo.get("tag"));
			this.first100Chars = String.valueOf(cellinfo.get("first100Chars"));
			
			if(ancestorCell != null) {
				this.ancestorCell = ancestorCell;
				ancestorCell.descendentCell = this;
			}
			if(!remaining.isEmpty()) {
				TableCellData cell = new TableCellData();
				cell.dataId = dataId;
				cell.childWebElement = childWebElement;
				cell.buildHierarchy(this, remaining);
			}
		}		
	}

	public TableCellData getHighestAncestorCell() {
		return getHighestAncestorCell(this);
	}

	private TableCellData getHighestAncestorCell(TableCellData cell) {
		if(cell.getAncestorCell() == null)
			return cell;
		return getHighestAncestorCell(cell.getAncestorCell());
	}

	public int getDataId() {
		return dataId;
	}

	/**
	 * @return The WebElement inside the td or th
	 */
	public WebElement getWebChildElement() {
		return childWebElement;
	}

	public boolean hasAncestorCell() {
		return ancestorCell != null;
	}
	
	public TableCellData getAncestorCell() {
		return ancestorCell;
	}

	public boolean hasDescendentCell() {
		return descendentCell != null;
	}

	public int getRowIndex() {
		return rowIndex;
	}

	public int getColumnIndex() {
		return columnIndex;
	}

	public int getIndexInSharedRow(TableCellData deeperCell) {

		WebElement parentCell = AbstractWebElement.wrap((WebElement) executor.executeScript(
				javascript, 
				"ancestorcell", 
				AbstractWebElement.unwrap(deeperCell.webElement), 
				depth));
		
		if(parentCell == null) {
			return deeperCell.columnIndex;
		}
		return Integer.valueOf(parentCell.getAttribute("columnIndex"));
	}
	
	public int getDepth() {
		return depth;
	}

	/**
	 * @return The WebElement for the td or th node
	 */
	public WebElement getWebElement() {
		return webElement;
	}

	/**
	 * @return The WebElement of the table that contains the td or th node
	 */
	public WebElement getTableWebElement() {
		return tableWebElement;
	}

	public int getTableRows() {
		return tableRows;
	}

	public int getTableColumns() {
		return tableColumns;
	}

	public String getTag() {
		return tag;
	}

	public String getTableTag() {
		return tableTag;
	}

	public String getFirst100Chars() {
		return first100Chars;
	}

	public boolean sameRowAndTableSize(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TableCellData))
			return false;
		TableCellData other = (TableCellData) obj;
		if (rowIndex != other.rowIndex)
			return false;
		if (tableColumns != other.tableColumns)
			return false;
		if (tableRows != other.tableRows)
			return false;
		return true;
	}
	
	/**
	 * 2 Instances will be equivalent if they both occupy the same row and column in a table of the same
	 * column size and row size. These may yet be two different tables, so equivalence may not mean equality.
	 * @param obj
	 * @return
	 */
	public boolean isEquivalent(Object obj) {
		if(!sameRowAndTableSize(obj)) 
			return false;
		TableCellData other = (TableCellData) obj;
		if(columnIndex != other.columnIndex)
			return false;
		return true;
	}
	
	public static boolean areAllEquivalent(List<TableCellData> cells) {
		if(cells.isEmpty())
			return false;
		if(cells.size() == 1)
			return true;
		TableCellData first = cells.get(0);
		for(TableCellData cell : cells) {
			if(!cell.isEquivalent(first)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return getString(this, 0);
	}
		
	private String getString(TableCellData cell, int tabs) {
		StringBuilder indent = new StringBuilder();
		for(int i=0; i<tabs; i++) {
			indent.append("    ");
		}
		StringBuilder builder = new StringBuilder();
		if(cell == null) {
			builder.append("null");
		}
		else {
			String ancestor = cell.ancestorCell == null ? "null" : "{dataId:" + String.valueOf(cell.ancestorCell.dataId) + "}";
			builder.append("TableCellData [\n")
					.append(indent.toString()).append("id=").append(cell.id).append(", \n")
					.append(indent.toString()).append("tag=").append(cell.tag).append(", \n")
					.append(indent.toString()).append("first100Chars=").append(cell.first100Chars).append(", \n")
					.append(indent.toString()).append("childHTML=").append(cell.childHTML).append(", \n")
					.append(indent.toString()).append("tableTag=").append(cell.tableTag).append(", \n")
					.append(indent.toString()).append("dataId=").append(cell.dataId).append(", \n")
					.append(indent.toString()).append("rowIndex=").append(cell.rowIndex).append(", \n")
					.append(indent.toString()).append("columnIndex=").append(cell.columnIndex).append(", \n")
					.append(indent.toString()).append("depth=").append(cell.depth).append(", \n")
					.append(indent.toString()).append("ancestorCell=").append(ancestor).append(", \n")
					.append(indent.toString()).append("descendentCell=").append(getString(cell.descendentCell, ++tabs)).append(", \n")
					.append(indent.toString()).append("childWebElement=").append(cell.childWebElement).append(", \n")
					.append(indent.toString()).append("webElement=").append(cell.webElement).append(", \n")
					.append(indent.toString()).append("tableWebElement=").append(cell.tableWebElement).append(", \n")
					.append(indent.toString()).append("tableRows=").append(cell.tableRows).append(", \n")
					.append(indent.toString()).append("tableColumns=").append(cell.tableColumns).append("]");
		}
		return builder.toString();
		
	}
}
