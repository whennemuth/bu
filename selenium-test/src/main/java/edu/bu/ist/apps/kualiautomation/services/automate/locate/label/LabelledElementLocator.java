package edu.bu.ist.apps.kualiautomation.services.automate.locate.label;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.element.AbstractWebElement;
import edu.bu.ist.apps.kualiautomation.services.automate.element.AttributeInspector;
import edu.bu.ist.apps.kualiautomation.services.automate.element.BasicElement;
import edu.bu.ist.apps.kualiautomation.services.automate.element.Element;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.AbstractElementLocator;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.Locator;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.label.LabelledElementBatches.Batch;
import edu.bu.ist.apps.kualiautomation.services.automate.table.TableCellData;
import edu.bu.ist.apps.kualiautomation.services.automate.table.TableColumnData;
import edu.bu.ist.apps.kualiautomation.services.automate.table.TableData;
import edu.bu.ist.apps.kualiautomation.util.Utils;

/**
 * With a specified WebElement that serves as a label, traverse up the DOM one element at a time until
 * a descendant is found that matches the criteria of elementType. This should be the field that is labelled.
 * <br><br>
 * ASSUMPTION: This method assumes that a label and its field will share the same parent, grandparent, etc. - but
 * not with any other labels and fields - other fields and labels exist outside of the shared ancestor.
 * The one exception to this is for fields found in a table row for a particular label. The rules for selecting
 * the correct field for that label among the candiates are covered in 
 * {@link edu.bu.ist.apps.kualiautomation.services.automate.table.TableData TableData}
 * 
 * @author wrh
 *
 */
public class LabelledElementLocator extends AbstractElementLocator {
	
	private boolean labelCanBeHyperlink = true;
	private List<String> attributeValues = new ArrayList<String>();
	private List<Element> labelElements = new ArrayList<Element>();
	private List<WebElement> tableElements = new ArrayList<WebElement>();
	private boolean tableOnly;

	public LabelledElementLocator(WebDriver driver, Locator parent){
		this(driver, driver, parent);
	}
	
	public LabelledElementLocator(WebDriver driver, SearchContext searchContext, Locator parent){
		super(driver, searchContext, parent);
	}
	
	public LabelledElementLocator(WebDriver driver, SearchContext searchContext, List<Element> labelElements, Locator parent){
		this(driver, searchContext, parent);
		this.labelElements.addAll(labelElements);
	}
	
	public static List<Element> getBestInTable(
			WebDriver driver,
			ElementType elementType,
			List<Element> labelElements, 
			List<WebElement> tableElements,
			List<String> parameters,
			Locator parent) {
		
		LabelledElementLocator locator = new LabelledElementLocator(driver, parent);
		locator.labelElements.addAll(labelElements);
		locator.tableElements.addAll(tableElements);
		locator.tableOnly = true;
		
		return locator.locateAll(elementType, parameters);
	}
	
	/**
	 * Locate all WebElement instances that sufficiently match ElementType and parameters.
	 * Treat the first parameter as a label. 
	 * All remaining parameters will be considered values to test WebElement attributes against.
	 */
	@Override
	protected List<WebElement> customLocate() {
				
		List<WebElement> located;
		try {
			located = new ArrayList<WebElement>();
			if(elementType != null && elementType.getTagname() != null) {
				
				String label = new String(parameters.get(0));
				if("null".equalsIgnoreCase(label) || Utils.isEmpty(label)) {
					return new ArrayList<WebElement>();
				}
				
				if(parameters.size() > 1) {
					attributeValues.addAll(parameters.subList(1, parameters.size()));
				}
				
				if(!tableOnly && labelElements.isEmpty()) {
					LabelElementLocator labelLocator = new LabelElementLocator(driver, searchContext, this);
					labelLocator.setIgnoreHidden(super.ignoreHidden);
					labelLocator.setIgnoreDisabled(super.ignoreDisabled);
					labelLocator.setLabelCanBeHyperlink(this.labelCanBeHyperlink);
					labelElements.addAll(labelLocator.locateAll(elementType, Arrays.asList(new String[]{label})));
					
					located.addAll(tryLabelIsSoughtField(labelElements));
				}
				
				if(located.isEmpty()) {
					// Assume that more than one label is found and subdivide searches by each.
					LabelledElementBatches batches1 = new LabelledElementBatches();
					
					if(tableOnly) {
						for(Element labelElement : labelElements) {	
							filterHyperlinkCandidates(labelElement.getWebElement(), tableElements);
							if(!tableElements.isEmpty()) {
								batches1.add(labelElement, tableElements);
							}
						}
					}
					else {
						for(Element labelElement : labelElements) {				
							List<WebElement> flds = tryTraditionalLabelSearchMethod(labelElement.getWebElement());
							if(flds.isEmpty()) {
								flds = trySearchingOutwardFromLabel(labelElement.getWebElement());
							}
							//if(flds.size() > 1 && attributeValues.isEmpty()) {
							if(flds.size() > 1) {
								// 1 is ARBITRARY. It's a limit set to avoid additional time consuming calls to 
								// tryTabularSearchMethod(). Doing this however locks in a state where the links are  
								// the label and can only qualify if their innerText or additional attributes match the.
								// the label value. A single matched link does not have to qualify as the label and can be merely "labelled".
								filterHyperlinkCandidates(labelElement.getWebElement(), flds);
							}
							if(!flds.isEmpty()) {
								batches1.add(labelElement, flds);
							}
						}
					}
					
					batches1.loadOneToOneResultBatches(located);
					
					if(located.isEmpty() && !batches1.getBatches().isEmpty()) {
						// No label search produced a single result. Assume these labels and fields exist in a table and try to
						// narrow down the results for each label by their relative proximity within the table to that label.
						LabelledElementBatches batches2 = new LabelledElementBatches();
						for(Batch batch : batches1.getBatches()) {
							
							List<WebElement> tableFlds = tryTabularSearchMethod(
									batch.getLabel().getWebElement(), 
									batch.getBatch());

							batches2.add(batch.getLabel(), tableFlds);
							
							batches2.loadOneToOneResultBatches(located);
							if(!located.isEmpty()) {
								// There may be another one-to-one match in the next batch, but we'll take the first one we can get.
								// This minimizes calls to tryTabularSearchMethod, which will probably only get more expensive 
								// because the batches1 list is sorted by the amount of WebElements each batch has.
								//
								// NOTE: The long-term solution is to speed up tryTabularSearchMethod somehow so this doesn't matter.
								break;
							}
						}
						
						if(located.isEmpty()) {
							batches2.loadSmallestResultBatches(located);
						}
						
						if(located.isEmpty()) {
							batches2.loadAllResultBatches(located);
						}
						
						if(located.isEmpty()) {
							batches1.loadAllResultBatches(located);
						}
					}
				}			
			}
		} 
		finally {
			if(!tableOnly) {
				labelElements.clear();
				tableElements.clear();
				attributeValues.clear();
			}			
		}
		
		return located;
	}

	/**
	 * Some applications disguise buttons to look like hotspots or links and are label-like in appearance.
	 * If the label that was found is a button with the label text being value of the value attribute,
	 * then assume the user specified a labelling value as not being the text of a nearby web element, 
	 * but the web element itself.
	 * 
	 * @param labelElements
	 * @return
	 */
	private List<WebElement> tryLabelIsSoughtField(List<Element> labelElements) {
		List<WebElement> filtered = new ArrayList<WebElement>();
		List<WebElement> buttonLabels = new ArrayList<WebElement>();
		for(Element elmt : labelElements) {
			if(isButton(elmt.getWebElement())) {
				buttonLabels.add(elmt.getWebElement());
			}
		}
		if(!buttonLabels.isEmpty()) {
			if(!attributeValues.isEmpty()) {
				AttributeInspector inspector = new AttributeInspector(buttonLabels);
				filtered.addAll(inspector.findForValues(attributeValues));								
			}
			else {
				filtered.addAll(buttonLabels);
			}
		}
		return filtered;
	}
	
	/**
	 * The label might be a <label> html element with a for attribute, in which case the search for the element
	 * should be easy.
	 * 
	 * @param labelElement
	 * @param attributeValues
	 * @return
	 */
	private List<WebElement> tryTraditionalLabelSearchMethod(WebElement labelElement) {
		List<WebElement> flds = new ArrayList<WebElement>();
		if("label".equals(labelElement.getTagName())) {
			String id = labelElement.getAttribute("for");
			if(!Utils.isEmpty(id)) {
				List<WebElement> temp = AbstractWebElement.wrap(driver.findElements(By.id(id)));
				for(WebElement candidate : temp) {
					if(ElementType.getInstance(candidate).equals(elementType)) {
						flds.add(candidate);
					}
				}
			}
		}
		return flds;
	}
	
	/**
	 * Recurse up the DOM from the provided elements parent until the sought element is found, or the root node is reached.
	 * 
	 * @param labelElement
	 * @return
	 */
	private List<WebElement> trySearchingOutwardFromLabel(WebElement labelElement) {
		return trySearchingOutwardFromLabel(labelElement, labelElement, false);
	}
		
	private List<WebElement> trySearchingOutwardFromLabel(WebElement searchCtx, WebElement labelElement, boolean parentOnly) {

		List<WebElement> candidates = new ArrayList<WebElement>();
		if(!parentOnly) {
			candidates.addAll(AbstractWebElement.wrap(elementType.findAll(searchCtx, skipFrameSearch)));
		}

		if(candidates.isEmpty()) {
			WebElement parent = null;
			try {
				parent = AbstractWebElement.wrap(searchCtx.findElement(By.xpath("./..")));
			} 
			catch (InvalidSelectorException e) {
				// Runtime exception thrown when you are at the top of the DOM and try to select higher.
				// An instance of com.gargoylesoftware.htmlunit.html.HtmlPage is returned, which triggers the exception.
				return candidates;
			}
			if(parent != null) {
				return trySearchingOutwardFromLabel(parent, labelElement, false);
			}
			return candidates;
		}
		else {
			if(attributeValues.isEmpty()) {
				return candidates;
			}
			else {
				// If parameters are present beyond the first, they are attribute values, so pick only 
				// those webElements that have every attributeValue accounted for among their attributes.
				AttributeInspector inspector = new AttributeInspector(candidates);
				List<WebElement> filtered = inspector.findForValues(attributeValues);
				if(filtered.isEmpty()) {
					return trySearchingOutwardFromLabel(searchCtx, labelElement, true);
				}
				else {
					return filtered;
				}
			}
		}
	}

	/**
	 * This function is called if searching up to now has not narrowed results down to one web element.
	 * Assume the reason for this is that the web elements, including the label element were found in a 
	 * table on separate rows. This would make them peers and therefore all included. A Tablular search
	 * method assumes that the webElement closest to the label in terms of rows and columns  would be 
	 * the "winner".
	 * 
	 * @param labelElement
	 * @param located
	 * @return
	 */
	private List<WebElement> tryTabularSearchMethod(WebElement labelElement, List<WebElement> located) {
		
		// 1) Try a column-based search first.
		TableColumnData data = new TableColumnData(driver, labelElement, located, TableData.JAVASCRIPT_URL);		
		List<WebElement> filtered = data.getFirstElementsBelowLabelInSameColumn();		
		if(filtered.size() == 1) {
			return filtered;
		}

		// 2) Try a search based on shared table row proximity.
		TableData tableData = new TableData(driver, labelElement, located);
		filtered = new ArrayList<WebElement>();		
		if(tableData.getFirstSharedTableRow() != null) {
			
			List<TableCellData> cells = tableData.getTableCellsClosestToLabel();			
			
			if(cells.size() <= located.size() && !cells.isEmpty()) {
				for(TableCellData cell : cells) {
					filtered.add(cell.getWebChildElement());
				}
			}
			
			if(filtered.size() > 1) {
				// 3) Try column-based search again against filtered set.
				data = new TableColumnData(driver, labelElement, filtered, TableData.JAVASCRIPT_URL);
				List<WebElement> refiltered = data.getFirstElementsBelowLabelInSameColumn();
				if(!refiltered.isEmpty() && refiltered.size() < filtered.size()) {
					filtered.clear();
					filtered.addAll(refiltered);
				}
			}
		}

		if(filtered.isEmpty())
			return located;
		else
			return filtered;
	}
	
	/**
	 * A list of hyperlinks can be found as the result of a search for anything nearest the label that
	 * matches the hyperlink tag. However, if the hyperlink does not wrap any child elements and does
	 * does not have innerText that matches the label, then it must be removed from the list.
	 * @param labelElement
	 * @param candidates
	 */
	private void filterHyperlinkCandidates(WebElement labelElement, List<WebElement> candidates) {
		if(ElementType.HYPERLINK.equals(elementType)) {
			for (Iterator<WebElement> iterator = candidates.iterator(); iterator.hasNext();) {
				WebElement candidate = (WebElement) iterator.next();
				List<WebElement> children = new ArrayList<WebElement>();
				//List<WebElement> children = AbstractWebElement.wrap(candidate.findElements(By.xpath(".//*")));
				if(children.isEmpty()) {
					List<WebElement> filtered = new ArrayList<WebElement>();
					AttributeInspector inspector = new AttributeInspector(candidate);
					if(attributeValues.isEmpty())
						filtered = inspector.findAnyForValue(labelElement.getText());
					else
						filtered = inspector.findForValues(attributeValues);
					
					if(filtered.isEmpty()) {
						iterator.remove();
					}
				}
			}
		}
	}
	
	/**
	 * Find a web element that is a parent to the specified child element.
	 * This only works if javascript is enabled on the driver.
	 * @param childElement
	 * @return
	 */
	@SuppressWarnings("unused")
	private WebElement getParentElement(WebElement childElement) {
		JavascriptExecutor executor = (JavascriptExecutor)searchContext;
		WebElement parentElement = AbstractWebElement.wrap((WebElement)executor.executeScript(
				"return arguments[0].parentNode;", 
				AbstractWebElement.unwrap(childElement)));
		return parentElement;
	}
	
	@Override
	protected Element getElement(WebDriver driver, WebElement we) {
		return new BasicElement(driver, we);
	}
	
	public boolean isLabelCanBeHyperlink() {
		return labelCanBeHyperlink;
	}
	
	private boolean isButton(WebElement we) {
		return ElementType.getInstance(we).is(ElementType.BUTTON.name());
	}
	
	public void setLabelCanBeHyperlink(boolean labelCanBeHyperlink) {
		this.labelCanBeHyperlink = labelCanBeHyperlink;
	}
}
