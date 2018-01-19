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
package edu.unc.lib.dl.services.camel.solr;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/solr-update-it-context.xml")
})
public abstract class AbstractSolrProcessorIT {

    @Autowired
    protected String baseAddress;

    @Autowired
    protected File solrDataDir;
    @Autowired
    protected EmbeddedSolrServer server;
    @Autowired
    protected SolrUpdateDriver driver;
    @Autowired
    protected SolrSearchService solrSearchService;
    @javax.annotation.Resource(name = "accessGroups")
    protected AccessGroupSet accessGroups;

    @Autowired
    protected Model queryModel;
    @Autowired
    protected RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    protected RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    protected DocumentIndexingPackageFactory dipFactory;
    @Autowired
    protected RepositoryPIDMinter pidMinter;

    protected ContentRootObject rootObj;
    protected AdminUnit unitObj;
    protected CollectionObject collObj;

    @Mock
    protected Exchange exchange;
    @Mock
    protected Message message;

    protected void generateBaseStructure() throws Exception {
        PID rootPid = pidMinter.mintContentPid();
        repositoryObjectFactory.createContentRootObject(rootPid.getRepositoryUri(), null);
        rootObj = repositoryObjectLoader.getContentRootObject(rootPid);

        PID unitPid = pidMinter.mintContentPid();
        Model unitModel = ModelFactory.createDefaultModel();
        Resource unitResc = unitModel.getResource(unitPid.getRepositoryPath());
        unitResc.addProperty(CdrAcl.unitOwner, "admin");
        unitObj = repositoryObjectFactory.createAdminUnit(unitPid, unitModel);
        rootObj.addMember(unitObj);

        PID collPid = pidMinter.mintContentPid();
        Model collModel = ModelFactory.createDefaultModel();
        Resource collResc = collModel.getResource(collPid.getRepositoryPath());
        collResc.addProperty(CdrAcl.canAccess, AUTHENTICATED_PRINC);
        collObj = repositoryObjectFactory.createCollectionObject(collPid, collModel);
        unitObj.addMember(collObj);
    }

    protected void setMessageTarget(RepositoryObject obj) {
        when(message.getHeader(eq(FCREPO_URI)))
                .thenReturn(obj.getPid().getRepositoryPath());
    }

    protected void indexObjectsInTripleStore(RepositoryObject... objs) {
        for (RepositoryObject obj : objs) {
            queryModel.add(obj.getModel());
        }
    }
}
