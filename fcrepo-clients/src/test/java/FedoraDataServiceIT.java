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
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.annotation.Resource;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fedora.FedoraDataService;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * Copyright 2010 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Gregory Jansen
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class FedoraDataServiceIT {

    @Resource
    private FedoraDataService fedoraDataService = null;

    @Resource
    private TripleStoreQueryService tripleStoreQueryService = null;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link edu.unc.lib.dl.fedora.FedoraDataService#getObjectViewXML(java.lang.String)}.
     *
    @Test
    public void testGetObjectViewXML() {
	try {
	    PID p = this.getTripleStoreQueryService().fetchByRepositoryPath("/Collections");
	    System.out.println("PID:" + p.getPid());
	    Document d = this.getFedoraDataService().getObjectViewXML(p.getPid());
	    XMLOutputter out = new XMLOutputter();
	    out.output(d, System.out);
	} catch(FedoraException e) {
	    e.printStackTrace();
	    fail(e.getLocalizedMessage());
	} catch(IOException e) {
	    throw new Error("Error in test code", e);
	} catch(NullPointerException e) {
	    e.printStackTrace();
	    throw new Error(e);
	}

    }*/

    @Test
    public void testGetItemViewXML() {
	try {
	    String collPath = this.getTripleStoreQueryService().fetchAllCollectionPaths().get(0);
	    Assert.assertNotNull("Have to be able to find at least one collection for integration test", collPath);
	    System.out.println("found collection path: "+collPath);
	    String pid = this.getTripleStoreQueryService().fetchByRepositoryPath(collPath).getPid();
	    System.out.println("using test PID:" + pid);
	    Document d = this.getFedoraDataService().getObjectViewXML(pid);
	    XMLOutputter out = new XMLOutputter();
	    out.output(d, System.out);
	} catch(FedoraException e) {
	    e.printStackTrace();
	    fail(e.getLocalizedMessage());
	} catch(IOException e) {
	    throw new Error("Error in test code", e);
	} catch(NullPointerException e) {
	    fail("NPE, this can be caused by Fedora not having been initialized and/or absence of any collections: "+e.getLocalizedMessage());
	    e.printStackTrace();
	    throw new Error(e);
	}
    }

    public FedoraDataService getFedoraDataService() {
        return fedoraDataService;
    }

    public void setFedoraDataService(FedoraDataService fedoraDataService) {
        this.fedoraDataService = fedoraDataService;
    }

    public TripleStoreQueryService getTripleStoreQueryService() {
        return tripleStoreQueryService;
    }

    public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
        this.tripleStoreQueryService = tripleStoreQueryService;
    }

}
