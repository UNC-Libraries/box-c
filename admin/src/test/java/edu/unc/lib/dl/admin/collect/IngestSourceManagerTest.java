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
package edu.unc.lib.dl.admin.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * @author bbpennel
 * @date Oct 23, 2015
 */
public class IngestSourceManagerTest {
    @Mock
    private TripleStoreQueryService tripleService;
    
    private IngestSourceManager sourceMan;
    
    @Before
    public void init() throws Exception {
        initMocks(this);
        sourceMan = new IngestSourceManager();
        //sourceMan.setTripleService(tripleService);
        sourceMan.setConfigPath("src/test/resources/ingestSources.json");
        sourceMan.init();
        
        when(tripleService.lookupAllContainersAbove(eq(new PID("uuid:testFolder"))))
                .thenReturn(Arrays.asList(new PID("uuid:root"), new PID("uuid:test")));
    }
    
//    @Test
//    public void listSourcesTest() throws Exception {
//        List<IngestSourceConfiguration> sources = sourceMan.listSources(new PID("uuid:testFolder"));
//        
//        assertNotNull(sources);
//        assertEquals("Only one source should match the path", 1, sources.size());
//        assertEquals("Got the wrong source", "testsource", sources.get(0).getId());
//    }
    
//    @Test
//    public void listSourcesNotInPathTest() throws Exception {
//        when(tripleService.lookupAllContainersAbove(any(PID.class)))
//                .thenReturn(Arrays.asList(new PID("uuid:root"), new PID("uuid:other")));
//        
//        List<IngestSourceConfiguration> sources = sourceMan.listSources(new PID("uuid:otherFolder"));
//        
//        assertEquals("No sources should match", 0, sources.size());
//    }
    
//    @Test
//    public void listCandidatesTest() throws Exception {
//        List<Map<String, Object>> candidates = sourceMan.listCandidates(new PID("uuid:testFolder"));
//        
//        assertNotNull(candidates);
//        assertEquals(2, candidates.size());
//        
//        Map<String, Object> candidateOne = candidates.get(0);
//        assertEquals("testsource", candidateOne.get("sourceId"));
//        assertEquals(PackagingType.DIRECTORY.toString(), candidateOne.get("packagingType"));
//        
//        Map<String, Object> candidateTwo = candidates.get(1);
//        assertEquals("testsource", candidateTwo.get("sourceId"));
//        assertEquals(PackagingType.BAGIT.toString(), candidateTwo.get("packagingType"));
//        assertEquals("0.96", candidateTwo.get("version"));
//        assertTrue("Failed to generate size of candidate bag", ((Long)candidateTwo.get("size")) > 0);
//        assertEquals("File count should only include files in the data dir", 4, candidateTwo.get("files"));
//    }
    
//    @Test
//    public void listCandidatesMultipleTest() throws Exception {
//        List<Map<String, Object>> candidates = sourceMan.listCandidates(new PID("uuid:nested"));
//        
//        assertNotNull(candidates);
//        assertEquals(4, candidates.size());
//        
//        Map<String, Object> candidate = candidates.get(1);
//        assertEquals("nestedsource", candidate.get("sourceId"));
//        assertEquals("smallbag", candidate.get("patternMatched"));
//        assertEquals(PackagingType.BAGIT.toString(), candidate.get("packagingType"));
//        assertEquals("0.96", candidate.get("version"));
//        assertTrue("Failed to generate size of candidate bag", ((Long)candidate.get("size")) > 0);
//        assertEquals("File count should only include files in the data dir", 4, candidate.get("files"));
//        
//        candidate = candidates.get(2);
//        assertEquals("nestedsource", candidate.get("sourceId"));
//        assertEquals("coll_folder/smallerbag", candidate.get("patternMatched"));
//        assertEquals(PackagingType.BAGIT.toString(), candidate.get("packagingType"));
//        assertEquals("0.96", candidate.get("version"));
//        assertTrue("Failed to generate size of candidate bag", ((Long)candidate.get("size")) > 0);
//        assertEquals("Count should only include payload", 3, candidate.get("files"));
//    }
}