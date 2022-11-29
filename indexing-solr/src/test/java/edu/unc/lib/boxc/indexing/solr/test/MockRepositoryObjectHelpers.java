package edu.unc.lib.boxc.indexing.solr.test;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 *
 * @author bbpennel
 *
 */
public class MockRepositoryObjectHelpers {

    private MockRepositoryObjectHelpers() {
    }

    public static PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    public static FileObject makeFileObject(PID pid, RepositoryObjectLoader repositoryObjectLoader) {
        FileObject fileObj = mock(FileObject.class);
        when(fileObj.getPid()).thenReturn(pid);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObj);
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(pid.getRepositoryPath());
        resc.addProperty(RDF.type, Cdr.FileObject);
        when(fileObj.getResource()).thenReturn(resc);

        return fileObj;
    }

    public static ContentContainerObject makeContainer(RepositoryObjectLoader repositoryObjectLoader) {
        return makeContainer(makePid(), repositoryObjectLoader);
    }

    public static ContentContainerObject makeContainer(PID pid, RepositoryObjectLoader repositoryObjectLoader) {
        ContentContainerObject container = mock(ContentContainerObject.class);
        when(container.getMembers()).thenReturn(new ArrayList<>());
        when(container.getPid()).thenReturn(pid);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(container);
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(pid.getRepositoryPath());
        resc.addProperty(RDF.type, Cdr.Work);
        when(container.getResource()).thenReturn(model.getResource(pid.getRepositoryPath()));

        return container;
    }

    public static ContentContainerObject addContainerToParent(ContentContainerObject container,
            RepositoryObjectLoader repositoryObjectLoader) {
        return addContainerToParent(container, makePid(), repositoryObjectLoader);
    }

    public static ContentContainerObject addContainerToParent(ContentContainerObject container, PID childPid,
            RepositoryObjectLoader repositoryObjectLoader) {
        ContentContainerObject memberObj = makeContainer(childPid, repositoryObjectLoader);
        ContentObject child = (ContentObject) repositoryObjectLoader.getRepositoryObject(childPid);
        child.getResource().addProperty(PcdmModels.memberOf, container.getResource());
        return memberObj;
    }

    public static FileObject addFileObjectToParent(ContentContainerObject container, PID childPid,
            RepositoryObjectLoader repositoryObjectLoader) {
        FileObject memberObj = makeFileObject(childPid, repositoryObjectLoader);
        memberObj.getResource().addProperty(PcdmModels.memberOf, container.getResource());
        return memberObj;
    }

    public static void addMembers(ContentContainerObject container, ContentObject... children) {
        for (ContentObject child : children) {
            child.getResource().addProperty(PcdmModels.memberOf, container.getResource());
        }
    }
}
