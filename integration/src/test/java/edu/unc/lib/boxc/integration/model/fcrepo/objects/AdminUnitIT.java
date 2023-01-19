package edu.unc.lib.boxc.integration.model.fcrepo.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;

/**
 *
 * @author bbpennel
 *
 */
public class AdminUnitIT extends AbstractFedoraIT {

    @Test
    public void testCreateAdminUnit() {
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource("");
        resc.addProperty(DcElements.title, "Unit Title");

        AdminUnit obj = repoObjFactory.createAdminUnit(model);

        assertTrue(obj.getTypes().contains(Cdr.AdminUnit.getURI()));
        assertTrue(obj.getTypes().contains(PcdmModels.Object.getURI()));

        assertEquals("Unit Title", obj.getResource()
                .getProperty(DcElements.title).getString());
    }

    @Test
    public void testAddCollectionAndGetMembers() throws Exception {
        AdminUnit obj = repoObjFactory.createAdminUnit(null);
        CollectionObject collectionObj = repoObjFactory.createCollectionObject(null);

        obj.addMember(collectionObj);

        treeIndexer.indexAll(baseAddress);

        List<ContentObject> members = obj.getMembers();
        assertEquals(1, members.size());
        assertEquals(collectionObj.getPid(), members.get(0).getPid(),
                "Must return the created collection as a member");
    }

    @Test
    public void testGetParent() throws Exception {
        repoInitializer.initializeRepository();

        ContentRootObject contentRoot = repoObjLoader.getContentRootObject(
                RepositoryPaths.getContentRootPid());

        AdminUnit adminUnit = repoObjFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit);

        RepositoryObject parent = adminUnit.getParent();
        assertEquals(contentRoot.getPid(), parent.getPid(),
                "Parent returned by the child must match the folder it was created in");
    }
}
