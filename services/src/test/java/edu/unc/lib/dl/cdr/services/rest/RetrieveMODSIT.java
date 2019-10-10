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
package edu.unc.lib.dl.cdr.services.rest;

import static edu.unc.lib.dl.acl.util.Permission.viewMetadata;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.cdr.services.rest.modify.AbstractAPIIT;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author harring
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/retrieve-mods-it-servlet.xml")
})
public class RetrieveMODSIT extends AbstractAPIIT {

    @Test
    public void testRetrieveMODSFromWork() throws Exception {
        doNothing().when(aclService).assertHasAccess(anyString(), any(PID.class), any(AccessGroupSet.class),
                eq(viewMetadata));
        PID workPid = makePid();
        File modsFile = setupWorkWithMODS(workPid);

        MvcResult result = mvc.perform(get("/description/" + workPid.getUUID()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String expectedResponseBody = FileUtils.readFileToString(modsFile, StandardCharsets.UTF_8);
        assertEquals(expectedResponseBody, responseBody);
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), any(PID.class), any(AccessGroupSet.class), eq(viewMetadata));

        PID workPid = makePid();
        setupWorkWithMODS(workPid);

        mvc.perform(get("/description/" + workPid.getUUID()))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testObjectWithoutMODS() throws Exception {
        doNothing().when(aclService).assertHasAccess(anyString(), any(PID.class), any(AccessGroupSet.class),
                eq(viewMetadata));
        WorkObject work = repositoryObjectFactory.createWorkObject(null);

        mvc.perform(get("/description/" + work.getPid().getUUID()))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void testObjectDoesNotExist() throws Exception {
        doNothing().when(aclService).assertHasAccess(anyString(), any(PID.class), any(AccessGroupSet.class),
                eq(viewMetadata));
        PID pid = makePid();

        mvc.perform(get("/description/" + pid.getUUID()))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    private File setupWorkWithMODS(PID pid) throws FileNotFoundException {
        WorkObject work = repositoryObjectFactory.createWorkObject(pid, null);
        File modsFile = new File("src/test/resources/mods/work-mods.xml");
        InputStream modsStream = new FileInputStream(modsFile);
        work.setDescription(modsStream);

        return modsFile;
    }
}
