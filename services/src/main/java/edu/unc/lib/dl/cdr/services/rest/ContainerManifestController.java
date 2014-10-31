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
package edu.unc.lib.dl.cdr.services.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.search.solr.util.SolrSettings;
import edu.unc.lib.dl.util.TripleStoreQueryService;

@Controller
public class ContainerManifestController {
	private static final Logger LOG = LoggerFactory.getLogger(ContainerManifestController.class);
		
	private HttpSolrServer server;
	
	@Autowired
	protected SolrSettings solrSettings;
	
	@Autowired
	private TripleStoreQueryService tripleStoreQueryService;
	
	public synchronized void initializeSolrServer() {
		if(server == null) server = new HttpSolrServer(solrSettings.getUrl());
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/edit/manifest-json/{pid}")
	public @ResponseBody Map<String, Object> getJSON(@PathVariable("pid") String pid) throws SolrServerException {
		if(server == null) initializeSolrServer();
		SolrQuery parameters = new SolrQuery();
		parameters.setQuery("ancestorPath:*"+ClientUtils.escapeQueryChars(","+pid+",")+"*");
		parameters.addSort("ancestorNames", ORDER.asc);
		parameters.addSort("isPart", ORDER.asc);
		parameters.addSort("displayOrder", ORDER.asc);
		parameters.addField("ancestorNames");
		parameters.addField("ancestorPath");
		parameters.addField("id");
		parameters.addField("filesizeTotal");
		parameters.addField("title");
		QueryResponse solrResponse = server.query(parameters);
		
		// objects map is a local index of pid to map
		Map<String, Map<String, Object>> objects = new HashMap<String, Map<String, Object>>();
		Map<String, Object> coll = new HashMap<String, Object>();
		coll.put("pid", pid);
		String title = tripleStoreQueryService.lookupLabel(pid);
		coll.put("title", title);
		objects.put(pid, coll);
		for(SolrDocument doc : solrResponse.getResults()) {
			Map<String, Object> line = new HashMap<String, Object>();
			objects.put((String)doc.getFieldValue("id"), line);

			Collection<Object> ancestorPaths = doc.getFieldValues("ancestorPath");
			line.put("depth", String.valueOf(ancestorPaths.size()));
			
			// get parent id
			String[] ancestorPathsArray = ancestorPaths.toArray(new String[] {});
			String lastAncestor = ancestorPathsArray[ancestorPathsArray.length-1];
			int start = lastAncestor.indexOf(",")+1;
			int end = lastAncestor.indexOf(",", start);
			String parent = lastAncestor.substring(start, end);
			
			// file object record
			line.put("pid", doc.getFieldValue("id"));
			line.put("parentPid", parent);
			line.put("title", doc.getFieldValue("title"));
			line.put("filesizeTotal", doc.getFieldValue("filesizeTotal"));
			// TODO get checksum of data file
		}
		
		for(Map<String, Object> record : objects.values()) {
			
			if(pid.equals(record.get("pid"))) continue;
			String parentPid = (String) record.get("parentPid");
			// file object as child
			Map<String, Object> parentHash = objects.get(parentPid);
			if(parentHash == null) {
				LOG.warn("Cannot find expected pid in index: {}", parentPid);
				continue;
			}
			List<Map<String, Object>> children = null;
			if(!parentHash.containsKey("children")) {
				children = new ArrayList<Map<String, Object>>();
				parentHash.put("children", children);
			} else {
				children = (List<Map<String, Object>>)parentHash.get("children");
			}
			children.add(record);
		}
		return coll;
	}
	
	@RequestMapping(value = "/edit/manifest-csv/{pid}")
	public void downloadCSV(@PathVariable("pid") String pid, HttpServletResponse response) throws SolrServerException, IOException {
		if(server == null) initializeSolrServer();
		SolrQuery parameters = new SolrQuery();
		parameters.setQuery("ancestorPath:*"+ClientUtils.escapeQueryChars(","+pid+",")+"*");
		parameters.addSort("ancestorNames", ORDER.asc);
		parameters.addSort("isPart", ORDER.asc);
		parameters.addSort("displayOrder", ORDER.asc);
		parameters.addField("ancestorPath");
		parameters.addField("ancestorNames");
		parameters.addField("id");
		parameters.addField("title");
		QueryResponse solrResponse = server.query(parameters);
		
		String id = pid.replace(":", "_");
		response.addHeader("Content-Disposition", "attachment; filename=\""+id+"-manifest.csv\"");
		try(ServletOutputStream out = response.getOutputStream()) {
			out.print("depth");
			out.print(',');
			out.print("pid");
			out.print(',');
			out.println("title");
			for(SolrDocument doc : solrResponse.getResults()) {
				String title = (String) doc.getFieldValue("title");
				String p = (String) doc.getFieldValue("id");
				String anc = (String) doc.getFieldValue("ancestorNames");
				int depth = doc.getFieldValues("ancestorPath").size();
				outputCSV(p, title, depth, anc, out);
			}
		}
	}

	private void outputCSV(String pid, String title, int depth, String anc, ServletOutputStream out) throws IOException {
		out.print(depth);
		out.print(',');
		out.print("info:fedora/");
		out.print(pid);
		out.print(',');
		out.print('"');
		title = title.replaceAll(Pattern.quote("\""), Matcher.quoteReplacement("\"\""));
		out.print(title);
		out.println('"');
	}
}
