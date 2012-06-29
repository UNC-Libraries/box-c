package cdr.forms;

import java.beans.PropertyEditorSupport;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class DateEditor extends PropertyEditorSupport {
	private DateFormat format = null;
	
	public DateEditor(DateFormat format) {
		this.format = format;
	}
	
	public void setAsText(String text) throws IllegalArgumentException {
		try {
            setValue(format.parse(text));
		}
        catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Could not convert Date for " + text + ": " + e.getMessage());
        }
	}

	public String getAsText() {
        Date value = (Date) getValue();
		return (value != null ? format.format(value) : "");
	}
}
