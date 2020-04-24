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
package edu.unc.lib.dl.persist.services.edit;

import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.acl.PatronAccessDetails;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.test.SelfReturningAnswer;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static edu.unc.lib.dl.rdf.CdrAcl.embargoUntil;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ExpireEmbargoServiceTest {

    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private RepositoryObject repoObj;
    @Mock
    private Model model;
    @Mock
    private Resource resc;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private PremisLogger premisLogger;
    @Mock
    private Statement statement;
    @Mock
    private Literal literal;

    @Captor
    private ArgumentCaptor<String> labelCaptor;

    private PremisEventBuilder eventBuilder;
    private PID pid;
    private ExpireEmbargoService service;

    @Before
    public void init() {
        initMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());
        service = new ExpireEmbargoService();
        service.setRepositoryObjectLoader(repoObjLoader);

        Model objModel = ModelFactory.createDefaultModel();
        when(repoObj.getModel()).thenReturn(objModel);

        when(repoObjLoader.getRepositoryObject(any(PID.class))).thenReturn(repoObj);
        when(repoObj.getPid()).thenReturn(pid);
        when(model.getResource(anyString())).thenReturn(resc);

        eventBuilder = mock(PremisEventBuilder.class, new SelfReturningAnswer());
        when(repoObj.getPremisLog()).thenReturn(premisLogger);
        when(premisLogger.buildEvent(eq(Premis.ExpireEmbargo))).thenReturn(eventBuilder);
        when(agent.getUsernameUri()).thenReturn("agentname");
        when(eventBuilder.write()).thenReturn(resc);
    }

    @Test
    public void expireEmbargoTest() {
        Date embargoUntilDate = yesterday();

        when(resc.hasProperty(embargoUntil)).thenReturn(true);
        when(resc.getProperty(embargoUntil)).thenReturn(statement);
        when(statement.getLiteral()).thenReturn(literal);
        when(literal.getValue()).thenReturn(embargoUntilDate.toString());

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setEmbargo(embargoUntilDate);

        service.expireEmbargo(pid, accessDetails, agent);

        verify(premisLogger).buildEvent(eq(Premis.ExpireEmbargo));
        verify(eventBuilder).addEventDetail(labelCaptor.capture());
        assertEquals("Expired an embargo which ended " + embargoUntilDate, labelCaptor.getValue());
        verify(eventBuilder).writeAndClose();
    }


    @Test
    public void expireEmbargoFailureTest() {
        when(resc.hasProperty(embargoUntil)).thenReturn(false);
        when(resc.getProperty(embargoUntil)).thenReturn(null);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setEmbargo(null);

        service.expireEmbargo(pid, accessDetails, agent);

        verify(premisLogger).buildEvent(eq(Premis.ExpireEmbargo));
        verify(eventBuilder).addEventDetail(labelCaptor.capture());
        assertEquals("Failed to expire embargo.", labelCaptor.getValue());
        verify(eventBuilder).writeAndClose();
    }

    private Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }
}
