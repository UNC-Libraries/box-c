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

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/*
 * @author harring
 */
public class SetRecordDatesFilterTest {
	
	private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";
	
	@Mock
	private DocumentIndexingPackageDataLoader loader;
	@Mock
	private DocumentIndexingPackage dip;
	@Mock
	private IndexDocumentBean idb;
	@Mock
	private PID pid;
	
	private SetRecordDatesFilter filter;
	
	@Before
	public void setup() throws Exception {
		initMocks(this);

		when(pid.getPid()).thenReturn(PID_STRING);
		
		when(dip.getDocument()).thenReturn(idb);
		when(dip.getPid()).thenReturn(pid);

		filter = new SetRecordDatesFilter();
	}
	
	@Test
	public void testCreateDate() throws Exception {
		
	}
	
	@Test
	public void testUpdateDate() throws Exception {
		
	}
	
	
	
}
