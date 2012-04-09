package edu.unc.lib.dl.ingest.sip;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.abdera.model.Entry;
import org.jdom.Element;
import org.jdom.JDOMException;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.AtomPubMetadataParserUtil;

public class AtomPubEntrySIP implements SubmissionInformationPackage {

	private PID containerPID;
	private boolean inProgress = false;
	private String suggestedSlug;
	private Map<String,Element> metadataStreams;
	private File contentFile;
	private String contentMimetype;
	private String filename;

	public AtomPubEntrySIP(PID containerPID, Entry atomEntry) throws IOException, JDOMException{
		if (atomEntry == null){
			throw new IllegalArgumentException("A non-null atom entry must be provided.");
		}
		this.containerPID = containerPID;
		metadataStreams = AtomPubMetadataParserUtil.extractDatastreams(atomEntry);
	}

	public PID getContainerPID() {
		return containerPID;
	}

	public void setContainerPID(PID containerPID) {
		this.containerPID = containerPID;
	}

	public boolean isInProgress() {
		return inProgress;
	}

	public void setInProgress(boolean inProgress) {
		this.inProgress = inProgress;
	}

	public String getSuggestedSlug() {
		return suggestedSlug;
	}

	public void setSuggestedSlug(String suggestedSlug) {
		this.suggestedSlug = suggestedSlug;
	}

	public Map<String, Element> getMetadataStreams() {
		return metadataStreams;
	}

	public void setMetadataStreams(Map<String, Element> metadataStreams) {
		this.metadataStreams = metadataStreams;
	}

	public File getContentFile() {
		return contentFile;
	}

	public void setContentFile(File contentFile) {
		this.contentFile = contentFile;
	}

	public String getContentMimetype() {
		return contentMimetype;
	}

	public void setContentMimetype(String mimetype) {
		this.contentMimetype = mimetype;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
}
