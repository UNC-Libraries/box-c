package edu.unc.lib.dl.update;

public interface UIPUpdateFilter {
	public UpdateInformationPackage doFilter(UpdateInformationPackage uip) throws UIPException;
}
