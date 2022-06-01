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
package edu.unc.lib.boxc.search.solr.services;

import edu.unc.lib.boxc.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import org.apache.solr.common.SolrInputDocument;

import java.util.List;

/**
 * @author bbpennel
 */
public class FacetValuesServiceIT extends BaseEmbeddedSolrTest {

    // single page of results
    // multiple pages
    // sort index
    // sort count
    // filtered by root id
    // filtered by keyword
    // returns no values
    // invalid sort
    // invalid start
    // invalid number of rows

    public class FacetValueTestCorpus extends TestCorpus {
        @Override
        public List<SolrInputDocument> populate() {
            super.populate();
        }
    }
}
