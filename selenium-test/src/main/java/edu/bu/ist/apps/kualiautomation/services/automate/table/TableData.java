package edu.bu.ist.apps.kualiautomation.services.automate.table;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.element.AbstractWebElement;
import edu.bu.ist.apps.kualiautomation.util.Utils;

/**
 * This class represents an html table that is treated as a parent in the hierarchy to all WebElement 
 * intances in a specified list and a web element representing a label. The purpose is to gain more 
 * information about those WebElement instances with respect to their position in the table, or if they 
 * all exist in a common table at all. This information makes it possible to determine for the label  
 * which WebElement it applies to. Two types of searches are attempted in order:
 * 
 * <pre style="font: inherit">
 * 
 *  1) Horizonal: Find web elements in the table closest to the right of the label in the same row.
 *  2) Vertical: Treat the label as if it were the header of a column and trim away web elements that
 *     are not in the same column and keep only those that are closest to the top of the column.
 * </pre>
 *  
 * @author wrh
 *
 */
public class TableData {

	public static final String JAVASCRIPT_URL = "TableCellAncestry.js";
	
	private WebDriver driver;
	/** WebElements within the table */
	private List<WebElement> webElements;
	/** Table cells that contain the WebElements */ 
	private List<TableCellData> hostCells = new ArrayList<TableCellData>();
	private TableCellData labelCell;
	private WebElement thisTable;
	private WebElement sharedRow;
	private int rows;
	private int columns;
	private Integer depth;

	private List<TableCellData> lastResults = new ArrayList<TableCellData>();
	
	/**
	 * This constructor first checks that each webElement in webElements share a common table.
	 * Next a TableCellData list is constructed for each webElement to capture data about the 
	 * container cell of each web element in webElements. 
	 * Finally, data about the table itself is captured (how many columns, rows, depth, etc.)
	 * 
	 * @param driver
	 * @param labelElement
	 * @param webElements
	 */
	public TableData(WebDriver driver, WebElement labelElement, List<WebElement> webElements) {
		
		this.driver = driver;
		this.webElements = webElements;

		createHostCell(labelElement, 0);
		
		for (ListIterator<WebElement> iterator = webElements.listIterator(); iterator.hasNext();) {
			WebElement we = (WebElement) iterator.next();
			createHostCell(we, iterator.nextIndex());
			if(!hostCells.isEmpty()) {
				thisTable = hostCells.get(0).getTableWebElement();
			}
		}
		
		sharedRow = getFirstSharedTableRow();
		if(sharedRow == null) {
			return; // There is no common table to all of webElements. Proceed no further.
		}
		
		thisTable = hostCells.get(0).getTableWebElement();
		String sDepth = String.valueOf(thisTable.getAttribute("depth"));
		if(!Utils.isEmpty(sDepth) && sDepth.matches("\\d+")) {
			depth = Integer.valueOf(sDepth);
		}
		rows = hostCells.get(0).getTableRows();
		columns = hostCells.get(0).getTableColumns();
	}

	/**
	 * Create a TableCellData instance to wrap the provided web element. Add this instance to the hostCells collection.
	 * @param we The web element the created TableCellData instance is to wrap.
	 * @param dataId
	 */
	private void createHostCell(WebElement we, int dataId) {
		TableCellData cell = TableCellData.getInstance(dataId, labelCell, driver, we, JAVASCRIPT_URL);
		if(cell != null) {
			hostCells.add(cell);
			if(labelCell == null && cell.getDataId() == 0) {
				labelCell = cell;
			}
		}
	}
		
	/**
	 * @return All the table cells that are the topmost ancestor of each WebElement are themselves all
	 * in the same row of the same table.
	 */
	public boolean allWebElementsShareSameAncestorsTable() {
		
		if(hostCells.isEmpty())
			return false;
		
		for(TableCellData cell : hostCells) {
			TableCellData topCell = cell.getHighestAncestorCell();
			if(topCell.getDepth() != hostCells.get(0).getDepth())
				return false;
			if(topCell.getTableColumns() != hostCells.get(0).getTableColumns()) 
				return false;
			if(topCell.getTableRows() != hostCells.get(0).getTableRows())
				return false;
			// Try javascript
			if(!bothAreSameWebElement(this.thisTable, topCell.getTableWebElement()))
				return false;
		}
		return true;
	}
	
	/**
	 * Get the first row that each of the web elements have in common with each other.
	 * 
	 * @return A table row element or null if at least one of the web elements does not share the 
	 * same table row with any of the others in its ancestor hierarchy.
	 */
	public WebElement getFirstSharedTableRow() {
		
		if(sharedRow != null)
			return sharedRow;
		
		if(allWebElementsShareSameAncestorsTable()) {
			String javascript = Utils.getClassPathResourceContent(JAVASCRIPT_URL);
			
			JavascriptExecutor executor = (JavascriptExecutor) driver;
			
			WebElement tablerow = AbstractWebElement.wrap((WebElement) executor.executeScript(
					javascript, 
					"row", 
					AbstractWebElement.unwrap(webElements)));
			
			return tablerow;
		}
		return null;
	}
	
	/**
	 * Find those table cells "closest" to the label cell of this instance.
	 * See {@link TableData#getClosestTableCells(TableCellData)} for details.
	 * @return
	 */
	public List<TableCellData> getTableCellsClosestToLabel() {
		return getClosestTableCells(labelCell);
	}
	
	/**
	 * Find those web elements within table cells that are "closest" to the cell containing the
	 * label cell of this instance. See {@link TableData#getClosestTableCells(TableCellData)} for details.
	 * @return
	 */
	public WebElement getWebElementClosestToLabel() {
		List<TableCellData> cells = getClosestTableCells(labelCell);
		if(cells.isEmpty()) {
			return null;
		}
		return cells.get(0).getWebChildElement();
	}
		
	/**
	 * Given a table cell, represented by a TableCellData instance, find the other TableCellData 
	 * instances that are closest to the right of that instance in the represented table (more than one 
	 * in case of a tie). If none are found to the right, then repeat the same process for the left side.
	 * 
	 * @return
	 */
	public List<TableCellData> getClosestTableCells(TableCellData cell) {
		
		// 1) Create a list of all host cells except the one containing the label
		List<TableCellData> candidates = new ArrayList<TableCellData>();
		for(TableCellData candidate : hostCells) {
			if(cell.getDataId() != candidate.getDataId()) {
				candidates.add(candidate);
			}
		}
		
		// 2) Get the cell(s) closest to the left of the specified cell (more than one if there is a tie)
		List<TableCellData> leftcells = getClosestTableCells(cell, "right", candidates);
		
		// 3) If there are no cells to the left of the specified cell, get those that are closest to the right.
		if(leftcells.isEmpty()) {
			List<TableCellData> rightcells = getClosestTableCells(cell, "left", candidates);
			lastResults.clear();
			lastResults.addAll(rightcells);
		}
		else {		
			lastResults.clear();
			lastResults.addAll(leftcells);
		}
		
		return lastResults;
	}

	/**
	 * Given a particular table column index, pick from a list of table cells those that are closest 
	 * in a specified direction to that index position. More than one cell will be returned if there
	 * is a draw between cells that exist in separate rows.
	 * 
	 * @param columnIndex
	 * @param direction
	 * @param candidates
	 * @return
	 */
	private List<TableCellData> getClosestTableCells(TableCellData cell, String direction, List<TableCellData> candidates) {
		List<TableCellData> newCandidates = new ArrayList<TableCellData>();
		for(TableCellData candidate: candidates) {
			if(newCandidates.isEmpty()) {
				Integer colIdx = cell.getIndexInSharedRow(candidate);
				boolean isRight = colIdx > cell.getColumnIndex();
				boolean isLeft = colIdx < cell.getColumnIndex();
				if("right".equals(direction) && isRight)
					newCandidates.add(candidate);
				if("left".equals(direction) && isLeft)
					newCandidates.add(candidate);
			}
			else {
				Integer peerColIdx = cell.getIndexInSharedRow(newCandidates.get(0));
				Integer candColIdx = cell.getIndexInSharedRow(candidate);
				boolean isCloserRight = peerColIdx > candColIdx;
				boolean isCloserLeft = peerColIdx < candColIdx;
				boolean isTied = peerColIdx == candColIdx;
				if("right".equals(direction) && isCloserRight) {
					newCandidates.clear();
					newCandidates.add(candidate);
				}
				if("left".equals(direction) && isCloserLeft) {
					newCandidates.clear();
					newCandidates.add(candidate);
				}
				if(isTied) {
					newCandidates.add(candidate);
				}
			}
		}

		if(newCandidates.size() > 1) {
			// Results have been narrowed down to a subset. These must now compete with each other for best
			// proximity with outermost and leftmost (or rightmost) cell of the table they all share in common.
			// Recursing this way should narrow down to one cell.
			
			if(areAllSameCell(newCandidates)) {
				// The web elements probably abutt each other in the same table cell
				if("right".equals(direction)) {
					// newCandidates.remove(1);
				}
				if("left".equals(direction)) {
					// newCandidates.remove(0);
				}
			}
			else if(newCandidates.size() == candidates.size()) {
				// Could not narrow down cells. Return what we have to avoid endless loop.
				return candidates;
			}
			else {
				// Of the candidate table cells find which is outermost (closest to document root).
				TableCellData shallowest = null;
				for(TableCellData candidate: newCandidates) {
					if(shallowest == null || candidate.getDepth() < shallowest.getDepth())
						shallowest = candidate;
				}
				// Get the cell at the edge of the same row containing shallowest.
				WebElement bordercell = getBorderCell(shallowest.getWebElement(), direction);
				// Get a TableCellData instance for the border cell
				TableCellData newcell = TableCellData.getInstance(
						shallowest.getDataId(), cell, driver, bordercell, JAVASCRIPT_URL);
				// Find among the candidates which is horizontally the closest to the border cell
				return getClosestTableCells(newcell, direction, newCandidates);				
			}
		}
		
		return newCandidates;
	}
	
	/**
	 * @return Two WebElements are equal as determined by javascript (===)
	 */
	public boolean bothAreSameWebElement(WebElement webElement1, WebElement webElement2) {
		JavascriptExecutor executor = (JavascriptExecutor) driver;

		return (Boolean) executor.executeScript(
				"return arguments[0] === arguments[1]", 
				AbstractWebElement.unwrap(webElement1), 
				AbstractWebElement.unwrap(webElement2));
	}
	
	/**
	 * Determine if all table cell web elements in a list are all the same cell.
	 * 
	 * @param cells
	 * @return
	 */
	public boolean areAllSameCell(List<TableCellData> cells) {
		// 1) Compare without invoking javascript
		if(!TableCellData.areAllEquivalent(cells))
			return false;
		if(cells.size() == 1)
			return true;
		// 2) Compare using javascript
		TableCellData first = cells.get(0);
		for(TableCellData cell : cells) {
			if(!bothAreSameWebElement(first.getWebElement(), cell.getWebElement())) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Return either the first or last cell in the same row as the specified cell, depending on direction.
	 * 
	 * @param cell
	 * @param direction
	 * @return
	 */
	public WebElement getBorderCell(WebElement cell, String direction) {
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		if("right".equals(direction)) {
			return AbstractWebElement.wrap((WebElement) executor.executeScript(
					"return arguments[0].parentNode.cells[0]", 
					AbstractWebElement.unwrap(cell)));
		}
		else {
			return AbstractWebElement.wrap((WebElement) executor.executeScript(
					"var cells = arguments[0].parentNode.cells" +
					"return cells[cells.length-1];", 
					AbstractWebElement.unwrap(cell)));
		}
	}
	
	
	public int getRowCount() {
		return rows;
	}

	public int getColumnCount() {
		return columns;
	}

	public List<TableCellData> getHostCells() {
		return hostCells;
	}

	public WebElement getWebElement() {
		return thisTable;
	}

	public Integer getDepth() {
		return depth;
	}
}
