package cdr.forms;

import javax.servlet.http.HttpSession;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;

import crosswalk.Form;

public abstract class AbstractFormFactory {
	private final static String SESSION_PREFIX = "form-";
	
	abstract public Form getForm(String id);

	public Form getSessionForm(String formId, HttpSession session) {
		StringBuilder sb = new StringBuilder();
		sb.append(SESSION_PREFIX).append(formId);
		String formAttrId = sb.toString();
		Form result = (Form) session.getAttribute(formAttrId);
		if(result == null) {
			Form def = this.getForm(formId);
			Copier copier = new EcoreUtil.Copier(false, true);
			result = (Form)copier.copy(def);
			copier.copyReferences();
			session.setAttribute(formAttrId, result);
		}
		return result;
	}
}
