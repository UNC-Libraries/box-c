package edu.unc.lib.dl.data.ingest.solr.filter;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class SetRecordDatesFilter implements IndexDocumentFilter {
	protected static final Logger log = LoggerFactory.getLogger(SetRecordDatesFilter.class);

	private TripleStoreQueryService tsqs;
	private String recordDatesQuery;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		String query = String.format(recordDatesQuery, tsqs.getResourceIndexModelUri(), dip.getPid().getURI());
		
		Map results = tsqs.sendSPARQL(query);
		List<Map> bindings = (List<Map>) ((Map) results.get("results")).get("bindings");
		if (bindings.size() == 0) {
			throw new IndexingException("Object " + dip.getPid() + " could not be found");
		} else {
			IndexDocumentBean idb = dip.getDocument();
			
			try {
				idb.setDateUpdated((String)bindings.get(0).get("modifiedDate"));
				idb.setDateAdded((String)bindings.get(0).get("createdDate"));
			} catch (ParseException e) {
				throw new IndexingException("Failed to parse date format from system generated date fields for " + dip.getPid().getPid(), e);
			}
		}
	}
}
