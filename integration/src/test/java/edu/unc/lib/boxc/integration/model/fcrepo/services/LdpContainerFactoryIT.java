package edu.unc.lib.boxc.integration.model.fcrepo.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.fcrepo.client.FcrepoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.integration.fcrepo.AbstractFedoraIT;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.rdf.RDFModelUtil;
import edu.unc.lib.boxc.model.fcrepo.services.LdpContainerFactory;

/**
 *
 * @author bbpennel
 *
 */
public class LdpContainerFactoryIT extends AbstractFedoraIT {

    @Autowired
    private LdpContainerFactory factory;

    @Test
    public void createDirectContainerTest() throws Exception {
        URI objUri;
        // create the base object
        try (FcrepoResponse resp = client.post(URI.create(baseAddress)).perform()) {
            objUri = resp.getLocation();
        }

        URI childUri;
        URI container = factory.createDirectContainer(objUri, PcdmModels.hasMember, "files");
        try (FcrepoResponse resp = client.post(container).perform()) {
            childUri = resp.getLocation();
        }

        Model model;
        try (FcrepoResponse resp = client.get(objUri).perform()) {
            model = RDFModelUtil.createModel(resp.getBody());
        }

        Resource objResc = model.getResource(objUri.toString());
        Statement member = objResc.getProperty(PcdmModels.hasMember);

        assertNotNull(member, "No hasMember relationship");
        assertEquals(childUri.toString(), member.getResource().toString(),
                "hasMember relation references the wrong object");
    }

    @Test
    public void createIndirectContainerTest() throws Exception {
        URI objUri;
        // create the base object
        try (FcrepoResponse resp = client.post(URI.create(baseAddress)).perform()) {
            objUri = resp.getLocation();
        }
        // Create the indirect container for the base object
        URI container = factory.createIndirectContainer(objUri, PcdmModels.hasRelatedObject, "related");

        // Create another object, which will later be added the base object
        URI childUri;
        try (FcrepoResponse resp = client.post(URI.create(baseAddress)).perform()) {
            childUri = resp.getLocation();
        }

        // Add the proxy to the container
        factory.createIndirectProxy(container, objUri, childUri);

        Model model;
        try (FcrepoResponse resp = client.get(objUri).perform()) {
            model = RDFModelUtil.createModel(resp.getBody());
        }

        Resource objResc = model.getResource(objUri.toString());
        Statement member = objResc.getProperty(PcdmModels.hasRelatedObject);

        assertNotNull(member, "No hasRelatedObject relationship");
        assertEquals(childUri.toString(), member.getResource().toString(),
                "hasRelatedObject relation references the wrong object");
    }
}
