/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedorax.server.module.storage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.common.Constants;
import org.fcrepo.common.PID;
import org.fcrepo.common.rdf.SimpleLiteral;
import org.fcrepo.common.rdf.SimpleTriple;
import org.fcrepo.common.rdf.SimpleURIReference;
import org.fcrepo.server.errors.ResourceIndexException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.resourceIndex.TripleGenerator;
import org.fcrepo.server.resourceIndex.TripleGeneratorBase;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.Datastream;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;

import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Generates testing RDF triples for Fedora 3.0 objects implementing
 * info:fedora/demo:UVA_STD_IMAGE_1.
 * 
 * @author Chris Wilper
 * @author Benjamin Armintor
 */
public class PreservedObjectTripleGenerator extends TripleGeneratorBase
		implements Constants, TripleGenerator {

	/**
	 * {@inheritDoc}
	 */
	public Set<Triple> getTriplesForObject(DOReader reader)
			throws ResourceIndexException {

		Set<Triple> set = new HashSet<Triple>();

		addChecksumsForDatastreams(reader, set);

		return set;
	}

	private void addChecksumsForDatastreams(DOReader reader, Set<Triple> set)
			throws ResourceIndexException {
		try {
			URIReference objURI = new SimpleURIReference(new URI(
					PID.toURI(reader.GetObjectPID())));
			Datastream[] datastreams = reader.GetDatastreams(null, null);
			for (Datastream ds : datastreams) {
				if (ds.DSChecksum != null && !"".equals(ds.DSChecksum)) {
					URIReference dsURI = new SimpleURIReference(new URI(objURI
							.getURI().toString() + "/" + ds.DatastreamID));
					add(objURI, VIEW.DISSEMINATES, dsURI, set);
					set.add(new SimpleTriple(dsURI,
							new SimpleURIReference(ContentModelHelper.CDRProperty.hasChecksum.getURI()),
							new SimpleLiteral(ds.DSChecksum)));
				}
				// if (ds.DatastreamID.equals("DATA_FILE"))
			}
		} catch (ServerException e) {
			throw new ResourceIndexException(e.getLocalizedMessage(), e);
		} catch (URISyntaxException e) {
			throw new ResourceIndexException(e.getLocalizedMessage(), e);
		}
	}

}
