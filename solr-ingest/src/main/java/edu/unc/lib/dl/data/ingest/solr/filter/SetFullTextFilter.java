package edu.unc.lib.dl.data.ingest.solr.filter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Retrieves full text data for object being indexed and stores it to the indexing document
 * @author bbpennel
 *
 */
public class SetFullTextFilter extends AbstractIndexDocumentFilter{
	private static final Logger log = LoggerFactory.getLogger(SetFullTextFilter.class);

	private AccessClient accessClient = null;

	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {

		List<String> result;
		// Check that this object has a full text DS before retrying to retrieve it
		if (dip.getTriples() != null) {
			result = dip.getTriples().get(ContentModelHelper.CDRProperty.fullText.getPredicate());
		} else {
			result = tsqs.fetchBySubjectAndPredicate(dip.getPid(), ContentModelHelper.CDRProperty.fullText.name());
		}

		if (result.size() == 0 || "false".equals(result.get(0)))
			return;

		try {
			MIMETypedStream stream = accessClient.getDatastreamDissemination(dip.getPid(), ContentModelHelper.Datastream.MD_FULL_TEXT.name(), null);
			dip.getDocument().setFullText(new String(stream.getStream()));
		} catch (FedoraException e) {
			log.error("Failed to retrieve full text datastream for {}", dip.getPid().getPid(), e);
		} catch (ServiceException e) {
			log.error("Failed to retrieve full text datastream for {}", dip.getPid().getPid(), e);
		}
	}

	public AccessClient getAccessClient() {
		return accessClient;
	}

	public void setAccessClient(AccessClient accessClient) {
		this.accessClient = accessClient;
	}
}
