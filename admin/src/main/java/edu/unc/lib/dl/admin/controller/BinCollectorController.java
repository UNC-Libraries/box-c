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

import static edu.unc.lib.dl.admin.collect.DepositBinCollector.EXTRA_DEPOSITOR_EMAIL;
import static edu.unc.lib.dl.admin.collect.DepositBinCollector.EXTRA_DEPOSITOR_NAME;
import static edu.unc.lib.dl.admin.collect.DepositBinCollector.EXTRA_OWNER_NAME;
import static edu.unc.lib.dl.admin.collect.DepositBinCollector.EXTRA_PERMISSION_GROUPS;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.THUMB_LARGE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.fcrepo3.ObjectAccessControlsBeanImpl;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.admin.collect.DepositBinCollector;
import edu.unc.lib.dl.admin.collect.DepositBinCollector.ListFilesResult;
import edu.unc.lib.dl.admin.collect.DepositBinConfiguration;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

/**
 * @author bbpennel
 * @date Jul 10, 2014
 */
@Controller
public class BinCollectorController {
    private static final Logger log = LoggerFactory.getLogger(BinCollectorController.class);

    @Autowired
    private DepositBinCollector binCollector;

    @Autowired(required = true)
    protected SolrQueryLayerService queryLayer;
    @Autowired
    protected SearchSettings searchSettings;
    @Autowired
    private AccessControlService aclService;

    private final List<String> resultsFieldList = Arrays.asList(SearchFieldKeys.ID.name(), SearchFieldKeys.TITLE.name(),
            SearchFieldKeys.DATASTREAM.name(), SearchFieldKeys.RESOURCE_TYPE.name(),
            SearchFieldKeys.ANCESTOR_PATH.name(),SearchFieldKeys.ROLE_GROUP.name(),
            SearchFieldKeys.RELATIONS.name(), SearchFieldKeys.PARENT_COLLECTION.name());

    @RequestMapping(value = "collector", method = RequestMethod.GET)
    public String listCollectors() {
        return "collector/listBins";
    }

    @RequestMapping(value = "collector/details/{key}", method = RequestMethod.GET)
    public String binDetails() {
        return "collector/listBins";
    }

    @RequestMapping(value = "collector/list", method = RequestMethod.GET)
    public @ResponseBody
    Object listCollectors(Model model) {

        AccessGroupSet groups = GroupsThreadStore.getGroups();

        // Get the list of collectors
        Map<String, DepositBinConfiguration> configs = binCollector.getBinConfigurations();

        // Filter list down to just those that the user has rights to
        List<Object> response = new ArrayList<Object>();

        Iterator<Entry<String, DepositBinConfiguration>> configIt = configs.entrySet().iterator();
        while (configIt.hasNext()) {
            Entry<String, DepositBinConfiguration> configEntry = configIt.next();
            DepositBinConfiguration config = configEntry.getValue();

            PID destPID = new PID(config.getDestination());

            SimpleIdRequest entryRequest = new SimpleIdRequest(destPID.getPid(), resultsFieldList, groups);
            BriefObjectMetadata entryBean = queryLayer.getObjectById(entryRequest);

            // Only select collectors where the user can ingest to the destination container
            ObjectAccessControlsBean aclBean =
                    new ObjectAccessControlsBeanImpl(entryBean.getPid(), entryBean.getRoleGroup());

            boolean hasPermission = aclBean.hasPermission(groups, Permission.addRemoveContents);
            if (!hasPermission) {
                continue;
            }

            Map<String, Object> collectorEntry = new HashMap<String, Object>();
            collectorEntry.put("key", configEntry.getKey());
            collectorEntry.put("name", config.getName());
            collectorEntry.put("destPID", config.getDestination());
            collectorEntry.put("destTitle", entryBean.getTitle());

            // If the destination was not a collection then find the record for its parent collection
            BriefObjectMetadata collectionBean = entryBean;
            if (!searchSettings.resourceTypeCollection.equals(entryBean.getResourceType())
                    && entryBean.getParentCollection() != null) {
                entryRequest = new SimpleIdRequest(entryBean.getParentCollection(), resultsFieldList, groups);
                collectionBean = queryLayer.getObjectById(entryRequest);
                collectorEntry.put("collectionTitle", collectionBean.getTitle());
            }

            Datastream thumbDS = entryBean.getDatastreamObject(THUMB_LARGE.getName());
            if (thumbDS != null) {
                String thumbPID = thumbDS.getOwner() == null ? entryBean.getId() : thumbDS.getOwner();

                collectorEntry.put("collectionThumb", thumbPID);
            }

            // Get statistics for remaining collectors
            ListFilesResult listFilesResult = binCollector.listFiles(configEntry.getKey());
            collectorEntry.put("applicableCount", listFilesResult.applicable.size());
            collectorEntry.put("nonapplicableCount", listFilesResult.nonapplicable.size());

            response.add(collectorEntry);
        }

        return response;
    }

    @RequestMapping(value = "collector/bin/{key}", method = RequestMethod.GET)
    public @ResponseBody
    Object viewCollector(@PathVariable("key") String binKey, Model model, HttpServletResponse resp) {
        DepositBinConfiguration config = binCollector.getConfiguration(binKey);

        if (config == null) {
            log.debug("Invalid bin key provided {}", binKey);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        // Check permissions
        if (!hasPermission(config.getDestination())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("key", binKey);
        response.put("name", config.getName());
        response.put("destPID", config.getDestination());
        response.put("binPaths", config.getPaths());

        ListFilesResult listFilesResult = binCollector.listFiles(binKey);
        response.put("applicableFiles", serializeFileList(listFilesResult.applicable));
        response.put("nonapplicableFiles", serializeFileList(listFilesResult.nonapplicable));

        return response;
    }

    private List<Object> serializeFileList(List<File> fileList) {
        List<Object> result = new ArrayList<Object>(fileList.size());
        for (File file : fileList) {
            Map<String, String> entry = new HashMap<String, String>();
            entry.put("path", file.getAbsolutePath());
            entry.put("size", Long.toString(file.length()));
            entry.put("time", Long.toString(file.lastModified()));

            result.add(entry);
        }

        return result;
    }

    @RequestMapping(value = "collector/bin/{key}", method = RequestMethod.POST)
    public @ResponseBody
    Object startCollection(@PathVariable("key") String binKey, HttpServletResponse resp,
            @RequestParam(value = "files[]", required = false) String[] fileList, Model model) {

        DepositBinConfiguration config = binCollector.getConfiguration(binKey);

        if (fileList == null || fileList.length == 0) {
            return null;
        }

        if (config == null) {
            log.debug("Invalid bin key provided {}", binKey);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        // Check permissions
        if (!hasPermission(config.getDestination())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        Map<String, String> extras = new HashMap<String,String>();
        extras.put(EXTRA_DEPOSITOR_NAME, GroupsThreadStore.getUsername());
        extras.put(EXTRA_OWNER_NAME, GroupsThreadStore.getUsername());
        extras.put(EXTRA_DEPOSITOR_EMAIL, GroupsThreadStore.getEmail());
        extras.put(EXTRA_PERMISSION_GROUPS, GroupsThreadStore.getGroupString());

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("key", binKey);
        response.put("name", config.getName());

        try {
            List<String> files = Arrays.asList(fileList);
            binCollector.collect(files, binKey, extras);
            response.put("success", true);
        } catch (IOException e) {
            response.put("success", false);
            response.put("error", e.toString());
            log.error("User {} failed to collect items from {}",
                    new Object[] { GroupsThreadStore.getUsername(), binKey, e });
        }

        return response;
    }

    private boolean hasPermission(String destination) {
        ObjectAccessControlsBean aclBean = aclService.getObjectAccessControls(new PID(destination));
        AccessGroupSet groups = GroupsThreadStore.getGroups();

        return aclBean.hasPermission(groups, Permission.addRemoveContents);
    }
}
