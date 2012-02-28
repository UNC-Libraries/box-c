package edu.unc.lib.dl.ingest.aip;

import java.net.URI;
import java.util.List;

import org.jrdf.graph.Graph;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContainerPlacement;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JRDFGraphUtil;

public class BiomedCentralAIPFilter implements AIPIngestFilter {

	@Override
	public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip) throws AIPException {
		//TODO limit to biomed central ingests
		RDFAwareAIPImpl rdfaip = null;
		if (aip instanceof RDFAwareAIPImpl) {
			rdfaip = (RDFAwareAIPImpl) aip;
		} else {
			rdfaip = new RDFAwareAIPImpl(aip);
		}
		filter(rdfaip);
		return null;
	}

	private void filter(RDFAwareAIPImpl rdfaip) throws AIPException {
		Graph g = rdfaip.getGraph();
		
		for (PID pid: rdfaip.getTopPIDs()){
			List<URI> contentModels = JRDFGraphUtil.getContentModels(g, pid);
			boolean isAggregate = false;
			for (URI contentModel: contentModels){
				if (ContentModelHelper.Model.AGGREGATE_WORK.equals(contentModel)){
					isAggregate = true;
					break;
				}
			}
			//If the top level PID(s) is not an aggregate, then quit
			if (!isAggregate)
				return;
		}

		for (PID pid: rdfaip.getPIDs()){
			ContainerPlacement placement = rdfaip.getContainerPlacement(pid);
			if (placement != null && placement.parentPID != null){
				String slug = JRDFGraphUtil.getRelatedLiteralObject(g, placement.pid, ContentModelHelper.CDRProperty.slug.getURI());
				if (slug == null) {
					throw new AIPException(pid.getPid() + " missing slug.");
				}
				if (slug.matches("^[0-9\\-]+\\.[xX][mM][lL]$")){
					// suppress the XML main file by turning off indexing
					JRDFGraphUtil.removeAllRelatedByPredicate(g, pid, ContentModelHelper.CDRProperty.allowIndexing.getURI());
					JRDFGraphUtil.addCDRProperty(g, pid, ContentModelHelper.CDRProperty.allowIndexing, "no");
				} else if (slug.matches("^[0-9\\-]+\\.\\w+$")){
					// If this is a main object, then designate it as a default web object for its parent container
					try {
						JRDFGraphUtil.addCDRProperty(g, placement.parentPID, ContentModelHelper.CDRProperty.defaultWebObject, new URI(pid.getURI()));
					} catch (Exception e){
						throw new AIPException("Could not add defaultWebObject triple for " + pid.getPid(), e);
					}
				}
				// Ignore supplemental files, which end in -S<#>
			}
			
		}
	}
}
