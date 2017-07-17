/**
 * Copyright © 2008 The University of North Carolina at Chapel Hill (cdr@unc.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.solr;

import edu.unc.lib.dl.cdr.services.AbstractFedoraEnhancementService;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;

/**
 * 
 * @author count0
 *
 */
public abstract class AbstractSolrObjectEnhancementService extends AbstractFedoraEnhancementService {
    protected SolrSearchService solrSearchService;

    public SolrSearchService getSolrSearchService() {
        return solrSearchService;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }
}
