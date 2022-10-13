package edu.bu.ist.apps.kualiautomation.services.automate.locate.label;

import org.openqa.selenium.WebElement;

/**
 * This is a basic implementation of ComparableLabel. It triggers the default comparing method by
 * equating all objects in its custom implementation and triggering the default logic in ComparableLabel
 * due to this lack of varying rank among compared items.
 * 
 * @author wrh
 *
 */
public class BasicComparableLabel extends ComparableLabel {
	
	/**
	 * Restrict this constructor (useDefaultMethodIfIndeterminate should be true always). 
	 */
	private BasicComparableLabel(String label, String text, boolean useDefaultMethodIfIndeterminate) {
		super(label, text, useDefaultMethodIfIndeterminate);
	}
	
	/**
	 * Restrict this constructor (useDefaultMethodIfIndeterminate should be true always). 
	 */
	private BasicComparableLabel(WebElement elmt, String label, String text, boolean useDefaultMethodIfIndeterminate) {
		super(elmt, label, text, useDefaultMethodIfIndeterminate);
	}

	public BasicComparableLabel(WebElement elmt, String label, String text) {
		this(elmt, label, text, true);
	}

	public BasicComparableLabel(String label, String text) {
		this(label, text, true);
	}

	@Override
	protected int customCompareTo(ComparableLabel lbl) {
		return 0;
	}

}
