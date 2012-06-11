package edu.unc.lib.dl.ingest.sip;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.abdera.model.Entry;
import org.jdom.Element;
import org.jdom.JDOMException;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.AtomPubMetadataParserUtil;

public class AtomPubEntrySIP extends FileSIP {
	private Map<String, Element> metadataStreams;

	public AtomPubEntrySIP(PID containerPID, Entry atomEntry) throws IOException, JDOMException {
		this.containerPID = containerPID;
		this.setMetadataStreams(atomEntry);
	}

	public AtomPubEntrySIP(PID containerPID, Entry atomEntry, File data, String mimeType, String fileLabel,
			String md5checksum) throws IOException, JDOMException {
		super(containerPID, data, mimeType, fileLabel, md5checksum);
		setMetadataStreams(atomEntry);
	}

	public Map<String, Element> getMetadataStreams() {
		return metadataStreams;
	}

	public void setMetadataStreams(Map<String, Element> metadataStreams) {
		this.metadataStreams = metadataStreams;
	}

	public void setMetadataStreams(Entry atomEntry) throws IOException, JDOMException {
		if (atomEntry == null) {
			throw new IllegalArgumentException("A non-null atom entry must be provided.");
		}
		metadataStreams = AtomPubMetadataParserUtil.extractDatastreams(atomEntry);
	}
}
