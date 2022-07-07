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
import edu.unc.lib.boxc.integration.factories.AdminUnitFactory;
import edu.unc.lib.boxc.integration.factories.CollectionFactory;
import edu.unc.lib.boxc.integration.factories.ContentRootObjectFactory;
import edu.unc.lib.boxc.integration.factories.FileFactory;
import edu.unc.lib.boxc.integration.factories.FolderFactory;
import edu.unc.lib.boxc.integration.factories.WorkFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Utility methods for testing endpoints in the SearchActionController
 * @author snluong
 */
public class SearchEndpointTestUtility {
    public static void assertValuePresent(List<JsonNode> json, int index, String key, String value) {
        var result = json.get(index);
        assertEquals(value, result.get(key).asText());
    }

    public static void assertValuePresent(List<JsonNode> json, int index, String key) {
        var result = json.get(index);
        assertNotNull(result.get(key).asText());
    }

    public static void assertSuccessfulResponse(CloseableHttpResponse response) {
        System.out.println("response = " + response);
        assertEquals(200, response.getStatusLine().getStatusCode());
    }
}
