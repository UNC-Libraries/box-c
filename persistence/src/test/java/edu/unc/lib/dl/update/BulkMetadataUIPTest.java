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
package edu.unc.lib.dl.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Test;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

/**
 * @author bbpennel
 * @date Jul 14, 2015
 */
public class BulkMetadataUIPTest {

    @Test
    public void test () throws Exception {
        File importFile = new File("src/test/resources/md_import.xml");

        BulkMetadataUIP uip = new BulkMetadataUIP(null, "", "", new AccessGroupSet(),
                importFile, "file");

        BulkMetadataDatastreamUIP next = uip.getNextUpdate();
        assertNotNull(next);
        assertEquals("Did not retrieve the correct object update pid",
                "uuid:76240153-300b-4e90-9c55-94c64f4a24de", next.getPID().getPid());
        assertNotNull(next.getIncomingData().get(Datastream.MD_DESCRIPTIVE.getName()));
        // System.out.println("|" + new XMLOutputter(Format.getRawFormat()).outputString(next.getIncomingData().get(Datastream.MD_DESCRIPTIVE.getName())) + "|");

        next = uip.getNextUpdate();
        assertNotNull(next);
        assertEquals("Incorrect second object pid retrieved",
                "uuid:d7e8f997-e9f5-4c8c-9869-c0898f7d08a9", next.getPID().getPid());
        assertNotNull(next.getIncomingData().get(Datastream.MD_DESCRIPTIVE.getName()));

        assertNull("Only two updates should be present", uip.getNextUpdate());
    }

    @Test
    public void resumeTest () throws Exception {
        File importFile = new File("src/test/resources/md_import.xml");

        BulkMetadataUIP uip = new BulkMetadataUIP("testid", "", "", new AccessGroupSet(),
                importFile, "file");

        uip.seekNextUpdate(new PID("uuid:76240153-300b-4e90-9c55-94c64f4a24de"), Datastream.MD_DESCRIPTIVE.toString());

        BulkMetadataDatastreamUIP next = uip.getNextUpdate();
        assertNotNull(next);
        assertEquals("Did not resume from the correct point",
                "uuid:d7e8f997-e9f5-4c8c-9869-c0898f7d08a9", next.getPID().getPid());
        assertNotNull(next.getIncomingData().get(Datastream.MD_DESCRIPTIVE.getName()));

    }
}
