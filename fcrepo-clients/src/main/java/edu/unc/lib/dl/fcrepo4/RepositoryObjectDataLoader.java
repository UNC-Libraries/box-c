package edu.unc.lib.dl.fcrepo4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.utils.DateUtils;
import org.apache.jena.riot.Lang;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;

public class RepositoryObjectDataLoader {

	private FcrepoClient client;
	
	/**
	 * Loads and assigns the RDF types for the given object
	 * 
	 * @param obj
	 * @return
	 * @throws FedoraException
	 */
	public RepositoryObjectDataLoader loadTypes(RepositoryObject obj) throws FedoraException {
		List<String> types = new ArrayList<>();
		// Iterate through all type properties and add to list
		Resource resc = obj.getModel().getResource(obj.getPid().getRepositoryUri().toString());
		StmtIterator it = resc.listProperties(RDF.type);
		while (it.hasNext()) {
			types.add(it.nextStatement().getResource().getURI());
		}

		obj.setTypes(types);

		return this;
	}

	/**
	 * Loads and assigns the model for direct relationships of the given
	 * repository object
	 * 
	 * @param obj
	 * @return
	 * @throws FedoraException
	 */
	public RepositoryObjectDataLoader loadModel(RepositoryObject obj) throws FedoraException {
		PID pid = obj.getPid();

		try (FcrepoResponse response = getClient().get(pid.getRepositoryUri())
				.accept("text/turtle")
				.perform()) {
			Model model = ModelFactory.createDefaultModel();
			model.read(response.getBody(), null, Lang.TURTLE.getName());

			obj.setModel(model);
		} catch (IOException e) {
			throw new FedoraException("Failed to read model for " + pid.getRepositoryUri(), e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}

		return this;
	}

	public RepositoryObjectDataLoader loadHeaders(RepositoryObject obj) throws FedoraException {
		PID pid = obj.getPid();

		try (FcrepoResponse response = getClient().head(pid.getRepositoryUri()).perform()) {
			if (response.getStatusCode() != 200) {
				throw new FedoraException("Received " + response.getStatusCode()
						+ " response while retrieving headers for " + pid.getRepositoryUri());
			}

			obj.setEtag(response.getHeaderValue("Etag"));
			String lastModString = response.getHeaderValue("Last-Modified");
			if (lastModString != null) {
				obj.setLastModified(DateUtils.parseDate(lastModString));
			}
		} catch (IOException e) {
			throw new FedoraException("Unable to create deposit record at " + pid.getRepositoryUri(), e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}

		return this;
	}

	public void setClient(FcrepoClient client) {
		this.client = client;
	}

	public FcrepoClient getClient() {
		return client;
	}
}
