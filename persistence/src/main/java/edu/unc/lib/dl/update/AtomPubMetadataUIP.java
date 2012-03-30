package edu.unc.lib.dl.update;

import java.io.IOException;
import java.util.HashMap;

import org.apache.abdera.model.Entry;
import org.jdom.Element;
import org.jdom.JDOMException;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.AtomPubMetadataParserUtil;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Update Information Package constructed from an Atom Pub entry object for metadata updating.
 * 
 * @author bbpennel
 * 
 */
public class AtomPubMetadataUIP extends MetadataUIP {

	public AtomPubMetadataUIP(PID pid, PersonAgent user, UpdateOperation operation, Entry entry) throws IOException,
			JDOMException, UIPException {
		super(pid, user, operation);
		incomingData = (HashMap<String, ?>) AtomPubMetadataParserUtil.extractDatastreams(entry);
		// If there are DC fields in the root entry document but no MODS, then add a null MODS stub for future population
		if (incomingData.containsKey(AtomPubMetadataParserUtil.ATOM_DC_DATASTREAM)
				&& !incomingData.containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())) {
			incomingData.put(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), null);
		}
		originalData = new HashMap<String, Element>();
		modifiedData = new HashMap<String, Element>();
		this.message = entry.getSummary();
	}
}
