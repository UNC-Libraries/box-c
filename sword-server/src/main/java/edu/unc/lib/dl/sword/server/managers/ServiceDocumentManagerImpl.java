package edu.unc.lib.dl.sword.server.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.abdera.i18n.iri.IRI;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ServiceDocument;
import org.swordapp.server.ServiceDocumentManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordCollection;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.SwordWorkspace;

import edu.unc.lib.dl.fedora.PID;

/**
 * Generates service document from all containers which are the immediate children of the starting path.
 * 
 * @author bbpennel
 */
public class ServiceDocumentManagerImpl extends AbstractFedoraManager implements ServiceDocumentManager {
	public ServiceDocument getServiceDocument(String sdUri, AuthCredentials auth, SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
		ServiceDocument sd = new ServiceDocument();
		SwordWorkspace workspace = new SwordWorkspace();
		SwordCollection collection = new SwordCollection();
		collection.setTitle("Test collection");
		collection.addAcceptPackaging("cdrcore");
		collection.addAccepts("*/*");
		collection.setAbstract("This is a test collection");
		IRI iri = new IRI("http://cdr-alpha.lib.unc.edu/sword/sd-iri/testCollection");
		collection.addSubService(iri);
		
		workspace.addCollection(collection);
		
		sd.addWorkspace(workspace);
		sd.setMaxUploadSize(999999999);
		
		return sd;
	}
	
	protected List<SwordCollection> getImmediateContainerChildren(String pid) throws IOException {
		String query = this.readFileAsString("immediateContainerChildren.sparql");
		PID pidObject = new PID(pid);
		query = String.format(query, tripleStoreQueryService.getResourceIndexModelUri(), 
				pidObject.getURI());
		List<SwordCollection> result = new ArrayList<SwordCollection>();
		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<Map> bindings = (List<Map>) ((Map) tripleStoreQueryService.sendSPARQL(query).get("results"))
				.get("bindings");
		for (Map<?,?> binding : bindings) {
			SwordCollection collection = new SwordCollection();
			PID containerPID = new PID((String) ((Map<?,?>) binding.get("pid")).get("value"));
			String slug = (String) ((Map<?,?>) binding.get("slug")).get("value");
			
			collection.setHref(this.swordPath + "collection/" + containerPID.getPid());
			collection.setTitle(slug);
			collection.addAccepts("*/*");
			//collection.addAcceptPackaging("cdrcore");
			collection.addAcceptPackaging("METSDSpaceSIP");
			IRI sdIRI = new IRI(this.swordPath + "servicedocument/" + containerPID.getPid());
			collection.addSubService(sdIRI);
			result.add(collection);
		}
		return result;
	}
}
