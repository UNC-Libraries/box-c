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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.beans.Field;
import org.jdom2.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.jdom2.output.XMLOutputter;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.FedoraDataService;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;


@Controller
@RequestMapping("exportxml")
public class ExportXMLController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(ExportXMLController.class);
	
	@Autowired
	private JavaMailSender mailSender;
	@Resource
	@Qualifier("forwardedAccessClient")
	private AccessClient client;
	
	
	@RequestMapping(value = "{pid}", method = RequestMethod.GET)
	public @ResponseBody 
	Object export(@PathVariable("pid") final String pid, final HttpServletRequest request) throws IOException, FedoraException {
		
		Runnable xmlGenerationRunnable = new Runnable() {
			@Override
			public void run() {
				try {
					SearchRequest searchRequest = generateSearchRequest(request, searchStateFactory.createSearchState());
					
					SearchState searchState = searchRequest.getSearchState();
					searchState.setResultFields(Arrays.asList(SearchFieldKeys.ID.name(), SearchFieldKeys.TITLE.name(),
							SearchFieldKeys.RESOURCE_TYPE.name(), SearchFieldKeys.ANCESTOR_IDS.name(),
							SearchFieldKeys.STATUS.name(), SearchFieldKeys.DATASTREAM.name(),
							SearchFieldKeys.ANCESTOR_PATH.name(), SearchFieldKeys.CONTENT_MODEL.name(),
							SearchFieldKeys.DATE_ADDED.name(), SearchFieldKeys.DATE_UPDATED.name(),
							SearchFieldKeys.LABEL.name()));
					searchState.setSortType("export");
					searchState.setRowsPerPage(searchSettings.maxPerPage);
					
					BriefObjectMetadata container = queryLayer.addSelectedContainer(pid, searchState, false);
					SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);
					
					List<BriefObjectMetadata> objects = resultResponse.getResultList();
					objects.add(0, container);
					queryLayer.getChildrenCounts(objects, searchRequest);
					
					String xmlString = "<?xml version=\"1.0\" encoding=\"utf-8\"?><bulkMetadata xmlns:mods=\"http://www.loc.gov/mods/v3\" xmlns:acl=\"http://cdr.unc.edu/definitions/acl\">";
					 			
					for (BriefObjectMetadata object : objects) {
						try{
							byte[] modsBytes = client.getDatastreamDissemination(object.getPid(), "MD_DESCRIPTIVE", null).getStream();
							Document mods = edu.unc.lib.dl.fedora.ClientUtils.parseXML(modsBytes);
							xmlString += "<object pid=\""+ object.getPid().getPid() +"\"><update type=\"MODS\">" + new XMLOutputter().outputString(mods).split(">", 2)[1] + "</update></object>";
							
						} catch(Exception e) {
							LOG.error("Failed to generate XMl in thread for ", pid, e);
						}
					}
					xmlString += "</bulkMetadata>";
					sendEmail(xmlString, "sreenu@live.unc.edu");
				} catch(Exception e) {
					LOG.error("Failed to generate XMl in thread for ", pid, e);
				}
			}
		};

		Thread thread = new Thread(xmlGenerationRunnable);
		thread.start();
		
		Map <String, String> response = new HashMap<>();
		response.put("message", "You will receive an email with XML Data shortly");
		return response;
	}
	
	public void sendEmail(String xmlString, String toEmail) {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);
			
			helper.setSubject("XML File creation complete");
			helper.setFrom("no-reply@example.com");
			helper.setText("Attached the XML");
			helper.setTo(toEmail);
			helper.addAttachment("cdr.xml",
			        new ByteArrayResource(xmlString.getBytes(Charset.forName("UTF-8"))));
			this.mailSender.send(mimeMessage);
			LOG.debug("sending email");
		} catch (MessagingException e) {
			LOG.error("Cannot send notification email", e);
		}
	}
}
