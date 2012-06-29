package cdr.forms;

import crosswalk.Form;
import edu.unc.lib.dl.security.access.UserSecurityProfile;

public class PermissionDeniedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2942249514946141228L;
	
	private UserSecurityProfile userSecurityProfile = null;
	private Form form = null;
	private String formId = null;

	public Form getForm() {
		return form;
	}

	public void setForm(Form form) {
		this.form = form;
	}

	public String getFormId() {
		return formId;
	}

	public void setFormId(String formId) {
		this.formId = formId;
	}

	public UserSecurityProfile getUserSecurityProfile() {
		return userSecurityProfile;
	}

	public void setUserSecurityProfile(UserSecurityProfile userSecurityProfile) {
		this.userSecurityProfile = userSecurityProfile;
	}

	public PermissionDeniedException(String message, UserSecurityProfile userSecurityProfile, Form form, String formId) {
		super(message);
		this.userSecurityProfile = userSecurityProfile;
		this.form = form;
		this.formId = formId;
	}
}
