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
package edu.unc.lib.dl.fedora;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.util.TripleStoreQueryService.PathInfo;

/**
 * Fedora data retrieval class used for accessing data streams and performing Mulgara queries to generate XML views of
 * Fedora objects for outside usage.
 *
 * @author Gregory Jansen
 * @author Ben Pennell
 */
public class FedoraDataService {

    private static final Logger LOG = LoggerFactory.getLogger(FedoraDataService.class);

    private edu.unc.lib.dl.fedora.AccessClient accessClient = null;

    private edu.unc.lib.dl.fedora.ManagementClient managementClient = null;

    private String threadGroupPrefix = "";

    private ExecutorService executor;
    private int maxThreads;
    private long serviceTimeout;

    public FedoraDataService() {
        maxThreads = 0;
        serviceTimeout = 5000L;
    }

    public void init() {
        CustomizableThreadFactory ctf = new CustomizableThreadFactory();
        ctf.setThreadGroupName(threadGroupPrefix + "FDS");
        ctf.setThreadNamePrefix(threadGroupPrefix + "FDSWorker-");
        this.executor = Executors.newFixedThreadPool(maxThreads, ctf);
    }

    public void destroy() {
        executor.shutdownNow();
    }

    public edu.unc.lib.dl.fedora.AccessClient getAccessClient() {
        return accessClient;
    }

    public void setAccessClient(edu.unc.lib.dl.fedora.AccessClient accessClient) {
        this.accessClient = accessClient;
    }

    /**
     * Retrieves a view-inputs document containing the FOXML datastream for the object identified by simplepid
     *
     * @param simplepid
     * @return
     * @throws FedoraException
     */
    public Document getFoxmlViewXML(String simplepid) throws FedoraException {
        final PID pid = new PID(simplepid);
        Document result = new Document();
        final Element inputs = new Element("view-inputs");
        result.setRootElement(inputs);

        List<Callable<Content>> callables = new ArrayList<Callable<Content>>();
        callables.add(new GetFoxml(pid));

        this.retrieveAsynchronousResults(inputs, callables, pid, true);

        return result;
    }

    /**
     * Retrieves a view-inputs document containing the Mods datastream for the object identified by simplepid
     *
     * @param simplepid
     * @return
     * @throws FedoraException
     */
    public Document getModsViewXML(String simplepid) throws FedoraException {
        final PID pid = new PID(simplepid);
        Document result = new Document();
        final Element inputs = new Element("view-inputs");
        result.setRootElement(inputs);

        List<Callable<Content>> callables = new ArrayList<Callable<Content>>();
        callables.add(new GetMods(pid));

        this.retrieveAsynchronousResults(inputs, callables, pid, true);

        return result;
    }

    public Document getObjectViewXML(String simplepid) throws FedoraException {
        return getObjectViewXML(simplepid, false);
    }

    /**
     * Retrieves a view-inputs document containing the FOXML, Fedora path
     * information, parent collection pid, permissions and order within parent
     * folder for the object identified by simplepid
     *
     * @param simplepid
     * @return
     * @throws FedoraException
     */
    public Document getObjectViewXML(String simplepid, boolean failOnException) throws FedoraException {
        final PID pid = new PID(simplepid);
        Document result = new Document();

        final Element inputs = new Element("view-inputs");
        result.setRootElement(inputs);

        List<Callable<Content>> callables = new ArrayList<Callable<Content>>();

        callables.add(new GetFoxml(pid));
        callables.add(new GetPathInfo(pid));
        callables.add(new GetParentCollection(pid));
        //callables.add(new GetPermissions(pid));
        callables.add(new GetOrderWithinParent(pid));
        callables.add(new GetDefaultWebObject(pid));

        this.retrieveAsynchronousResults(inputs, callables, pid, failOnException);

        return result;
    }

    private void retrieveAsynchronousResults(Element inputs, List<Callable<Content>> callables, PID pid,
            boolean failOnException) throws FedoraException {
        Collection<Future<Content>> futures = new ArrayList<Future<Content>>(callables.size());

        if (GroupsThreadStore.getGroups() != null) {
            AccessGroupSet groups = GroupsThreadStore.getGroups();
            for (Callable<Content> c : callables) {
                if (GroupForwardingCallable.class.isInstance(c)) {
                    GroupForwardingCallable rfc = (GroupForwardingCallable)c;
                    rfc.setGroups(groups);
                }
            }
        }

        for (Callable<Content> callable: callables) {
            futures.add(executor.submit(callable));
        }

        for (Future<Content> future : futures) {
            try {
                Content results = future.get(serviceTimeout, TimeUnit.MILLISECONDS);
                if (results != null) {
                    inputs.addContent(results);
                }
            } catch (InterruptedException e) {
                LOG.warn("Attempt to get asynchronous results was interrupted for " + pid.getPid(), e);
                return;
            } catch (ExecutionException e) {
                if (failOnException) {
                    if (e.getCause() instanceof FedoraException) {
                        throw (FedoraException) e.getCause();
                    }
                    throw new ServiceException("Failed to get asynchronous results for " + pid.getPid(), e);
                }
                LOG.warn("Failed to get asynchronous results for " + pid.getPid() + ", continuing.", e);
            } catch (TimeoutException e) {
                if (failOnException) {
                    throw new ServiceException("Failed to get asynchronous results for " + pid.getPid(), e);
                }
                LOG.warn("Request for asynchronous results timed out for " + pid.getPid() + ", continuing.", e);
            }
        }
    }

    public edu.unc.lib.dl.fedora.ManagementClient getManagementClient() {
        return managementClient;
    }

    public void setManagementClient(edu.unc.lib.dl.fedora.ManagementClient managementClient) {
        this.managementClient = managementClient;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public long getServiceTimeout() {
        return serviceTimeout;
    }

    public void setServiceTimeout(long serviceTimeout) {
        this.serviceTimeout = serviceTimeout;
    }

    public String getThreadGroupPrefix() {
        return threadGroupPrefix;
    }

    public void setThreadGroupPrefix(String threadGroupPrefix) {
        this.threadGroupPrefix = threadGroupPrefix;
    }

    private abstract class GroupForwardingCallable implements Callable<Content> {
        AccessGroupSet groups = null;

        public void setGroups(AccessGroupSet groups) {
            this.groups = groups;
        }

        protected void storeGroupsOnCurrentThread() {
            LOG.debug("storing groups on thread for FedoraDataService.GroupForwardingCallable: " + groups);
            GroupsThreadStore.storeGroups(groups);
        }

        protected void clearGroupsOnCurrentThread() {
            LOG.debug("clearing groups on thread for FedoraDataService.GroupForwardingCallable");
            GroupsThreadStore.clearGroups();
        }
    }

    /**
     * Retrieves FOXML and adds the results as a child of inputs.
     *
     */
    private class GetFoxml extends GroupForwardingCallable {
        private PID pid;

        public GetFoxml(PID pid) {
            this.pid = pid;
        }

        @Override
        public Content call() throws FedoraException {
            try {
                this.storeGroupsOnCurrentThread();
                LOG.debug("HERE Get FOXML for pid " + pid.getPid());

                // add foxml
                Document foxml;
                foxml = managementClient.getObjectXML(pid);

                return foxml.getRootElement().detach();
            } finally {
                this.clearGroupsOnCurrentThread();
            }
        }
    }

    /**
     * Retrieves object path information indicating the object hierarchy leading up to the specified object and adds the
     * results as a child of inputs named "path".
     *
     */
    private class GetPathInfo implements Callable<Content> {
        private PID pid;

        public GetPathInfo(PID pid) {
            this.pid = pid;
        }

        @Override
        public Content call() {
            LOG.debug("Get path info for " + pid.getPid());
            // add path info
            List<PathInfo> path = null;
            if (path == null || path.size() == 0) {
                throw new ServiceException("No path information was returned for " + pid.getPid());
            }
            Element pathEl = new Element("path");
            for (PathInfo i : path) {
                Element p = new Element("object");
                p.setAttribute("label", i.getLabel());
                p.setAttribute("pid", i.getPid().getPid());
                p.setAttribute("slug", i.getSlug());
                pathEl.addContent(p);
            }
            return pathEl;
        }
    }

    /**
     * Retrieves Mods datastream for pid and adds the results as a child of inputs.
     *
     */
    private class GetMods extends GroupForwardingCallable {
        private PID pid;

        public GetMods(PID pid) {
            this.pid = pid;
        }

        @Override
        public Content call() throws FedoraException, ServiceException, SAXException {
            // add MODS
            try {
                this.storeGroupsOnCurrentThread();
                LOG.debug("Get mods for " + pid.getPid());
                byte[] modsBytes = getAccessClient()
                        .getDatastreamDissemination(pid, "MD_DESCRIPTIVE", null).getStream();
                Document mods = edu.unc.lib.dl.fedora.ClientUtils.parseXML(modsBytes);
                return mods.getRootElement().detach();
            } finally {
                this.clearGroupsOnCurrentThread();
            }
        }
    }

    /**
     * Retrieves the pid identifying the most immediate collection containing the object identified and adds the results
     * as a child of inputs named "parentCollection".
     *
     */
    private class GetParentCollection implements Callable<Content> {
        private PID pid;

        public GetParentCollection(PID pid) {
            this.pid = pid;
        }

        @Override
        public Content call() {
            LOG.debug("Get parent collection for " + pid.getPid());
            PID parentCollection = null;
            if (parentCollection == null) {
                return null;
            }
            Element parentColEl = new Element("parentCollection");
            parentColEl.setText(parentCollection.getPid());
            return parentColEl;
        }
    }

    /**
     * Retrieves the internal sort order value for the default sort within the folder/collection containing the object
     * identified by pid and stores the results as a child of inputs named "order".
     *
     */
    private class GetOrderWithinParent extends GroupForwardingCallable {
        private PID pid;

        public GetOrderWithinParent(PID pid) {
            this.pid = pid;
        }

        @Override
        public Content call() {
            try {
                this.storeGroupsOnCurrentThread();
                LOG.debug("Get Order within Parent for " + pid.getPid());
                PID container = null;
                byte[] structMapBytes = getAccessClient().getDatastreamDissemination(container, "MD_CONTENTS", null)
                        .getStream();
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setValidating(false);
                SAXParser saxParser = factory.newSAXParser();
                StructMapOrderExtractor handler = new StructMapOrderExtractor(pid);
                saxParser.parse(new ByteArrayInputStream(structMapBytes), handler);
                if (handler.getOrder() != null) {
                    Element orderEl = new Element("order");
                    orderEl.setText(handler.getOrder());
                    return orderEl;
                }
                return null;
            } catch (Exception e) {
                throw new ServiceException(e);
            } finally {
                this.clearGroupsOnCurrentThread();
            }
        }
    }

    private class GetDefaultWebObject extends GroupForwardingCallable {
        private PID pid;

        public GetDefaultWebObject(PID pid) {
            this.pid = pid;
        }

        @Override
        public Content call() {
            try {
                this.storeGroupsOnCurrentThread();
                String webObject = null;
            } catch (Exception e) {
                throw new ServiceException(e);
            } finally {
                this.clearGroupsOnCurrentThread();
            }
            return null;
        }
    }
}
