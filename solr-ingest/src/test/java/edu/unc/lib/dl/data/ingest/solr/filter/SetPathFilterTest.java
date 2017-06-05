/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.data.ingest.solr.filter;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 * 
 * @author lfarrell
 *
 */
public class SetPathFilterTest {
	
    @Mock
    private ContentPathFactory pathFactory;
    
    @Mock
    private DocumentIndexingPackageDataLoader loader;
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private IndexDocumentBean idb;
    
    @Mock
    private PID pid;
    
    private SetPathFilter filter;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		
		when(pid.getPid()).thenReturn("uuid:" + UUID.randomUUID().toString());
        
        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
		
		filter = new SetPathFilter();
		filter.setPathFactory(pathFactory);
	}
	
	@Test
	public void testUnitPath() throws Exception {
	    // Assert that the parent unit and collection are not set
	}
	
	@Test
	public void testCollectionPath() throws Exception {
	    // Test that the ancestor path is set to contain unit
	    
	    // Assert that the parent collection is not set
	}
	
	@Test
	public void testWorkPath() throws Exception {
	    // Assert that the rollup is the id of the work itself
	}
	
	@Test
	public void testFileObjectPath() throws Exception {
	    // Assert that the rollup is the id of the parent work
	}
	
	@Test(expected = IndexingException.class)
	public void testNoAncestors() throws Exception {
	    
	}
}
