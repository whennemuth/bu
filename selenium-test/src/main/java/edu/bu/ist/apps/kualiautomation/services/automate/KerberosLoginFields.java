package edu.bu.ist.apps.kualiautomation.services.automate;

import edu.bu.ist.apps.kualiautomation.entity.LabelAndValue;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;

public enum KerberosLoginFields {

	KUALI(
			null, 
			"j_username", 
			null,
			"j_password",
			"Continue"),
	KUALI_TESTDRIVE_1(
			null, 
			"login_user", 
			null, 
			"login_pw",
			"login"),
	KUALI_TESTDRIVE_2(
			null, 
			"username", 
			null, 
			"password", 
			"login");
	
	private String userLabel;
	private String userIdentifier;
	private String passwordLabel;
	private String passwordIdentifier;
	private String submitButtonLabel;
	
	private KerberosLoginFields(String userLabel, String userIdentifier, String passwordLabel, String passwordIdentifier, String submitButtonLabel) {
		this.userLabel = userLabel;
		this.userIdentifier = userIdentifier;
		this.passwordLabel = passwordLabel;
		this.passwordIdentifier = passwordIdentifier;
		this.submitButtonLabel = submitButtonLabel;
	}

	public String getUserLabel() {
		return userLabel;
	}
	public String getUserIdentifier() {
		return userIdentifier;
	}
	public String getPasswordLabel() {
		return passwordLabel;
	}
	public String getPasswordIdentifier() {
		return passwordIdentifier;
	}
	public String getSubmitButtonLabel() {
		return submitButtonLabel;
	}
	public LabelAndValue getUsernameLabelAndValue() {
		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.TEXTBOX.name());
		lv.setLabel(this.getUserLabel());
		lv.setIdentifier(this.getUserIdentifier());
		return lv;
	}
	public LabelAndValue getPasswordLabelAndValue() {
		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.PASSWORD.name());
		lv.setLabel(this.getPasswordLabel());
		lv.setIdentifier(this.getPasswordIdentifier());
		return lv;
	}
	public LabelAndValue getSubmitButtonLabelAndValue() {
		LabelAndValue lv = new LabelAndValue();
		lv.setElementType(ElementType.BUTTON.name());
		lv.setLabel(this.getSubmitButtonLabel());
		return lv;
	}
}
