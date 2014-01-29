package edu.unc.lib.dl.data.ingest.solr.action;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * Updates an object and all of its descendants using the pipeline provided. No cleanup is performed on any of the
 * updated objects.
 * 
 * @author bbpennel
 * 
 */
public class UpdateTreeAction extends AbstractIndexingAction {
	private static final Logger log = LoggerFactory.getLogger(UpdateTreeAction.class);

	@Autowired
	protected TripleStoreQueryService tsqs;
	private String descendantsQuery;
	protected boolean addDocumentMode = true;

	@PostConstruct
	public void init() {
		try {
			descendantsQuery = FileUtils.inputStreamToString(this.getClass().getResourceAsStream("countDescendants.itql"));
		} catch (IOException e) {
			log.error("Failed to load queries", e);
		}
	}

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		log.debug("Starting update tree of {}", updateRequest.getPid().getPid());

		// Perform updates
		index(updateRequest);

		if (log.isDebugEnabled())
			log.debug("Finished updating tree of " + updateRequest.getPid().getPid() + ".  "
					+ updateRequest.getChildrenPending() + " objects updated in "
					+ (System.currentTimeMillis() - updateRequest.getTimeStarted()) + " ms");
	}

	public void setTsqs(TripleStoreQueryService tsqs) {
		this.tsqs = tsqs;
	}

	public boolean isAddDocumentMode() {
		return addDocumentMode;
	}

	public void setAddDocumentMode(boolean addDocumentMode) {
		this.addDocumentMode = addDocumentMode;
	}

	protected void index(SolrUpdateRequest updateRequest) {
		int totalObjects = countDescendants(updateRequest.getPid()) + 1;
		updateRequest.setChildrenPending(totalObjects);

		// Start indexing
		RecursiveTreeIndexer treeIndexer = new RecursiveTreeIndexer(updateRequest, this, addDocumentMode);
		treeIndexer.index(updateRequest.getPid(), null);
	}

	protected int countDescendants(PID pid) {
		List<List<String>> results = tsqs.queryResourceIndex(String.format(descendantsQuery,
				this.tsqs.getResourceIndexModelUri(), pid.getURI()));
		if (results == null || results.size() == 0 || results.get(0).size() == 0)
			return 0;
		return Integer.parseInt(results.get(0).get(0));
	}

	public DocumentIndexingPackage getDocumentIndexingPackage(PID pid, DocumentIndexingPackage parent) {
		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackage(pid);
		dip.setParentDocument(parent);
		return dip;
	}
}