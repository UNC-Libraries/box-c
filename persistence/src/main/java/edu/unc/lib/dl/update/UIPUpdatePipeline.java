package edu.unc.lib.dl.update;

import java.util.List;


public class UIPUpdatePipeline {

	private List<UIPUpdateFilter> updateFilters;
	
	public UpdateInformationPackage processUIP(UpdateInformationPackage uip) throws UIPException {
		for (UIPUpdateFilter filter: this.updateFilters){
			uip = filter.doFilter(uip);
		}
		return uip;
	}

	public void setUpdateFilters(List<UIPUpdateFilter> updateFilters) {
		this.updateFilters = updateFilters;
	}
}
