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
package edu.unc.lib.boxc.search.solr.test;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static java.util.Arrays.asList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.solr.facets.FilterableDisplayValueFacet;
import org.apache.solr.common.SolrInputDocument;

import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.ContentCategory;

/**
 * Utility which constructs a small corpus of solr documents for testing and
 * provides access to the pids for each document
 *
 * The test corpus follows the following structure
 * root
 *  > unit (unitOwner)
 *    > coll1 (manager)
 *      > folder1
 *        > work1
 *          > file1 -> text/txt
 *          > file2 -> text/pdf
 *        > work2
 *          > file1 -> image/jpeg
 *    > coll2
 *      > work3
 *        > file1 -> text/txt
 *      > privateFolder (remove public)
 *        > privateWork
 *          > file1 -> image/png
 *
 * @author bbpennel
 *
 */
public class TestCorpus {
    public PID rootPid;
    public PID unitPid;
    public PID coll1Pid;
    public PID coll2Pid;
    public PID folder1Pid;
    public PID work1Pid;
    public PID work1File1Pid;
    public PID work1File2Pid;
    public PID work2Pid;
    public PID work2File1Pid;
    public PID work3Pid;
    public PID work3File1Pid;
    public PID privateFolderPid;
    public PID privateWorkPid;
    public PID privateWorkFile1Pid;

    public static final String TEST_COLL_ID = "10478";

    private Map<PID, String> titleCache = new HashMap<>();

    public TestCorpus() {
        // Initialize all pids
        Field[] fields = getClass().getFields();
        for (Field field : fields) {
            if (field.getName().endsWith("Pid")) {
                try {
                    field.set(this, makePid());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        rootPid = getContentRootPid();
    }

    public List<SolrInputDocument> populate() {
        List<SolrInputDocument> docs = new ArrayList<>();

        SolrInputDocument newDoc = makeContainerDocument(rootPid, "Collections", ResourceType.ContentRoot);
        addAclProperties(newDoc, PUBLIC_PRINC, null, null);
        docs.add(newDoc);

        newDoc = makeContainerDocument(unitPid, "Unit", ResourceType.AdminUnit,
                rootPid);
        addAclProperties(newDoc, PUBLIC_PRINC, "unitOwner", null);
        docs.add(newDoc);

        newDoc = makeContainerDocument(coll1Pid, "Collection 1", ResourceType.Collection,
                rootPid, unitPid);
        addAclProperties(newDoc, PUBLIC_PRINC, "unitOwner", "manager");
        newDoc.addField("collectionId", TEST_COLL_ID);
        docs.add(newDoc);

        newDoc = makeContainerDocument(folder1Pid, "Folder 1", ResourceType.Folder,
                rootPid, unitPid, coll1Pid);
        addAclProperties(newDoc, PUBLIC_PRINC, "unitOwner", "manager");
        docs.add(newDoc);

        newDoc = makeContainerDocument(work1Pid, "Work 1", ResourceType.Work,
                rootPid, unitPid, coll1Pid, folder1Pid);
        addAclProperties(newDoc, PUBLIC_PRINC, "unitOwner", "manager");
        addFileProperties(newDoc, ContentCategory.text, "text/plain", "Plain Text");
        addFileProperties(newDoc, ContentCategory.text, "application/pdf", "PDF");
        docs.add(newDoc);

        newDoc = makeFileDocument(work1File1Pid, "File 1",
                rootPid, unitPid, coll1Pid, folder1Pid, work1Pid);
        addFileProperties(newDoc, ContentCategory.text, "text/plain", "Plain Text");
        addAclProperties(newDoc, PUBLIC_PRINC, "unitOwner", "manager");
        docs.add(newDoc);

        newDoc = makeFileDocument(work1File2Pid, "File 2",
                rootPid, unitPid, coll1Pid, folder1Pid, work1Pid);
        addAclProperties(newDoc, PUBLIC_PRINC, "unitOwner", "manager");
        addFileProperties(newDoc, ContentCategory.text, "application/pdf", "PDF");
        docs.add(newDoc);

        newDoc = makeContainerDocument(work2Pid, "Work 2", ResourceType.Work,
                rootPid, unitPid, coll1Pid, folder1Pid);
        addAclProperties(newDoc, PUBLIC_PRINC, "unitOwner", "manager");
        addFileProperties(newDoc, ContentCategory.image, "image/jpeg", "JPEG Image");
        docs.add(newDoc);

        newDoc = makeFileDocument(work2File1Pid, "File 3",
                rootPid, unitPid, coll1Pid, folder1Pid, work2Pid);
        addAclProperties(newDoc, PUBLIC_PRINC, "unitOwner", "manager");
        addFileProperties(newDoc, ContentCategory.image, "image/jpeg", "JPEG Image");
        docs.add(newDoc);

        newDoc = makeContainerDocument(coll2Pid, "Collection 2", ResourceType.Collection,
                rootPid, unitPid);
        addAclProperties(newDoc, PUBLIC_PRINC, "unitOwner", null);
        docs.add(newDoc);

        newDoc = makeContainerDocument(work3Pid, "Work 3", ResourceType.Work,
                rootPid, unitPid, coll2Pid);
        addAclProperties(newDoc, PUBLIC_PRINC, "unitOwner", null);
        addFileProperties(newDoc, ContentCategory.text, "text/plain", "Plain Text");
        docs.add(newDoc);

        newDoc = makeFileDocument(work3File1Pid, "File 1",
                rootPid, unitPid, coll2Pid, work3Pid);
        addAclProperties(newDoc, PUBLIC_PRINC, "unitOwner", null);
        addFileProperties(newDoc, ContentCategory.text, "text/plain", "Plain Text");
        docs.add(newDoc);

        newDoc = makeContainerDocument(privateFolderPid, "Private Folder", ResourceType.Folder,
                rootPid, unitPid, coll2Pid);
        addAclProperties(newDoc, null, "unitOwner", null);
        docs.add(newDoc);

        newDoc = makeContainerDocument(privateWorkPid, "Private Work", ResourceType.Work,
                rootPid, unitPid, coll2Pid, privateFolderPid);
        addAclProperties(newDoc, null, "unitOwner", null);
        addFileProperties(newDoc, ContentCategory.image, "image/png", "Portable Network Graphics");
        docs.add(newDoc);

        newDoc = makeFileDocument(privateWorkFile1Pid, "Private File",
                rootPid, unitPid, coll2Pid, privateFolderPid, privateWorkPid);
        addAclProperties(newDoc, null, "unitOwner", null);
        addFileProperties(newDoc, ContentCategory.image, "image/png", "Portable Network Graphics");
        docs.add(newDoc);

        return docs;
    }

    public SolrInputDocument makeContainerDocument(PID pid, String title, ResourceType type, PID... ancestors) {
        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("title", title);
        titleCache.put(pid, title);
        newDoc.addField("id", pid.getId());
        newDoc.addField("rollup", pid.getId());
        addPathProperties(newDoc, pid, ancestors);
        newDoc.addField("resourceType", type.name());
        return newDoc;
    }

    public SolrInputDocument makeFileDocument(PID pid, String title, PID... ancestors) {
        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("title", title);
        newDoc.addField("id", pid.getId());
        newDoc.addField("rollup", ancestors[ancestors.length - 1].getId());
        addPathProperties(newDoc, null, ancestors);
        newDoc.addField("resourceType", ResourceType.File.name());
        return newDoc;
    }

    public void addPathProperties(SolrInputDocument newDoc, PID selfPid, PID... ancestors) {
        newDoc.addField("ancestorIds", makeAncestorIds(selfPid, ancestors));
        newDoc.addField("ancestorPath", makeAncestorPath(ancestors));
        if (ancestors.length > 1) {
            var title = titleCache.get(ancestors[1]);
            var value = FilterableDisplayValueFacet.buildValue(title, ancestors[1].getId());
            newDoc.addField("parentUnit", value);
        }
        if (ancestors.length > 2) {
            var title = titleCache.get(ancestors[2]);
            var value = FilterableDisplayValueFacet.buildValue(title, ancestors[2].getId());
            newDoc.addField("parentCollection", value);
        }
    }

    public void addFileProperties(SolrInputDocument doc, ContentCategory typeCategory, String mimeType, String desc) {
        if (doc.getField(SearchFieldKey.FILE_FORMAT_CATEGORY.getSolrField()) != null) {
            doc.getField(SearchFieldKey.FILE_FORMAT_CATEGORY.getSolrField()).addValue(typeCategory.getDisplayName());
        } else {
            doc.addField(SearchFieldKey.FILE_FORMAT_CATEGORY.getSolrField(), new ArrayList<>(Arrays.asList(typeCategory.getDisplayName())));
        }
        if (doc.getField(SearchFieldKey.FILE_FORMAT_TYPE.getSolrField()) != null) {
            doc.getField(SearchFieldKey.FILE_FORMAT_TYPE.getSolrField()).addValue(mimeType);
        } else {
            doc.addField(SearchFieldKey.FILE_FORMAT_TYPE.getSolrField(), new ArrayList<>(Arrays.asList(mimeType)));
        }
        if (desc != null) {
            if (doc.getField(SearchFieldKey.FILE_FORMAT_DESCRIPTION.getSolrField()) != null) {
                doc.getField(SearchFieldKey.FILE_FORMAT_DESCRIPTION.getSolrField()).addValue(desc);
            } else {
                doc.addField(SearchFieldKey.FILE_FORMAT_DESCRIPTION.getSolrField(), new ArrayList<>(Arrays.asList(desc)));
            }
        }
    }

    public void addAclProperties(SolrInputDocument doc, String readGroup, String unitOwner, String manager) {
        List<String> adminList = new ArrayList<>();
        List<String> roleGroups = new ArrayList<>();
        if (unitOwner != null) {
            adminList.add(unitOwner);
            roleGroups.add(UserRole.unitOwner.name() + "|" + unitOwner);
        }
        if (manager != null) {
            adminList.add(manager);
            roleGroups.add(UserRole.canManage.name() + "|" + manager);
        }
        if (readGroup != null) {
            roleGroups.add(UserRole.canViewOriginals.name() + "|" + readGroup);
            doc.addField("readGroup", asList(readGroup));
        }

        doc.addField("roleGroup", roleGroups);
        doc.addField("adminGroup", adminList);
    }

    public String makeAncestorIds(PID self, PID... pids) {
        String path;
        if (pids == null) {
            path = "";
        } else {
            path = "/" + Arrays.stream(pids).map(p -> p.getId()).collect(Collectors.joining("/"));
        }
        if (self != null) {
            path += "/" + self.getId();
        }
        return path;
    }

    public List<String> makeAncestorPath(PID... pids) {
        List<String> result = new ArrayList<>();
        int i = 0;
        for (PID pid : pids) {
            i++;
            result.add(i + "," + pid.getId());
        }
        return result;
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
