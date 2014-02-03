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

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.exception.UnsupportedContentModelException;
import edu.unc.lib.dl.data.ingest.solr.filter.IndexDocumentFilter;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.JDOMQueryUtil;

public class DocumentIndexingPipeline implements DocumentFilteringPipeline {
	protected Collection<IndexDocumentFilter> filters;

	@Override
	public void process(DocumentIndexingPackage dip) throws IndexingException {
		if (dip.getRelsExt() != null) {
			@SuppressWarnings("unchecked")
			List<org.jdom.Element> hasModelElements = dip.getRelsExt().getChildren(
					ContentModelHelper.FedoraProperty.hasModel.name(), JDOMNamespaceUtil.FEDORA_MODEL_NS);
			if (JDOMQueryUtil.getElementByAttribute(hasModelElements, "resource", JDOMNamespaceUtil.RDF_NS,
					ContentModelHelper.Model.DEPOSIT_RECORD.toString()) != null)
				throw new UnsupportedContentModelException("Could not index object " + dip.getPid().toString()
						+ ", objects of type " + ContentModelHelper.Model.DEPOSIT_RECORD.toString()
						+ " are not supported for indexing.");
		}

		for (IndexDocumentFilter filter : filters) {
			filter.filter(dip);
		}
	}

	@Override
	public void setFilters(List<IndexDocumentFilter> filters) {
		this.filters = filters;
	}
}