package edu.unc.lib.dl.update;

public interface UIPProcessor {
	public void process(UpdateInformationPackage uip) throws UpdateException, UIPException;
}
