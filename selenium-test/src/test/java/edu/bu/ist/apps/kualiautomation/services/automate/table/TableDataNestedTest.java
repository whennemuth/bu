package edu.bu.ist.apps.kualiautomation.services.automate.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.AbstractJettyBasedTest;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TableDataNestedTest extends AbstractJettyBasedTest {

	private TableData table;

	@Override
	public void setupBefore() {
	}

	@Override
	public void loadHandlers(Map<String, String> handlers) {
		handlers.put("table-test-page2", "TableTestPage2.htm");
	}
	
	@Test
	public void test01FindHostCellsNestedTables() {
		driver.get("http://localhost:8080/table-test-page2");
		String[] buttonIds = new String[]{"button2b", "button2c", "button2f", "button2g", "button2h"};		
		findAndAssert("label2a", buttonIds, 3, 5, 6, "tr2", "button2b");		
		findAndAssert("label2d", buttonIds, 3, 5, 6, "tr2", "button2f");
	}
	
	@Test
	public void test02FindHostCellsNestedTables() {		
		driver.get("http://localhost:8080/table-test-page2");
		findAndAssert("label2e", new String[]{"button2f", "button2g", "button2h"}, 1, 4, 4, "tr2-2", "button2f");
		findAndAssert("label2h", new String[]{"button2f", "button2g", "button2h"}, 1, 4, 4, "tr2-2", "button2h");
	}
	
	@Test
	public void test03FindHostCellsNestedTables() {		
		driver.get("http://localhost:8080/table-test-page2");
		findAndAssert("label2g", new String[]{"button2f", "button2g"}, 1, 4, 3, "tr2-2-1", "button2g");
		findAndAssert("label2g", new String[]{"button2f"}, 1, 4, 2, "tr2-2-1", "button2f");
		findAndAssert("label2f", new String[]{"button2f", "button2g"}, 1, 4, 3, "tr2-2-1", "button2f");
		findAndAssert("label2f", new String[]{"button2g"}, 1, 4, 2, "tr2-2-1", "button2g");
	}
	
	private void findAndAssert(
			String labelId, 
			String[] buttonIds, 
			int rows, 
			int cols, 
			int hostcellCount, 
			String rowId,
			String closestWebElementId) {
		
		List<WebElement> allElements = new ArrayList<WebElement>();
		WebElement labelElement = driver.findElement(By.id(labelId));
		assertNotNull(labelElement);
		for(String id : buttonIds) {
			allElements.add(driver.findElement(By.id(id)));
		}		
		table = new TableData(driver, labelElement, allElements);
		assertEquals(rows, table.getRowCount());
		assertEquals(cols, table.getColumnCount());		
		List<TableCellData> hostcells = table.getHostCells();
		assertEquals(hostcellCount, hostcells.size());
		WebElement tablerow = table.getFirstSharedTableRow();
		if(rowId == null) {
			assertNull(tablerow);
		}
		else {
			assertNotNull(tablerow);
			assertEquals(rowId, tablerow.getAttribute("id"));
		}
		WebElement closest = table.getWebElementClosestToLabel();
		if(closestWebElementId == null) {
			assertNull(closest);
		}
		else {
			assertNotNull(closest);
			assertEquals(closestWebElementId, closest.getAttribute("id"));
		}		
	}
}
