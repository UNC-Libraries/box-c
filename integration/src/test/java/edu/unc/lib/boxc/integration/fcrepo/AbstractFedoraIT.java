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
package edu.unc.lib.boxc.integration.fcrepo;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Model;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;

/**
 *
 * @author harring
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-fedora-container.xml", "/spring-test/cdr-client-container.xml"})
public abstract class AbstractFedoraIT {

    @Autowired
    protected String baseAddress;

    @Autowired
    protected FcrepoClient client;

    @Autowired
    protected PIDMinter pidMinter;
    @Autowired
    protected RepositoryObjectFactory repoObjFactory;
    @Autowired
    protected RepositoryObjectLoader repoObjLoader;
    @Autowired
    protected TransactionManager txManager;
    @Autowired
    protected RepositoryObjectDriver driver;

    @Autowired
    protected Model queryModel;
    @Autowired
    protected RepositoryObjectTreeIndexer treeIndexer;
    @Autowired
    protected RepositoryInitializer repoInitializer;

    @Before
    public void init_() {
        // Override base uri for IT tests
        TestHelper.setContentBase("http://localhost:48085/rest");
    }

    protected URI createBaseContainer(String name) throws IOException, FcrepoOperationFailedException {

        URI baseUri = URI.create(URIUtil.join(baseAddress, name));
        // Create a parent object to put the binary into
        try (FcrepoResponse response = client.put(baseUri).perform()) {
            return response.getLocation();
        } catch (FcrepoOperationFailedException e) {
            if (e.getStatusCode() != HttpStatus.SC_CONFLICT) {
                throw e;
            }
            // Ignore duplicate creation of base container
            return baseUri;
        }
    }

    /**
     * Asserts that the object identified by pid has been created in Fedora.  Does not work for binary resources
     *
     * @param pid
     * @throws FcrepoOperationFailedException
     * @throws IOException
     */
    protected void assertObjectExists(PID pid) throws IOException, FcrepoOperationFailedException {
        assertObjectExists(pid.getRepositoryUri());
    }

    protected void assertObjectExists(URI uri) throws IOException, FcrepoOperationFailedException {
        try (FcrepoResponse response = client.head(uri).perform()) {
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        }
    }

    protected ContentObject findContentObjectByPid(List<ContentObject> objs, final PID pid) {
        return objs.stream()
                .filter(p -> p.getPid().equals(pid)).findAny().get();
    }
}
