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
package edu.unc.lib.dl.cdr.services.processing;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.UUID;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author harring
 *
 */
public class SetAsPrimaryObjectServiceTest {

    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private WorkObject workObj;
    @Mock
    private FileObject fileObj;
    @Mock
    private RepositoryObjectFactory factory;
    @Mock
    private Resource primaryResc;

    private PID fileObjPid;
    private PID workObjPid;
    private SetAsPrimaryObjectService service;

    @Before
    public void init() {
        initMocks(this);

        fileObjPid = makePid();
        workObjPid = makePid();

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }

        }).when(workObj).setPrimaryObject(fileObjPid);

        when(agent.getPrincipals()).thenReturn(groups);
        when(repoObjLoader.getRepositoryObject(eq(fileObjPid))).thenReturn(fileObj);

        service = new SetAsPrimaryObjectService();
        service.setAclService(aclService);
        service.setRepositoryObjectLoader(repoObjLoader);
    }

    @Test
    public void setFileObjectAsPrimaryTest() {
        when(fileObj.getParent()).thenReturn(workObj);

        service.setAsPrimaryObject(agent, fileObjPid);

        verify(workObj).setPrimaryObject(fileObjPid);
        verify(factory).createExclusiveRelationship(any(PID.class), any(Property.class), any(Resource.class));

    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

}
