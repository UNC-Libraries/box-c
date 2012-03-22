package edu.unc.lib.dl.update;

import java.util.List;

import edu.unc.lib.dl.ingest.IngestException;

public class UIPUpdatePipeline {

	private List<UIPUpdateFilter> updateFilters;
	
	public UpdateInformationPackage processUIP(UpdateInformationPackage uip) throws IngestException {
		try {
			for (UIPUpdateFilter filter: this.updateFilters){
				uip = filter.doFilter(uip);
			}
		} catch (UIPException e){
			throw new IngestException("Failed to apply filter to UIP", e);
		}
		return uip;
	}

	public void setUpdateFilters(List<UIPUpdateFilter> updateFilters) {
		this.updateFilters = updateFilters;
	}
}
