package cdr.forms;

import crosswalk.Form;

public abstract class AbstractFormFactory {
	
	/**
	 * Returns a new instance of the given form model.
	 * @param id the identifier of the form, usually file name
	 * @return the form ecore model
	 */
	abstract public Form getForm(String id);
}
