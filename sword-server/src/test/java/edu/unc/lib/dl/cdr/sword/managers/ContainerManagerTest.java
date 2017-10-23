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
package edu.unc.lib.dl.cdr.sword.managers;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.swordapp.server.DepositReceipt;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.util.DepositReportingUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class ContainerManagerTest extends Assert {

    private SwordConfigurationImpl config;

    public ContainerManagerTest(){
        config = new SwordConfigurationImpl();
        config.setBasePath("http://localhost");
        config.setSwordPath("http://localhost/sword");
        config.setAdminDepositor("admin");
    }


    @Test
    public void getEntryCredentials() throws Exception {
        DepositReceipt resultReceipt = mock(DepositReceipt.class);

        DepositReportingUtil depositReportingUtil = mock(DepositReportingUtil.class);
        when(depositReportingUtil.retrieveDepositReceipt(any(PID.class), any(SwordConfigurationImpl.class))).thenReturn(resultReceipt);

        PID pid = new PID("uuid:test");

        TripleStoreQueryService tripleStoreQueryService = mock(TripleStoreQueryService.class);

        Map<String,String> disseminations = new HashMap<>();
        disseminations.put(pid.getURI() + "/" + ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), "text/xml");
        disseminations.put(pid.getURI() + "/" + ContentModelHelper.Datastream.DATA_FILE.getName(), "image/jpg");

        when(tripleStoreQueryService.fetchDisseminatorMimetypes(any(PID.class))).thenReturn(disseminations);

        File modsFile = new File("src/test/resources/modsDocument.xml");
        byte[] modsBytes = FileUtils.readFileToByteArray(modsFile);
        MIMETypedStream mimeStream = new MIMETypedStream();
        mimeStream.setStream(modsBytes);

//        GroupsThreadStore.storeGroups(new AccessGroupSet());
//        AccessControlService aclService = mock(AccessControlService.class);
//        ObjectAccessControlsBean objectACLs = mock(ObjectAccessControlsBean.class);
//        when(objectACLs.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(true);
//        when(aclService.getObjectAccessControls(any(PID.class))).thenReturn(objectACLs);
//
//        AccessClient accessClient = mock(AccessClient.class);
//        when(accessClient.getDatastreamDissemination(any(PID.class), eq(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()), anyString())).thenReturn(mimeStream);
//
//        ContainerManagerImpl containerManager = new ContainerManagerImpl();
//        containerManager.setDepositReportingUtil(depositReportingUtil);
//        containerManager.setTripleStoreQueryService(tripleStoreQueryService);
//        containerManager.setAccessClient(accessClient);
//        containerManager.setAclService(aclService);
//
//        String editIRI = "http://localhost"  + SwordConfigurationImpl.EDIT_PATH + "/" + pid.getPid();
//
//        AuthCredentials auth = new AuthCredentials("testuser", "", null);
//
//        // Check to make sure that not finding the user's onyen is not the end of the world
//        when(objectACLs.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(true);
//        try {
//            DepositReceipt receipt = containerManager.getEntry(editIRI, null, auth, config);
//            assertNotNull(receipt);
//        } catch (SwordAuthException e){
//            fail();
//        }
//
//        when(objectACLs.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(false);
//        try {
//            containerManager.getEntry(editIRI, null, auth, config);
//            fail();
//        } catch (SwordError e){
//            //pass
//        }
//        GroupsThreadStore.clearGroups();
    }
}
