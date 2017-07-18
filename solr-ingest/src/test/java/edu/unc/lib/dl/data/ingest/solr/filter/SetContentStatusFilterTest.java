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
package edu.unc.lib.dl.data.ingest.solr.filter;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.FacetConstants;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Model;

public class SetContentStatusFilterTest extends Assert {
    @Mock
    private DocumentIndexingPackageDataLoader loader;
    private DocumentIndexingPackageFactory factory;

    private Map<String, List<String>> triples;

    private SetContentStatusFilter filter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        factory = new DocumentIndexingPackageFactory();
        factory.setDataLoader(loader);

        triples = new HashMap<String, List<String>>();
        triples.put(FedoraProperty.disseminates.toString(),
                Arrays.asList("info:fedora/uuid:item/" + Datastream.RELS_EXT.getName()));

        when(loader.loadTriples(any(DocumentIndexingPackage.class))).thenReturn(triples);

        filter = new SetContentStatusFilter();
    }

    @Test
    public void testDescribedQuery() throws Exception {

        triples.put(FedoraProperty.hasModel.toString(),
                Arrays.asList(Model.SIMPLE.toString()));

        triples.put(FedoraProperty.disseminates.toString(), Arrays.asList("info:fedora/uuid:item/"
                + Datastream.MD_DESCRIPTIVE.getName(), "info:fedora/uuid:item/"
                + Datastream.RELS_EXT.getName()));

        when(loader.loadParentDip(any(DocumentIndexingPackage.class))).thenReturn(factory.createDip("uuid:parent"));
        DocumentIndexingPackage dip = factory.createDip("uuid:item");
        filter.filter(dip);
        IndexDocumentBean idb = dip.getDocument();

        assertEquals("Only one content status should be set for non-aggregate", 1, idb.getContentStatus().size());
        assertTrue(idb.getContentStatus().contains(FacetConstants.CONTENT_DESCRIBED));

    }

    @Test
    public void testNotDescribedQuery() throws Exception {

        when(loader.loadParentDip(any(DocumentIndexingPackage.class))).thenReturn(factory.createDip("uuid:parent"));
        DocumentIndexingPackage dip = factory.createDip("uuid:item");
        filter.filter(dip);
        IndexDocumentBean idb = dip.getDocument();

        assertEquals("Only one content status should be set for non-aggregate", 1, idb.getContentStatus().size());
        assertTrue(idb.getContentStatus().contains(FacetConstants.CONTENT_NOT_DESCRIBED));

    }

    @Test
    public void testAggregateNoDWOQuery() throws Exception {
        triples.put(FedoraProperty.hasModel.toString(),
                Arrays.asList(Model.AGGREGATE_WORK.toString()));

        DocumentIndexingPackage dip = factory.createDip("uuid:item");
        filter.filter(dip);
        IndexDocumentBean idb = dip.getDocument();

        assertEquals("Two statuses expected for aggregate object", 2, idb.getContentStatus().size());
        assertTrue("Object incorrectly labeled as described",
                idb.getContentStatus().contains(FacetConstants.CONTENT_NOT_DESCRIBED));
        assertTrue("Aggregate should not have a default web object assigned",
                idb.getContentStatus().contains(FacetConstants.CONTENT_NO_DEFAULT_OBJECT));
    }

    @Test
    public void testAggregateWithDWOQuery() throws Exception {

        triples.put(CDRProperty.defaultWebObject.toString(),
                Arrays.asList("dwo"));

        triples.put(FedoraProperty.hasModel.toString(),
                Arrays.asList(Model.AGGREGATE_WORK.toString()));

        DocumentIndexingPackage dip = factory.createDip("uuid:item");
        filter.filter(dip);
        IndexDocumentBean idb = dip.getDocument();

        assertEquals("Two statuses expected for aggregate object", 2, idb.getContentStatus().size());
        assertTrue("Object incorrectly labeled as described",
                idb.getContentStatus().contains(FacetConstants.CONTENT_NOT_DESCRIBED));
        assertTrue("Aggregate should not have a default web object assigned",
                idb.getContentStatus().contains(FacetConstants.CONTENT_DEFAULT_OBJECT));
    }
}
