/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.data.ingest.solr.test;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.PcdmModels;

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
