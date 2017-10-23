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
package edu.unc.lib.dl.cdr.sword.server.managers;

import java.io.IOException;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionListManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ErrorURIRegistry;

/**
 *
 * @author bbpennel
 *
 */
public class CollectionListManagerImpl extends AbstractFedoraManager implements CollectionListManager {

    private static final Abdera abdera = new Abdera();
    private int pageSize = 10;

    @Override
    public Feed listCollectionContents(IRI collectionIRI, AuthCredentials auth, SwordConfiguration config)
            throws SwordServerException, SwordAuthException, SwordError {

        SwordConfigurationImpl configImpl = (SwordConfigurationImpl) config;

        PID containerPID = null;

        String query = collectionIRI.getPath();
        // Remove path prefixing from url
        int index = query.indexOf(SwordConfigurationImpl.COLLECTION_PATH + "/");
        if (index == -1) {
            throw new SwordServerException("No collection path was found for IRI " + collectionIRI);
        }
        query = query.substring(index + SwordConfigurationImpl.COLLECTION_PATH.length() + 1);

        // Extract the PID
        String pidString = null;
        int startPage = 0;
        index = query.indexOf("/");
        if (index > 0) {
            pidString = query.substring(0, index);
            try {
                startPage = Integer.parseInt(query.substring(index + 1));
                if (startPage < 0) {
                    startPage = 0;
                }
            } catch (NumberFormatException e) {
                throw new SwordError(ErrorURIRegistry.RETRIEVAL_EXCEPTION, 404,
                        "Collection content page number was not valid.");
            }
        } else {
            pidString = query;
        }

        if (pidString == null || "".equals(pidString.trim())) {
            containerPID = RepositoryPaths.getContentRootPid();
        } else {
            containerPID = new PID(pidString);
        }

        // Verify access control
        if (!hasAccess(auth, containerPID, Permission.viewDescription, configImpl)) {
            throw new SwordError(ErrorURIRegistry.INSUFFICIENT_PRIVILEGES, 403,
                    "Insufficient privileges to view the collection list for " + containerPID.getPid());
        }

        Feed feed = abdera.getFactory().newFeed();
        feed.setId(containerPID.getPid());
        // add in the next page link
        feed.addLink(configImpl.getSwordPath() + SwordConfigurationImpl.COLLECTION_PATH + "/" + containerPID.getPid()
                + "/" + (startPage + 1), "next");

        try {
            this.getImmediateChildren(containerPID, startPage, feed, auth, configImpl);
        } catch (IOException e) {
            throw new SwordError(ErrorURIRegistry.RETRIEVAL_EXCEPTION, 500, "Failed to retrieve children for "
                    + containerPID.getPid(), e);
        }

        return feed;
    }

    protected List<Entry> getImmediateChildren(PID pid, int startPage, Feed feed, AuthCredentials auth,
            SwordConfigurationImpl config) throws IOException {
//        String query = this.readFileAsString("immediateChildrenPaged.sparql");
//        query = String.format(query, tripleStoreQueryService.getResourceIndexModelUri(), pid.getURI(), pageSize,
//                startPage * pageSize);
//        List<Entry> result = new ArrayList<Entry>();
//        @SuppressWarnings({ "rawtypes", "unchecked" })
//        List<Map> bindings = (List<Map>) ((Map) tripleStoreQueryService.sendSPARQL(query)
//                .get("results")).get("bindings");
//        for (Map<?, ?> binding : bindings) {
//            PID childPID = new PID((String) ((Map<?, ?>) binding.get("pid")).get("value"));
//            String slug = (String) ((Map<?, ?>) binding.get("slug")).get("value");
//
//            if (hasAccess(auth, childPID, Permission.viewDescription, config)) {
//                Entry entry = feed.addEntry();
//                entry.addLink(config.getSwordPath() + SwordConfigurationImpl.EDIT_MEDIA_PATH + "/"
//                        + childPID.getPid() + ".atom", "edit");
//                entry.addLink(config.getSwordPath() + SwordConfigurationImpl.EDIT_MEDIA_PATH + "/"
//                        + childPID.getPid(), "edit-media");
//                entry.setId(childPID.getURI());
//                entry.setTitle(slug);
//
//                result.add(entry);
//            }
//        }
//        return result;
        return null;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}