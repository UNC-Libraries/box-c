package edu.unc.lib.dl.update;

import java.io.IOException;
import java.util.HashMap;

import org.apache.abdera.model.Entry;
import org.jdom.Element;
import org.jdom.JDOMException;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.AtomPubMetadataParserUtil;

/**
 * Update Information Package constructed from an Atom Pub entry object for metadata updating.
 * @author bbpennel
 *
 */
public class AtomPubMetadataUIP extends MetadataUIP {

	public AtomPubMetadataUIP(PID pid, PersonAgent user, UpdateOperation operation, Entry entry) throws IOException, JDOMException, UIPException{
		super(pid, user, operation);
		incomingData = (HashMap<String, ?>)AtomPubMetadataParserUtil.extractDatastreams(entry);
		originalData = new HashMap<String,Element>();
		modifiedData = new HashMap<String,Element>();
		this.message = entry.getSummary();
	}
}
