/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.deposit.fcrepo4;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.jena.query.Dataset;

import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.reporting.ActivityMetricsClient;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;

/**
 * 
 * @author bbpennel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-fedora-container.xml", "/spring-test/cdr-client-container.xml"})
public class AbstractFedoraDepositJobIT {

    @Autowired
    protected String serverAddress;
    @Autowired
    protected Repository repository;
    @Autowired
    protected Dataset dataset;
    @Autowired
    protected JobStatusFactory jobStatusFactory;
    @Autowired
    protected DepositStatusFactory depositStatusFactory;
    @Autowired
    protected PremisLoggerFactory premisLoggerFactory;
    @Autowired
    protected FcrepoClient client;
    @Autowired
    protected ActivityMetricsClient metricsClient;
    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    
    protected File depositsDirectory;
    protected File depositDir;
    protected String jobUUID;
    protected String depositUUID;
    protected PID depositPid;
    
    @Before
    public void initBase() throws Exception {
        depositsDirectory = tmpFolder.newFolder("deposits");
        
        jobUUID = UUID.randomUUID().toString();

        depositPid = repository.mintDepositRecordPid();
        depositUUID = depositPid.getId();
        depositDir = new File(depositsDirectory, depositUUID);
        depositDir.mkdir();
    }

    protected URI createBaseContainer(String name) throws IOException, FcrepoOperationFailedException {
        URI baseUri = URI.create(serverAddress + name);
        // Create a parent object to put the binary into
        try (FcrepoResponse response = client.put(baseUri).perform()) {
            return response.getLocation();
        } catch (FcrepoOperationFailedException e) {
            // Eat conflict exceptions since this will run multiple times
            if (e.getStatusCode() != HttpStatus.SC_CONFLICT) {
                throw e;
            }
            return baseUri;
        }
    }
    
    protected ContentObject findContentObjectByPid(List<ContentObject> objs, final PID pid) {
        return objs.stream()
                .filter(p -> p.getPid().equals(pid)).findAny().get();
    }
}
