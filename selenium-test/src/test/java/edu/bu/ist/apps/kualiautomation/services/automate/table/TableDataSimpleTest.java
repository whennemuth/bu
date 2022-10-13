package edu.bu.ist.apps.kualiautomation.services.automate.table;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TableDataSimpleTest extends AbstractJettyBasedTest {

	private TableData table;

	@Override
	public void setupBefore() {
	}

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("table-test-page1", "TableTestPage1.htm");
		handlers.put("table-test-page2", "TableTestPage2.htm");
	}
	
	/**
	 * For table with row:
	 * --------------------------------------
	 * labe1a | button1a | label1b | button1b
	 * --------------------------------------
	 * 
	 * ... Out of both cells that contain a button, find which is closest to the right of label1a (should be button1a)
	 */
	@Test
	public void test01FindHostCellsSingleTable1a() {		
		List<TableCellData> closest = testFindHostCellsSingleTable("label1a");
		assertEquals(1, closest.size());		
		assertTrue(isExpectedCell(closest.get(0), 1, 2, "match1a"));
	}
	
	/**
	 * For table with row:
	 * --------------------------------------
	 * labe1a | button1a | label1b | button1b
	 * --------------------------------------
	 * 
	 * ... Out of both cells that contain a button, find which is closest to the right of label1b (should be button1b)
	 */
	@Test
	public void test02FindHostCellsSingleTable1b() {		
		List<TableCellData> closest = testFindHostCellsSingleTable("label1b");
		assertEquals(1, closest.size());		
		assertTrue(isExpectedCell(closest.get(0), 1, 4, "match1b"));
	}
	
	/**
	 * For table with row:
	 * --------------------------------------
	 * labe1a | button1a | label1b | button1b
	 * --------------------------------------
	 * 
	 * ... Out of both cells that contain a button, find which is closest to the left of label1c (should be button1b)
	 */
	@Test
	public void test03FindHostCellsSingleTable1c() {		
		List<TableCellData> closest = testFindHostCellsSingleTable("label1c");
		assertEquals(1, closest.size());		
		assertTrue(isExpectedCell(closest.get(0), 1, 4, "match1b"));
	}
	
	/**
	 * Test detection that two WebElement instance do or do not share a common table ancestor.
	 */
	@Test
	public void test04CommonTableAncestry1() {
		driver.get("http://localhost:8080/table-test-page1");
		
		// 1) Two separate tables can be told apart if they have different column counts
		List<WebElement> labelledElements = driver.findElements(By.xpath("//input[@title='test1']"));
		assertEquals(2, labelledElements.size());
		WebElement labelElement = driver.findElement(By.id("outerlabel1"));
		assertNotNull(labelElement);
		assertEquals("outer-label1", labelElement.getText().trim());		
		table = new TableData(driver, labelElement, labelledElements);
		List<TableCellData> hostcells = table.getHostCells();
		assertNull(table.getFirstSharedTableRow());
		
		// 2) Two separate tables with same depth, rows count, and column count can be told apart by javascript
		labelledElements = driver.findElements(By.xpath("//input[@title='test3']"));
		assertEquals(2, labelledElements.size());
		labelElement = driver.findElement(By.id("outerlabel2"));
		assertNotNull(labelElement);
		assertEquals("outer-label2", labelElement.getText().trim());		
		table = new TableData(driver, labelElement, labelledElements);
		hostcells = table.getHostCells();
		assertNull(table.getFirstSharedTableRow());
		
		// 3) Two WebElements with the same table ancestry and same row
		labelledElements.clear();
		labelledElements.add(driver.findElement(By.id("button1e")));
		labelledElements.add(driver.findElement(By.id("button1f")));
		assertEquals(2, labelledElements.size());
		table = new TableData(driver, labelElement, labelledElements);
		hostcells = table.getHostCells();
		assertEquals(2, hostcells.size()); // will not include cell that contains the label (not in a table).		
		assertNotNull(table.getFirstSharedTableRow());
			
		// 4) Two WebElements with the same table ancestry and different row
		labelledElements = driver.findElements(By.xpath("//input[@title='test5']"));
		assertEquals(2, labelledElements.size());
		labelElement = driver.findElement(By.id("outerlabel3"));
		assertNotNull(labelElement);
		assertEquals("outer-label3", labelElement.getText().trim());		
		table = new TableData(driver, labelElement, labelledElements);
		hostcells = table.getHostCells();
		assertNull(table.getFirstSharedTableRow());
		
	}
	
	private List<TableCellData> testFindHostCellsSingleTable(String labelElementId) {
		
		/**
		 * 1) Open the web page
		 */
		driver.get("http://localhost:8080/table-test-page2");
		
		/**
		 * 2) Simulate a condition in which a LabelledElementLocator search produced 2 results for a 
		 * Particular label. Typically TableData would be used to find the web element closest to 
		 * the right of the label.
		 */
		List<WebElement> labelledElements = driver.findElements(By.xpath("//input[@title='test1']"));
		assertEquals(2, labelledElements.size());
		WebElement labelElement = driver.findElement(By.id(labelElementId));
		assertNotNull(labelElement);
		
		/**
		 * 3) Instantiate the TableData and assert that the located WebElement instances are now wrapped
		 * in TableCellElements that correctly reflect their relative position within the table.
		 */
		table = new TableData(driver, labelElement, labelledElements);
		assertNotNull(table.getFirstSharedTableRow());
		assertEquals(3, table.getRowCount());
		assertEquals(5, table.getColumnCount());
		
		List<TableCellData> hostcells = table.getHostCells();
		assertEquals(3, hostcells.size());
		
		TableCellData labelCell = hostcells.get(0);
		List<TableCellData> othercells = hostcells.subList(1, hostcells.size());
		
		assertTrue("The first cell returned by TableData.getHostCells() should always contain the label element", 
				isExpectedCell(labelCell, 1, 1, labelElementId));
		
		assertTrue(hasExpectedCell(othercells, 1, 2, "match1a"));
		
		assertTrue(hasExpectedCell(othercells, 1, 4, "match1b"));
		
		assertNotNull(table.getFirstSharedTableRow());
		
		assertTrue(labelCell.getDepth() > table.getDepth());
		
		// TD is right under TABLE and TR, or TABLE, TBODY and TR
		assertTrue((labelCell.getDepth() - table.getDepth()) <= 3); 
		
		List<TableCellData> closest = table.getTableCellsClosestToLabel();

		return closest;
	}
	
	/**
	 * Determine if any of a list of TableElementCell instances occupies a specified row and column 
	 * position and contains a WebELement that is identifiable by a specified property.
	 * 
	 * @param hostcells The cells, one of which should match based on the remaining parameters
	 * @param row The expected row index of the cell.
	 * @param column The expected column index of the cell
	 * @param property The expected value of either the "myprop" attribute or the innerText property 
	 * of the WebElement inside the cell.
	 * @return
	 */
	public static boolean hasExpectedCell(List<TableCellData> hostcells, int row, int column, String property) {
		for(TableCellData cell : hostcells) {
			if(cell.getRowIndex() != row)
				continue;
			if(cell.getColumnIndex() != cell.getColumnIndex())
				continue;
			if(property.equalsIgnoreCase(cell.getWebChildElement().getText()))
				return true;
			if(property.equalsIgnoreCase(cell.getWebChildElement().getAttribute("myprop")))
				return true;
		}
		return false;
	}

	public static boolean isExpectedCell(TableCellData hostcell, int row, int column, String property) {
		return hasExpectedCell(Arrays.asList(hostcell), row, column, property);
	}
}
