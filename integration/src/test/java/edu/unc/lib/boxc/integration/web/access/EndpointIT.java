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
package edu.unc.lib.boxc.integration.web.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.integration.factories.AdminUnitFactory;
import edu.unc.lib.boxc.integration.factories.CollectionFactory;
import edu.unc.lib.boxc.integration.factories.ContentRootObjectFactory;
import edu.unc.lib.boxc.integration.factories.FileFactory;
import edu.unc.lib.boxc.integration.factories.FolderFactory;
import edu.unc.lib.boxc.integration.factories.WorkFactory;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author snluong
 */
public class EndpointIT {
    @Autowired
    protected AdminUnitFactory adminUnitFactory;
    @Autowired
    protected WorkFactory workFactory;
    @Autowired
    protected CollectionFactory collectionFactory;
    @Autowired
    protected FolderFactory folderFactory;
    @Autowired
    protected ContentRootObjectFactory contentRootObjectFactory;

    public void createDefaultObjects() throws Exception {
        var adminUnit1 = adminUnitFactory.createAdminUnit(Map.of("title", "Object1"));
        var adminUnit2 = adminUnitFactory.createAdminUnit(Map.of("title", "Object2"));
        var collection = collectionFactory.createCollection(adminUnit1, Map.of("title", "Object" + System.nanoTime()));
        var work = workFactory.createWork(collection, Map.of("title", "Object" + System.nanoTime()));
        var fileOptions = Map.of(
                "title", "Object" + System.nanoTime(),
                WorkFactory.PRIMARY_OBJECT_KEY, "false",
                FileFactory.FILE_FORMAT_OPTION, FileFactory.AUDIO_FORMAT);
        workFactory.createFileInWork(work, fileOptions);
        folderFactory.createFolder(collection, Map.of("title", "Object" + System.nanoTime()));
    }

    public List<JsonNode> getMetadataFromResponse(CloseableHttpResponse response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        var respJson = mapper.readTree(response.getEntity().getContent());

        return IteratorUtils.toList(respJson.get("metadata").elements());
    }
}
