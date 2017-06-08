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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

/**
 *
 * @author bbpennel
 * @date Mar 12, 2014
 */
public class SetFullTextFilterTest extends Assert {

    @Mock
    private DocumentIndexingPackageDataLoader loader;
    @Mock
    private AccessClient accessClient;
    private DocumentIndexingPackageFactory factory;
    private DocumentIndexingPackage dip;
    @Mock
    private MIMETypedStream stream;
    Map<String, List<String>> triples;

    private SetFullTextFilter filter;

    @Before
    public void setup() throws Exception {

        initMocks(this);

        factory = new DocumentIndexingPackageFactory();
        factory.setDataLoader(loader);

        triples = new HashMap<>();

        when(loader.loadTriples(any(DocumentIndexingPackage.class))).thenReturn(triples);

        filter = new SetFullTextFilter();
        filter.setAccessClient(accessClient);

        when(stream.getStream()).thenReturn("Full text value".getBytes());
        when(accessClient.getDatastreamDissemination(any(PID.class), eq(Datastream.MD_FULL_TEXT.name()), anyString()))
                .thenReturn(stream);

        dip = factory.createDip("uuid:item");
    }

    @Test
    public void testFullText() throws Exception {
        triples.put(CDRProperty.fullText.toString(), Arrays.asList("pid/MD_FULL_TEXT"));

        filter.filter(dip);

        verify(stream).getStream();
        assertNotNull(dip.getDocument().getFullText());

    }

    @Test
    public void testNoFullText() throws Exception {
        filter.filter(dip);

        verify(stream, never()).getStream();
        assertNull(dip.getDocument().getFullText());

    }

}
