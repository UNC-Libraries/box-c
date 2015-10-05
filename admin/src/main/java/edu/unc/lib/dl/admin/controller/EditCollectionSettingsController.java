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
package edu.unc.lib.dl.admin.controller;

import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.RELS_EXT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.fedora.DatastreamDocument;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.OptimisticLockException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.ContainerSettings;
import edu.unc.lib.dl.model.ContainerSettings.ContainerView;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.RDFXMLUtil;

/**
 * @author bbpennel
 * @date Aug 24, 2015
 */
@Controller
public class EditCollectionSettingsController {
	private static final Logger log = LoggerFactory.getLogger(EditCollectionSettingsController.class);

	@Resource(name="forwardedManagementClient")
	private ManagementClient client;
	@Autowired
	private TripleStoreQueryService tripleStoreQueryService;
	@Autowired
	private SolrQueryLayerService solrQueryService;
	
	@RequestMapping(value = "editCollection/{pid}", method = RequestMethod.GET)
	public @ResponseBody ContainerSettings getSettings(@PathVariable("pid") String pidString) {
		Map<String, List<String>> triples = tripleStoreQueryService.fetchAllTriples(new PID(pidString));
		if (triples == null || triples.size() == 0) {
			return null;
		}
		
		SimpleIdRequest solrRequest = new SimpleIdRequest(pidString);
		solrRequest.setResultFields(Arrays.asList(SearchFieldKeys.TITLE.name()));
		BriefObjectMetadata data = solrQueryService.getObjectById(solrRequest);
		
		ContainerSettings settings = new ContainerSettings(triples);
		settings.setTitle(data.getTitle());
		
		// Provide default values if they have not been specified before
		if (settings.getDefaultView() == null) {
			settings.setDefaultView(ContainerView.METADATA.name());
		}
		if (settings.getViews() == null || settings.getViews().size() == 0) {
			settings.setViews(Arrays.asList(ContainerView.METADATA.name(), ContainerView.STRUCTURE.name()));
		}
		
		return settings;
	}
	
	@RequestMapping(value = "editCollection/{pid}", method = RequestMethod.POST)
	public @ResponseBody Map<Object, Object> editSettings(@PathVariable("pid") String pidString,
			@RequestBody EditCollectionSettingsRequest data) {
		Map<Object, Object> result = new HashMap<>();
		
		PID pid = new PID(pidString);
		do {
			try {
				DatastreamDocument relsDoc = client.getXMLDatastreamIfExists(pid, Datastream.RELS_EXT.getName());
				Element rdfEl = relsDoc.getDocument().getRootElement();
				
				// Replace the default view
				if (data.getDefaultView() == null) {
					RDFXMLUtil.removeLiteral(rdfEl, CDRProperty.collectionDefaultView.getPredicate(),
							CDRProperty.collectionDefaultView.getNamespace(), null, null);
				} else {
					RDFXMLUtil.setExclusiveLiteral(rdfEl, CDRProperty.collectionDefaultView.getPredicate(),
							CDRProperty.collectionDefaultView.getNamespace(), data.getDefaultView(), null);
				}
				
				// Replace the views selected for this collection
				RDFXMLUtil.removeLiteral(rdfEl, CDRProperty.collectionShowView.getPredicate(),
						CDRProperty.collectionShowView.getNamespace(), null, null);
				if (data.getViews() != null) {
					for (String view : data.getViews()) {
						if (ContainerView.valueOf(view) != null) {
							RDFXMLUtil.addTriple(rdfEl, CDRProperty.collectionShowView.getPredicate(),
									CDRProperty.collectionShowView.getNamespace(), true, view, null);
						}
					}
				}
				
				client.modifyDatastream(pid, RELS_EXT.getName(),
						"Setting exclusive relation", relsDoc.getLastModified(), relsDoc.getDocument());
				
				return result;
			} catch (OptimisticLockException e) {
				log.debug("Unable to update RELS-EXT for {}, retrying", pid, e);
			} catch (FedoraException e) {
				
			}
		} while (true);
	}
	
	public static class EditCollectionSettingsRequest {
		private String defaultView;
		private List<String> views;

		public String getDefaultView() {
			return defaultView;
		}

		public void setDefaultView(String defaultView) {
			this.defaultView = defaultView;
		}

		public List<String> getViews() {
			return views;
		}

		public void setViews(List<String> views) {
			this.views = views;
		}
	}
}
