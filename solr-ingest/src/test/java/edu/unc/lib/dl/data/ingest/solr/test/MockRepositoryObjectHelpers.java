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

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.objects.AbstractContentContainerObject;
import edu.unc.lib.boxc.model.fcrepo.objects.AbstractContentObject;
import edu.unc.lib.boxc.model.fcrepo.objects.FileObjectImpl;

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

    public static FileObjectImpl makeFileObject(PID pid, RepositoryObjectLoader repositoryObjectLoader) {
        FileObjectImpl fileObj = mock(FileObjectImpl.class);
        when(fileObj.getPid()).thenReturn(pid);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObj);
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(pid.getRepositoryPath());
        resc.addProperty(RDF.type, Cdr.FileObject);
        when(fileObj.getResource()).thenReturn(resc);

        return fileObj;
    }

    public static AbstractContentContainerObject makeContainer(RepositoryObjectLoader repositoryObjectLoader) {
        return makeContainer(makePid(), repositoryObjectLoader);
    }

    public static AbstractContentContainerObject makeContainer(PID pid, RepositoryObjectLoader repositoryObjectLoader) {
        AbstractContentContainerObject container = mock(AbstractContentContainerObject.class);
        when(container.getMembers()).thenReturn(new ArrayList<>());
        when(container.getPid()).thenReturn(pid);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(container);
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(pid.getRepositoryPath());
        resc.addProperty(RDF.type, Cdr.Work);
        when(container.getResource()).thenReturn(model.getResource(pid.getRepositoryPath()));

        return container;
    }

    public static AbstractContentContainerObject addContainerToParent(AbstractContentContainerObject container,
            RepositoryObjectLoader repositoryObjectLoader) {
        return addContainerToParent(container, makePid(), repositoryObjectLoader);
    }

    public static AbstractContentContainerObject addContainerToParent(AbstractContentContainerObject container, PID childPid,
            RepositoryObjectLoader repositoryObjectLoader) {
        AbstractContentContainerObject memberObj = makeContainer(childPid, repositoryObjectLoader);
        AbstractContentObject child = (AbstractContentObject) repositoryObjectLoader.getRepositoryObject(childPid);
        child.getResource().addProperty(PcdmModels.memberOf, container.getResource());
        return memberObj;
    }

    public static FileObjectImpl addFileObjectToParent(AbstractContentContainerObject container, PID childPid,
            RepositoryObjectLoader repositoryObjectLoader) {
        FileObjectImpl memberObj = makeFileObject(childPid, repositoryObjectLoader);
        memberObj.getResource().addProperty(PcdmModels.memberOf, container.getResource());
        return memberObj;
    }

    public static void addMembers(AbstractContentContainerObject container, AbstractContentObject... children) {
        for (AbstractContentObject child : children) {
            child.getResource().addProperty(PcdmModels.memberOf, container.getResource());
        }
    }
}
