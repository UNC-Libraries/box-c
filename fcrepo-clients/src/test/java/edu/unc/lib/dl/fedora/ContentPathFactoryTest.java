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
package edu.unc.lib.dl.fedora;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.sparql.JenaSparqlQueryServiceImpl;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 *
 * @author bbpennel
 *
 */
public class ContentPathFactoryTest {

    private static final long CACHE_TIME_TO_LIVE = 100l;
    private static final long CACHE_MAX_SIZE = 5;
    private static final String FEDORA_BASE = "http://example.com/rest/";

    private ContentPathFactory pathFactory;

    private SparqlQueryService queryService;

    private Model model;

    private PID collectionsPid;

    @Before
    public void init() {

        model = ModelFactory.createDefaultModel();

        queryService = new JenaSparqlQueryServiceImpl(model);

        pathFactory = new ContentPathFactory();
        pathFactory.setQueryService(queryService);
        pathFactory.setCacheMaxSize(CACHE_MAX_SIZE);
        pathFactory.setCacheTimeToLive(CACHE_TIME_TO_LIVE);
        pathFactory.init();

        collectionsPid = makePid("collections");
        model.getResource(collectionsPid.getRepositoryPath());
    }

    @Test
    public void getAncestorPidsTest() throws Exception {
        PID unitPid = addChild(collectionsPid);
        PID collPid = addChild(unitPid);

        List<PID> ancestors = pathFactory.getAncestorPids(collPid);

        assertEquals("Incorrect number of ancestors", 2, ancestors.size());
        assertEquals(collectionsPid, ancestors.get(0));
        assertEquals(unitPid, ancestors.get(1));
    }

    @Test
    public void getFileAncestorsTest() throws Exception {
        PID unitPid = addChild(collectionsPid);
        PID collPid = addChild(unitPid);
        PID workPid = addChild(collPid);
        PID fileObjPid = addChild(workPid);
        PID filePid = addFile(fileObjPid);

        List<PID> ancestors = pathFactory.getAncestorPids(filePid);

        assertEquals("Incorrect number of ancestors", 5, ancestors.size());
        assertEquals(collectionsPid, ancestors.get(0));
        assertEquals(fileObjPid, ancestors.get(4));
    }

    @Test
    public void getCachedAncestorPidsTest() throws Exception {
        PID unitPid = addChild(collectionsPid);
        PID collPid = addChild(unitPid);

        List<PID> ancestors = pathFactory.getAncestorPids(collPid);

        // Switch ownership of coll to a new unit
        PID unit2Pid = addChild(collectionsPid);
        model.remove(model.getResource(unitPid.getRepositoryPath()),
                PcdmModels.hasMember, model.getResource(collPid.getRepositoryPath()));
        model.getResource(unit2Pid.getRepositoryPath())
                .addProperty(PcdmModels.hasMember, model.getResource(collPid.getRepositoryPath()));

        assertEquals("Incorrect number of ancestors", 2, ancestors.size());
        assertEquals(collectionsPid, ancestors.get(0));
        assertEquals(unitPid, ancestors.get(1));

        // Wait for cache to expire and then check that the new path is retrieved
        TimeUnit.MILLISECONDS.sleep(CACHE_TIME_TO_LIVE * 2);

        ancestors = pathFactory.getAncestorPids(collPid);
        assertEquals("Incorrect number of ancestors", 2, ancestors.size());
        assertEquals(collectionsPid, ancestors.get(0));
        assertEquals("Ancestors did not update to new unit", unit2Pid, ancestors.get(1));
    }

    private PID addChild(PID parentPid) {
        PID childPid = makePid();
        Resource childResc = model.getResource(childPid.getRepositoryPath());
        model.getResource(parentPid.getRepositoryPath())
                .addProperty(PcdmModels.hasMember, childResc);

        return childPid;
    }

    private PID addFile(PID parentPid) {
        PID childPid = makePid();
        Resource childResc = model.getResource(childPid.getRepositoryPath());
        model.getResource(parentPid.getRepositoryPath())
                .addProperty(PcdmModels.hasFile, childResc);

        return childPid;
    }

    private PID makePid() {
        return makePid(UUID.randomUUID().toString());
    }

    private PID makePid(String id) {
        return PIDs.get(id);
    }
}
