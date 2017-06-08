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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

public class AtomicUpdateTest {

    private static String ID_FIELD = "id";
    private static String UPDATE_TIMESTAMP = "timestamp";

    @Test
    public void atomicUpdate() throws IOException {
        IndexDocumentBean idb = new IndexDocumentBean();
        idb.setId("id");
        idb.setStatus(Arrays.asList("Unpublished", "Parent Unpublished"));

        DocumentObjectBinder binder = new DocumentObjectBinder();
        SolrInputDocument sid = binder.toSolrInputDocument(idb);

        String operation = "set";

        for (String fieldName : sid.getFieldNames()) {
            if (!ID_FIELD.equals(fieldName)) {
                SolrInputField inputField = sid.getField(fieldName);
                // Adding in each non-null field value, except the timestamp field which gets cleared if not specified so
                // that it always gets updated as part of a partial update
                // TODO enable timestamp updating when fix for SOLR-4133 is released, which enables setting null fields
                if (inputField != null && (inputField.getValue() != null || UPDATE_TIMESTAMP.equals(fieldName))) {
                    Map<String, Object> partialUpdate = new HashMap<String, Object>();
                    partialUpdate.put(operation, inputField.getValue());
                    sid.setField(fieldName, partialUpdate);
                }
            }
        }

    /*    Map<String,String> mapField = new HashMap<String,String>();
        mapField.put("", arg1)
        Map<String, Object> partialUpdate = new HashMap<String, Object>();
        partialUpdate.put("set", inputField.getFirstValue());
        sid.setField(fieldName, partialUpdate);*/

        StringWriter writer = new StringWriter();
        ClientUtils.writeXML(sid, writer);
    }
}
