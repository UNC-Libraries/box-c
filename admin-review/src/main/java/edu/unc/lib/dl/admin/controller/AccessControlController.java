package edu.unc.lib.dl.admin.controller;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SolrSettings;
import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

@Controller
public class AccessControlController extends AbstractSwordController {
	private static final Logger log = LoggerFactory.getLogger(AccessControlController.class);

	@Autowired
	SolrSettings solrSettings;
	@Autowired
	private String swordUrl;
	@Autowired
	private String swordUsername;
	@Autowired
	private String swordPassword;

	private List<String> targetResultFields = Arrays.asList(SearchFieldKeys.ID.name(), SearchFieldKeys.TITLE.name(),
			SearchFieldKeys.STATUS.name(), SearchFieldKeys.ROLE_GROUP.name(), SearchFieldKeys.ANCESTOR_PATH.name());

	private List<String> parentResultFields = Arrays.asList(SearchFieldKeys.ID.name(), SearchFieldKeys.STATUS.name(),
			SearchFieldKeys.ROLE_GROUP.name());
	
	private String[] accessGroupFields;
	
	@PostConstruct
	public void init() {
		accessGroupFields = new String[] { solrSettings.getFieldName(SearchFieldKeys.ADMIN_GROUP.name()),
				solrSettings.getFieldName(SearchFieldKeys.READ_GROUP.name()) };
	}

	@RequestMapping(value = "acl/{prefix}/{id}", method = RequestMethod.GET)
	public String getAccessControl(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletResponse response) {
		String pid = idPrefix + ":" + id;

		model.addAttribute("pid", pid);

		// Retrieve ancestor information about the targeted object
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
		SimpleIdRequest objectRequest = new SimpleIdRequest(pid, targetResultFields, accessGroups);
		BriefObjectMetadataBean targetObject = queryLayer.getObjectById(objectRequest);
		if (targetObject == null)
			throw new InvalidRecordRequestException();
		model.addAttribute("targetMetadata", targetObject);

		// Get access information for the target's parent
		objectRequest = new SimpleIdRequest(targetObject.getAncestorPathFacet().getSearchKey(), parentResultFields,
				accessGroups);
		BriefObjectMetadataBean parentObject = queryLayer.getObjectById(objectRequest);
		if (parentObject == null)
			throw new InvalidRecordRequestException();
		model.addAttribute("parentMetadata", parentObject);

		// Retrieve the targeted objects directly attributed ACL document
		String dataUrl = swordUrl + "em/" + pid + "/ACL";
		HttpClient client = HttpClientUtil.getAuthenticatedClient(dataUrl, swordUsername, swordPassword);
		client.getParams().setAuthenticationPreemptive(true);
		GetMethod method = new GetMethod(dataUrl);
		// Pass the users groups along with the request
		AccessGroupSet groups = GroupsThreadStore.getGroups();
		method.addRequestHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, groups.joinAccessGroups(";"));

		Element accessControlElement;
		try {
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				String accessControlXML = method.getResponseBodyAsString();
				log.warn(accessControlXML);
				SAXBuilder saxBuilder = new SAXBuilder();
				accessControlElement = saxBuilder.build(new StringReader(accessControlXML)).getRootElement();
				model.addAttribute("accessControlXML", accessControlXML);
				model.addAttribute("targetACLs", accessControlElement);
			} else {
				log.error("Failed to retrieve access control document for " + pid + ": "
						+ method.getStatusLine().toString());
				response.setStatus(method.getStatusCode());
				return null;
			}
		} catch (Exception e) {
			response.setStatus(500);
			log.error("Failed to retrieve access control document for " + pid, e);
			return null;
		} finally {
			if (method != null)
				method.releaseConnection();
		}

		Map<String, List<RoleGrant>> rolesGranted = new LinkedHashMap<String, List<RoleGrant>>();
		for (Object elementObject : accessControlElement.getChildren()) {
			Element childElement = (Element) elementObject;
			if (childElement.getNamespace().equals(JDOMNamespaceUtil.CDR_ACL_NS)) {
				String group = childElement.getAttributeValue("group", JDOMNamespaceUtil.CDR_ACL_NS);
				String role = childElement.getAttributeValue("role", JDOMNamespaceUtil.CDR_ACL_NS);

				List<RoleGrant> groupList = rolesGranted.get(role);
				if (groupList == null) {
					groupList = new ArrayList<RoleGrant>();
					rolesGranted.put(role, groupList);
				}
				groupList.add(new RoleGrant(group, false));
			}
		}

		for (String parentRoles : parentObject.getRoleGroup()) {
			String[] roleParts = parentRoles.split("\\|");
			if (roleParts.length < 2)
				continue;
			String role = roleParts[0];
			role = role.split("#")[1];

			List<RoleGrant> groupList = rolesGranted.get(role);
			if (groupList == null) {
				groupList = new ArrayList<RoleGrant>();
				rolesGranted.put(role, groupList);
			}
			String group = roleParts[1];
			// If the map already contains this group, then it is marked explicitly as not inherited
			groupList.add(new RoleGrant(group, true));
		}

		model.addAttribute("rolesGranted", rolesGranted);

		model.addAttribute("template", "ajax");
		return "edit/accessControl";
	}

	@RequestMapping(value = "acl/{prefix}/{id}", method = RequestMethod.PUT)
	public @ResponseBody
	String saveAccessControl(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id,
			HttpServletRequest request, HttpServletResponse response) {

		String pid = idPrefix + ":" + id;
		String datastream = "ACL";

		return this.updateDatastream(pid, datastream, request, response);
	}

	@RequestMapping(value = "acl/getGroups", method = RequestMethod.GET)
	public @ResponseBody
	Collection<String> getAllAccessGroups() throws AccessRestrictionException, SolrServerException {
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
		return this.queryLayer.getDistinctFieldValues(accessGroupFields, 500, accessGroups);
	}

	public static class RoleGrant {
		public String roleName;
		public boolean inherited;

		public RoleGrant(String roleName, boolean inherited) {
			this.roleName = roleName;
			this.inherited = inherited;
		}

		public String getRoleName() {
			return roleName;
		}

		public boolean isInherited() {
			return inherited;
		}
	}
}
