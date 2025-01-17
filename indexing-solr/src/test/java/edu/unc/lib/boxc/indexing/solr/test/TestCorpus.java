package edu.unc.lib.boxc.indexing.solr.test;

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.ContentCategory;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import org.apache.solr.common.SolrInputDocument;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

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
    public PID pid7;
    public PID pid6File;

    public PID pid7File;
    public PID nonExistentPid;

    public TestCorpus() {
        pid1 = getContentRootPid();
        pid2 = makePid();
        pid3 = makePid();
        pid4 = makePid();
        pid5 = makePid();
        pid6 = makePid();
        pid7 = makePid();
        pid6File = makePid();
        pid7File = makePid();
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
        List<String> collectionDatastream = List.of(
                DatastreamType.JP2_ACCESS_COPY.getId() + "|image/jp2|bunny.jp2|jp2|766||" + pid2.getId() + "|1200x1200");
        newDoc.addField(SearchFieldKey.DATASTREAM.getSolrField(), collectionDatastream);
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
        newDoc.addField("id", pid6File.getId());
        newDoc.addField("rollup", pid6.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid2, pid6));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1, pid2, pid6));
        newDoc.addField("resourceType", ResourceType.File.name());
        List<String> imgDatastreams = Arrays.asList(
                ORIGINAL_FILE.getId() + "|image/png|file.png|png|766|urn:sha1:checksum||1200x1200",
                DatastreamType.JP2_ACCESS_COPY.getId() + "|image/jp2|bunny.jp2|jp2|766||" + pid6File.getId() + "|1200x1200");
        newDoc.addField(SearchFieldKey.DATASTREAM.getSolrField(), imgDatastreams);
        newDoc.addField(SearchFieldKey.FILE_FORMAT_CATEGORY.getSolrField(), ContentCategory.image.getDisplayName());
        newDoc.addField(SearchFieldKey.FILE_FORMAT_TYPE.getSolrField(), "png");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "Work");
        newDoc.addField("id", pid6.getId());
        newDoc.addField("rollup", pid6.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid2));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1, pid2));
        newDoc.addField("resourceType", ResourceType.Work.name());
        List<String> workDatastreams = Arrays.asList(
                ORIGINAL_FILE.getId() + "|image/png|file.png|png|766|urn:sha1:checksum|" + pid6File.getId() + "|1200x1200",
                DatastreamType.JP2_ACCESS_COPY.getId() + "|image/jp2|bunny.jp2|jp2|766||" + pid6File.getId() + "|1200x1200");
        newDoc.addField(SearchFieldKey.DATASTREAM.getSolrField(), workDatastreams);
        newDoc.addField(SearchFieldKey.FILE_FORMAT_CATEGORY.getSolrField(), ContentCategory.image.getDisplayName());
        newDoc.addField(SearchFieldKey.FILE_FORMAT_TYPE.getSolrField(), "png");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "Work2");
        newDoc.addField("id", pid7.getId());
        newDoc.addField("rollup", pid7.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid3));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1, pid3));
        newDoc.addField("resourceType", ResourceType.Work.name());
        List<String> work2Datastreams = Arrays.asList(
                ORIGINAL_FILE.getId() + "|image/png|file.png|png|766|urn:sha1:checksum|" + pid6File.getId() + "|1200x1200",
                DatastreamType.JP2_ACCESS_COPY.getId() + "|image/jp2|bunny.jp2|jp2|766||" + pid6File.getId() + "|1200x1200");
        newDoc.addField(SearchFieldKey.DATASTREAM.getSolrField(), work2Datastreams);
        newDoc.addField(SearchFieldKey.FILE_FORMAT_CATEGORY.getSolrField(), ContentCategory.image.getDisplayName());
        newDoc.addField(SearchFieldKey.FILE_FORMAT_TYPE.getSolrField(), "png");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "File2");
        newDoc.addField("id", pid7File.getId());
        newDoc.addField("rollup", pid7.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid3, pid7));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1, pid3, pid7));
        newDoc.addField("resourceType", ResourceType.File.name());
        List<String> fileDatastreams = Arrays.asList(
                ORIGINAL_FILE.getId() + "|image/png|file.png|png|766|urn:sha1:checksum||120x120",
                DatastreamType.JP2_ACCESS_COPY.getId() + "|image/jp2|bunny.jp2|jp2|766||" + pid7File.getId() + "|120x120");
        newDoc.addField(SearchFieldKey.DATASTREAM.getSolrField(), fileDatastreams);
        newDoc.addField(SearchFieldKey.FILE_FORMAT_CATEGORY.getSolrField(), ContentCategory.image.getDisplayName());
        newDoc.addField(SearchFieldKey.FILE_FORMAT_TYPE.getSolrField(), "png");
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
        List<String> collection2Datastream = List.of(
                DatastreamType.JP2_ACCESS_COPY.getId() + "|image/jp2|bunny.jp2|jp2|0||" + pid2.getId() + "|1200x1200");
        newDoc.addField(SearchFieldKey.DATASTREAM.getSolrField(), collection2Datastream);
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
