package edu.unc.lib.dl.update;

import java.util.List;


public class UIPUpdatePipeline {

	private List<UIPUpdateFilter> updateFilters;
	
	public UpdateInformationPackage processUIP(UpdateInformationPackage uip) throws UpdateException {
		try {
			for (UIPUpdateFilter filter: this.updateFilters){
				uip = filter.doFilter(uip);
			}
		} catch (UIPException e){
			throw new UpdateException("Failed to apply filter to UIP", e);
		}
		return uip;
	}

	public void setUpdateFilters(List<UIPUpdateFilter> updateFilters) {
		this.updateFilters = updateFilters;
	}
}
