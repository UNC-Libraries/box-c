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
package edu.unc.lib.dl.fcrepo4;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.test.TestHelper;
import edu.unc.lib.dl.util.URIUtil;

/**
 *
 * @author harring
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-fedora-container.xml", "/spring-test/cdr-client-container.xml"})
public class AbstractFedoraIT {

    @Autowired
    protected String baseAddress;

    @Autowired
    protected FcrepoClient client;

    @Autowired
    protected RepositoryPIDMinter pidMinter;
    @Autowired
    protected RepositoryObjectFactory repoObjFactory;
    @Autowired
    protected RepositoryObjectLoader repoObjLoader;
    @Autowired
    protected TransactionManager txManager;
    @Autowired
    protected RepositoryObjectDriver driver;

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

    @Test
    public void dummyTest() throws Exception {
        // a placeholder to prevent JUnit from displaying an error for this test,
        // which isn't intended to have its own test cases
    }
}
