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
package edu.unc.lib.dl.data.ingest.solr.indexing;

import java.util.Collection;
import java.util.List;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.filter.IndexDocumentFilter;

public class DocumentIndexingPipeline implements DocumentFilteringPipeline {
	protected Collection<IndexDocumentFilter> filters;
	
	@Override
	public void process(DocumentIndexingPackage dip) throws IndexingException {
		for (IndexDocumentFilter filter: filters) {
			filter.filter(dip);
		}
	}

	@Override
	public void setFilters(List<IndexDocumentFilter> filters) {
		this.filters = filters;
	}
}