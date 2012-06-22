package edu.unc.lib.dl.cdr.services.model;

import edu.unc.lib.dl.fedora.PID;

public class LabeledPID extends PID {
	private static final long serialVersionUID = 1L;
	private String label;
	
	public LabeledPID(String pid, String label) {
		super(pid);
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
