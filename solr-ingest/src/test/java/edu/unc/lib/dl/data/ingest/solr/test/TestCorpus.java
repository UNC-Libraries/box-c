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
package edu.unc.lib.dl.data.ingest.solr.test;

import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getContentRootPid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrInputDocument;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

/**
 * Utility which constructs a small corpus of solr documents for testing and
 * provides access to the pids for each document
 *
 * The test corpus follows the following structure
 * obj1 (root)
 *   > obj2
 *     > obj4
 *     > obj5
 *     > obj6
 *   > obj3
 *
 * @author bbpennel
 *
 */
public class TestCorpus {
    public PID pid1;
    public PID pid2;
    public PID pid3;
    public PID pid4;
    public PID pid5;
    public PID pid6;
    public PID nonExistentPid;

    public TestCorpus() {
        pid1 = getContentRootPid();
        pid2 = makePid();
        pid3 = makePid();
        pid4 = makePid();
        pid5 = makePid();
        pid6 = makePid();
        nonExistentPid = makePid();
    }

    public List<SolrInputDocument> populate() {
        List<SolrInputDocument> docs = new ArrayList<>();

        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("title", "Collections");
        newDoc.addField("id", pid1.getId());
        newDoc.addField("rollup", pid1.getId());
        newDoc.addField("roleGroup", "");
        newDoc.addField("readGroup", "");
        newDoc.addField("adminGroup", "");
        newDoc.addField("ancestorIds", "");
        newDoc.addField("resourceType", "ContentRoot");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "A collection");
        newDoc.addField("id", pid2.getId());
        newDoc.addField("rollup", pid2.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid2));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1));
        newDoc.addField("resourceType", "Collection");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "Subfolder 1");
        newDoc.addField("id", pid4.getId());
        newDoc.addField("rollup", pid4.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid2, pid4));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1, pid2));
        newDoc.addField("resourceType", "Folder");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "Orphaned");
        newDoc.addField("id", pid5.getId());
        newDoc.addField("rollup", pid5.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid2));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1, pid2));
        newDoc.addField("resourceType", "File");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "File");
        newDoc.addField("id", pid6.getId());
        newDoc.addField("rollup", pid6.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid2));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1, pid2));
        newDoc.addField("resourceType", "File");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "Second collection");
        newDoc.addField("id", pid3.getId());
        newDoc.addField("rollup", pid3.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid3));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1));
        newDoc.addField("resourceType", "Collection");
        docs.add(newDoc);

        return docs;
    }

    public String makeAncestorIds(PID... pids) {
        return "/" + Arrays.stream(pids).map(p -> p.getId()).collect(Collectors.joining("/"));
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
