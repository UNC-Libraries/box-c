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
package edu.unc.lib.dcr.migration.fcrepo3;

import static edu.unc.lib.dcr.migration.MigrationConstants.toBxc3Uri;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel.CONTAINER;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel.PRESERVEDOBJECT;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.Relationship.contains;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.ORIGINAL_DS;
import static edu.unc.lib.dl.xml.SecureXMLFactory.createSAXBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.jdom2.Document;
import org.junit.Test;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 */
public class FoxmlDocumentHelpersTest {

    @Test
    public void getObjectModelForFolder() throws Exception {
        String objectId = "uuid:c9b95252-ca53-4e38-9155-81f3304ed3d5";
        PID pid = PIDs.get(objectId);
        Document foxml = createSAXBuilder().build(getClass().getResource("/foxml/folderSmall.xml"));
        Model model = FoxmlDocumentHelpers.getObjectModel(foxml);

        Resource resultResc = model.getResource(toBxc3Uri(pid));

        List<Statement> containsList = resultResc.listProperties(contains.getProperty()).toList();
        assertEquals(6, containsList.size());

        assertTrue(resultResc.hasProperty(hasModel.getProperty(), CONTAINER.getResource()));
        assertTrue(resultResc.hasProperty(hasModel.getProperty(), PRESERVEDOBJECT.getResource()));
    }

    @Test
    public void listDatastreamVersionsOutOfOrder() throws Exception {
        Document foxml = createSAXBuilder().build(getClass().getResource("/foxml/fileMultipleDataFile.xml"));

        List<DatastreamVersion> dsVersions = FoxmlDocumentHelpers.listDatastreamVersions(foxml, ORIGINAL_DS);

        assertEquals(3, dsVersions.size());

        DatastreamVersion v0 = dsVersions.get(0);
        assertEquals("DATA_FILE.0", v0.getVersionName());
        assertEquals("2011-10-13T17:56:37.993Z", v0.getCreated());
        assertEquals("99fc2ca39feea757475df7caa4a8dbac", v0.getMd5());
        assertEquals("text/plain", v0.getMimeType());

        DatastreamVersion v2 = dsVersions.get(2);
        assertEquals("DATA_FILE.2", v2.getVersionName());
        assertEquals("2011-12-05T15:00:54.408Z", v2.getCreated());
        assertEquals("a4443ebd04da89c898a448a48f71e7b3", v2.getMd5());
        assertEquals("application/pdf", v2.getMimeType());
    }
}
