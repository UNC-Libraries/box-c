package edu.unc.lib.dl.cdr.services.tika;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractFedoraEnhancement;
import edu.unc.lib.dl.cdr.services.AbstractIrodsObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException.Severity;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FileSystemException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;

/**
 * Enhancement which extracts the full text from sourceData datastreams of supported mimetypes. The text is stored to
 * MD_FULL_TEXT datastream of the object. A fullText relation is also added to the object to record which objects have
 * had full text extracted and which datastream it is stored in.
 * 
 * @author bbpennel
 * 
 */
public class FullTextEnhancement extends AbstractFedoraEnhancement {
	private static final Logger LOG = LoggerFactory.getLogger(FullTextEnhancement.class);

	public FullTextEnhancement(FullTextEnhancementService service, PID pid) {
		super(service, pid);
	}

	@Override
	public Element call() throws EnhancementException {
		Element result = null;
		LOG.debug("Called image enhancement service for " + pid);

		Document foxml = null;
		String dsid = null;
		try {
			// get sourceData data stream IDs
			List<String> srcDSURIs = this.service.getTripleStoreQueryService().getSourceData(pid);

			foxml = service.getManagementClient().getObjectXML(pid);

			// get current DS version paths in iRODS
			for (String srcURI : srcDSURIs) {
				dsid = srcURI.substring(srcURI.lastIndexOf("/") + 1);

				String dsLocation = null;
				String dsIrodsPath = null;
				String vid = null;

				Datastream ds = service.getManagementClient().getDatastream(pid, dsid, "");

				vid = ds.getVersionID();
				dsLocation = this.getDSLocation(dsid, vid, foxml);

				if (dsLocation != null) {
					dsIrodsPath = service.getManagementClient().getIrodsPath(dsLocation);

					String text = this.extractText(dsIrodsPath);

					// Add full text ds to object
					String textURL = service.getManagementClient().upload(text);

					if (FOXMLJDOMUtil.getDatastream(foxml, ContentModelHelper.Datastream.MD_FULL_TEXT.getName()) == null) {
						String message = "Adding full text metadata extracted by Apache Tika";
						service.getManagementClient().addManagedDatastream(pid,
								ContentModelHelper.Datastream.MD_FULL_TEXT.getName(), false, message, new ArrayList<String>(),
								ContentModelHelper.Datastream.MD_FULL_TEXT.getLabel(), false, "text/plain", textURL);
					} else {
						String message = "Replacing full text metadata extracted by Apache Tika";
						service.getManagementClient().modifyDatastreamByReference(pid,
								ContentModelHelper.Datastream.MD_FULL_TEXT.getName(), false, message, new ArrayList<String>(),
								ContentModelHelper.Datastream.MD_FULL_TEXT.getLabel(), "text/plain", null, null, textURL);
					}

					// Add full text relation
					PID textPID = new PID(pid.getPid() + "/" + ContentModelHelper.Datastream.MD_FULL_TEXT.getName());
					setExclusiveTripleRelation(pid, ContentModelHelper.CDRProperty.fullText.toString(), textPID);
				}
			}
		} catch (FileSystemException e) {
			throw new EnhancementException(e, Severity.FATAL);
		} catch (NotFoundException e) {
			throw new EnhancementException(e, Severity.UNRECOVERABLE);
		} catch (FedoraException e) {
			throw new EnhancementException("Image Enhancement failed to process " + dsid, e, Severity.RECOVERABLE);
		} catch (Exception e) {
			throw new EnhancementException("Image Enhancement failed to process " + dsid, e, Severity.UNRECOVERABLE);
		}

		return result;
	}

	private String extractText(String dsIrodsPath) throws Exception {
		LOG.debug("Run irods script to perform text extraction on {} ", dsIrodsPath);
		InputStream response = ((AbstractIrodsObjectEnhancementService) service).remoteExecuteWithPhysicalLocation(
				"textextract", dsIrodsPath);
		BufferedReader r = new BufferedReader(new InputStreamReader(response));
		try {
			StringBuilder text = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				text.append(line);
			}
			return text.toString().trim();
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				r.close();
			} catch (Exception ignored) {
			}
		}
	}
}
