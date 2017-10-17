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

import java.io.IOException;
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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
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

/**
 *
 * @author bbpennel
 *
 */
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

    private final List<String> targetResultFields = Arrays.asList(SearchFieldKeys.ID.name(),
            SearchFieldKeys.TITLE.name(), SearchFieldKeys.STATUS.name(), SearchFieldKeys.ROLE_GROUP.name(),
            SearchFieldKeys.ANCESTOR_PATH.name());

    private final List<String> parentResultFields = Arrays.asList(SearchFieldKeys.ID.name(),
            SearchFieldKeys.STATUS.name(), SearchFieldKeys.ROLE_GROUP.name());

    private String[] accessGroupFields;

    @PostConstruct
    public void init() {
        accessGroupFields = new String[] { solrSettings.getFieldName(SearchFieldKeys.ADMIN_GROUP.name()),
                solrSettings.getFieldName(SearchFieldKeys.READ_GROUP.name()) };
    }

    @RequestMapping(value = "acl/{pid}", method = RequestMethod.GET)
    public String getAccessControl(@PathVariable("pid") String pid, Model model, HttpServletResponse response) {
        model.addAttribute("pid", pid);

        // Retrieve ancestor information about the targeted object
        AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
        SimpleIdRequest objectRequest = new SimpleIdRequest(pid, targetResultFields, accessGroups);
        BriefObjectMetadataBean targetObject = queryLayer.getObjectById(objectRequest);
        if (targetObject == null) {
            throw new InvalidRecordRequestException();
        }
        model.addAttribute("targetMetadata", targetObject);

        // Get access information for the target's parent
        BriefObjectMetadataBean parentObject = null;
        if (targetObject.getAncestorPathFacet() != null) {
            objectRequest = new SimpleIdRequest(targetObject.getAncestorPathFacet().getSearchKey(), parentResultFields,
                    accessGroups);
            parentObject = queryLayer.getObjectById(objectRequest);
            if (parentObject == null) {
                throw new InvalidRecordRequestException();
            }
            model.addAttribute("parentMetadata", parentObject);
        }

        // Retrieve the targeted objects directly attributed ACL document
        String dataUrl = swordUrl + "em/" + pid + "/ACL";
        CloseableHttpClient client = HttpClientUtil.getAuthenticatedClient(null, swordUsername, swordPassword);
        HttpGet method = new HttpGet(dataUrl);
        // Pass the users groups along with the request
        AccessGroupSet groups = GroupsThreadStore.getGroups();
        method.addHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, groups.joinAccessGroups(";"));

        Element accessControlElement;
        try (CloseableHttpResponse httpResp = client.execute(method)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                String accessControlXML = EntityUtils.toString(httpResp.getEntity(), "UTF-8");
                log.debug(accessControlXML);
                SAXBuilder saxBuilder = new SAXBuilder();
                accessControlElement = saxBuilder.build(new StringReader(accessControlXML)).getRootElement();
                model.addAttribute("accessControlXML", accessControlXML);
                model.addAttribute("targetACLs", accessControlElement);
            } else {
                log.error("Failed to retrieve access control document for " + pid + ": " + httpResp.getStatusLine());
                response.setStatus(statusCode);
                return null;
            }
        } catch (IOException | JDOMException e) {
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Failed to retrieve access control document for " + pid, e);
            return null;
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

        if (parentObject != null) {
            for (String parentRoles : parentObject.getRoleGroup()) {
                String[] roleParts = parentRoles.split("\\|");
                if (roleParts.length < 2) {
                    continue;
                }
                String role = roleParts[0];

                List<RoleGrant> groupList = rolesGranted.get(role);
                if (groupList == null) {
                    groupList = new ArrayList<RoleGrant>();
                    rolesGranted.put(role, groupList);
                }
                String group = roleParts[1];
                // If the map already contains this group, then it is marked
                // explicitly as not inherited
                groupList.add(new RoleGrant(group, true));
            }
        }

        model.addAttribute("rolesGranted", rolesGranted);

        model.addAttribute("template", "ajax");
        return "edit/accessControl";
    }

    @RequestMapping(value = "acl/{pid}", method = RequestMethod.PUT)
    public @ResponseBody String saveAccessControl(@PathVariable("pid") String pid, HttpServletRequest request,
            HttpServletResponse response) {
        String datastream = "ACL";

        return this.updateDatastream(pid, datastream, request, response);
    }

    @RequestMapping(value = "acl/getGroups", method = RequestMethod.GET)
    public @ResponseBody Collection<String> getAllAccessGroups()
            throws AccessRestrictionException, SolrServerException {
        AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
        return this.queryLayer.getDistinctFieldValues(accessGroupFields, HttpStatus.SC_INTERNAL_SERVER_ERROR,
                accessGroups);
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
