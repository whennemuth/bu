package edu.bu.ist.apps.kualiautomation.services.automate;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class KerberosLoginParms {
	
	private String username;
	private String password;
	private String usernameLabel;
	private String passwordLabel;
	private String usernameOtherIdentifier;
	private String passwordOtherIdentifier;
	private String submitButtonLabel;
	private Integer configEnvironmentId = -1;
	private KerberosLoginFields kerberosLoginFields = KerberosLoginFields.KUALI;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUsernameLabel() {
		return kerberosLoginFields.getUserLabel();
	}
	public void setUsernameLabel(String usernameLabel) {
		this.usernameLabel = usernameLabel;
	}
	public String getPasswordLabel() {
		return kerberosLoginFields.getPasswordLabel();
	}
	public void setPasswordLabel(String passwordLabel) {
		this.passwordLabel = passwordLabel;
	}
	public String getUsernameOtherIdentifier() {
		return kerberosLoginFields.getUserIdentifier();
	}
	public void setUsernameOtherIdentifier(String usernameOtherIdentifier) {
		this.usernameOtherIdentifier = usernameOtherIdentifier;
	}
	public String getPasswordOtherIdentifier() {
		return kerberosLoginFields.getPasswordIdentifier();
	}
	public void setPasswordOtherIdentifier(String passwordOtherIdentifier) {
		this.passwordOtherIdentifier = passwordOtherIdentifier;
	}
	public String getSubmitButtonLabel() {
		return kerberosLoginFields.getSubmitButtonLabel();
	}
	public void setSubmitButtonLabel(String submitButtonLabel) {
		this.submitButtonLabel = submitButtonLabel;
	}
	public Integer getConfigEnvironmentId() {
		return configEnvironmentId;
	}
	public void setConfigEnvironmentId(Integer configEnvironmentId) {
		this.configEnvironmentId = configEnvironmentId;
	}
	@JsonIgnore
	public KerberosLoginFields getKerberosLoginFields() {
		return kerberosLoginFields;
	}
	@JsonIgnore
	public void setKerberosLoginFields(KerberosLoginFields kerberosLoginFields) {
		this.kerberosLoginFields = kerberosLoginFields;
	}
	
}
