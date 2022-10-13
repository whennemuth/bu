package edu.bu.ist.apps.kualiautomation.services.automate.locate.label;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.services.automate.element.AbstractWebElement;

/**
 * One label is considered a better match than another label if that label is a direct child of the other.
 * 
 * NOTE: A descendent will not be better than an ancestor if it is more than one level deeper.
 * @author wrh
 *
 */
public class HierarchyBasedComparableLabel extends ComparableLabel {

	public HierarchyBasedComparableLabel(WebElement elmt, String label, String text, List<WebElement> elmts) {
		super(elmt, label, text, false);
		this.list.addAll(elmts);
	}

	private List<WebElement> list = new ArrayList<WebElement>();
	
	public HierarchyBasedComparableLabel(ComparableLabel comparable, List<ComparableLabel> list) {
		super(comparable.getWebElement(), comparable.getRawLabel(), comparable.getRawText(), false);
		for(ComparableLabel label : list) {
			this.list.add(label.getWebElement());
		}
	}
	
	@Override
	protected int customCompareTo(ComparableLabel lbl) {
		
		if(webElement.equals(lbl.getWebElement()))
			return ITS_A_DRAW;

		// Get the immediate children of the this label
		List<WebElement> thisChildren = AbstractWebElement.wrap(webElement.findElements(By.xpath("./*")));
		for(WebElement child : thisChildren) {
			for(WebElement elmt : list) {
				if(child.equals(elmt)) {
					this.disqualified = true;
					return OTHER_LABEL_IS_BETTER;
				}				
			}
		}
		
		// Get the immediate children of the this label
		List<WebElement> otherChildren = AbstractWebElement.wrap(lbl.getWebElement().findElements(By.xpath("./*")));
		for(WebElement child : otherChildren) {
			for(WebElement elmt : list) {
				if(child.equals(elmt)) {
					lbl.disqualified = true;
					return THIS_LABEL_IS_BETTER;
				}				
			}
		}
		
		return ITS_A_DRAW;
	}

	public static List<ComparableLabel> getInstances(List<ComparableLabel> labels) {
		List<ComparableLabel> list = new ArrayList<ComparableLabel>();
		for(ComparableLabel comparable : labels) {
			list.add(new HierarchyBasedComparableLabel(comparable, labels));
		}
		return list;
	}

}
