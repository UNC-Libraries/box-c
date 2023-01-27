package edu.unc.lib.boxc.indexing.solr.action;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.action.ClearIndexAction;

public class ClearIndexActionTest extends BaseEmbeddedSolrTest {

    @BeforeEach
    public void setup() throws Exception {

        server.add(populate());
        server.commit();

    }

    @Test
    public void testDeleteAll() throws Exception {

        SolrDocumentList docListBefore = getDocumentList();
        assertEquals(4, docListBefore.size(), "Index should contain contents before delete");

        ClearIndexAction action = new ClearIndexAction();
        action.setSolrUpdateDriver(driver);

        SolrUpdateRequest request = mock(SolrUpdateRequest.class);
        action.performAction(request);

        SolrDocumentList docListAfter = getDocumentList();

        assertEquals(0, docListAfter.size(), "Index must be empty after delete all");
    }

    protected List<SolrInputDocument> populate() {
        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("title", "Collections");
        newDoc.addField("id", "uuid:1");
        newDoc.addField("rollup", "uuid:1");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1");
        newDoc.addField("resourceType", "Folder");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "A collection");
        newDoc.addField("id", "uuid:2");
        newDoc.addField("rollup", "uuid:2");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1/uuid:2");
        newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1"));
        newDoc.addField("resourceType", "Collection");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "File");
        newDoc.addField("id", "uuid:6");
        newDoc.addField("rollup", "uuid:6");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1/uuid:2");
        newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1", "2,uuid:2"));
        newDoc.addField("resourceType", "File");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "Second collection");
        newDoc.addField("id", "uuid:3");
        newDoc.addField("rollup", "uuid:3");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1/uuid:3");
        newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1"));
        newDoc.addField("resourceType", "Collection");
        docs.add(newDoc);

        return docs;
    }
}
