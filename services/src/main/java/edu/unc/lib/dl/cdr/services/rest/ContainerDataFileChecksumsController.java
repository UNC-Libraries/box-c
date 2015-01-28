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
import java.util.HashMap;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.unc.lib.dl.search.solr.util.SolrSettings;

@Controller
public class ContainerDataFileChecksumsController {

	private HttpSolrServer server;

	@Autowired
	protected SolrSettings solrSettings;

	public synchronized void initializeSolrServer() {
		if (server == null)
			server = new HttpSolrServer(solrSettings.getUrl());
	}

	@RequestMapping(value = "/edit/fileinfo/{pid}")
	public void get(@PathVariable("pid") String pid,
			HttpServletResponse response) throws SolrServerException,
			IOException {
		String fid = pid.replace(":", "_");
		response.addHeader("Content-Disposition", "attachment; filename=\""
				+ fid + "-fileinfo.csv\"");
		response.addHeader("Content-Type", "text/csv");
		try (ServletOutputStream out = response.getOutputStream()) {
			out.print("title");
			out.print(',');
			out.print("pid");
			out.print(',');
			out.print("mimetype");
			out.print(',');
			out.print("length");
			out.print(',');
			out.println("checksum");

			if (server == null)
				initializeSolrServer();
			SolrQuery parameters = new SolrQuery();
			parameters.setQuery("contentModel:"+ClientUtils.escapeQueryChars("info:fedora/cdr-model:Simple")
					+" AND ancestorPath:*"+ClientUtils.escapeQueryChars(","+pid+",")+"*");
			parameters.addSort("filesizeTotal", ORDER.desc);
			parameters.addField("title");
			parameters.addField("id");
			parameters.addField("datastream");
			parameters.setRows(2000);
			QueryResponse solrResponse = server.query(parameters);

			for (SolrDocument doc : solrResponse.getResults()) {
				Map<String, String> line = new HashMap<String, String>();
				line.put("pid", (String) doc.getFieldValue("id"));
				line.put("title", (String) doc.getFieldValue("title"));
				String[] dsValues = new String[5];
				for (Object ds : doc.getFieldValues("datastream")) {
					String dstr = (String) ds;
					if(dstr.startsWith("DATA_FILE|")) {
						dsValues = dstr.split(Pattern.quote("|"));
						break;
					}
				}
				line.put("md5sum", dsValues[4]);
				line.put("length", dsValues[3]);
				line.put("mimetype", dsValues[1]);
				outputCSV(line, out);
			}
		}
	}

	private void outputCSV(Map<String, String> map, ServletOutputStream out)
			throws IOException {
		// title, pid, mimetype, length, checksum
		String title = (String) map.get("title");
		title = title.replaceAll(Pattern.quote("\""),
				Matcher.quoteReplacement("\"\""));
		out.print('"');
		out.print(title);
		out.print('"');
		out.print(',');
		out.print(map.get("pid"));
		out.print(',');
		out.print(map.get("mimetype"));
		out.print(',');
		out.print(map.get("length"));
		out.print(',');
		out.println(map.get("md5sum"));		
	}
}
