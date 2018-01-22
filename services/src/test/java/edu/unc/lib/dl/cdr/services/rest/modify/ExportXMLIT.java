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
package edu.unc.lib.dl.cdr.services.rest.modify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.cdr.services.rest.modify.ExportXMLController.XMLExportRequest;

/**
 *
 * @author harring
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/export-xml-it-servlet.xml")
})
public class ExportXMLIT extends AbstractAPIIT {

    @Test
    public void testExportMODS() throws Exception {

        String pid1 = makePid().getRepositoryPath();
        String pid2 = makePid().getRepositoryPath();
        List<String> pids = new ArrayList<>();
        pids.add(pid1);
        pids.add(pid2);

        XMLExportRequest exportRequest = new XMLExportRequest(pids, false, "user@example.com");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(exportRequest);
        MvcResult result = mvc.perform(post("/edit/exportXML").param("exportRequest", json))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

}
