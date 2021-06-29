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
package edu.unc.lib.dl.persist.services.storage;

import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getContentRootPid;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageType;
import edu.unc.lib.dl.persist.api.storage.UnknownStorageLocationException;

/**
 * @author bbpennel
 *
 */
public class StorageLocationManagerImplTest {
    private static final String LOC1_ID = "loc1";
    private static final String LOC1_NAME = "Stor1";
    private static final String LOC1_BASE = "file://some/path";
    private static final String LOC2_ID = "loc2";
    private static final String LOC2_NAME = "Stor2";
    private static final String LOC2_BASE = "file://some/other";

    @Mock
    private ContentPathFactory pathFactory;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;

    private StorageLocationManagerImpl locManager;

    private StorageLocationTestHelper helper;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        initMocks(this);
        tmpFolder.create();

        locManager = new StorageLocationManagerImpl();
        locManager.setPathFactory(pathFactory);
        locManager.setRepositoryObjectLoader(repositoryObjectLoader);

        helper = new StorageLocationTestHelper();
    }

    private void initializeManager() throws Exception {
        locManager.setConfigPath(helper.serializeLocationConfig());
        locManager.setMappingPath(helper.serializeLocationMappings());

        locManager.init();
    }

    @Test(expected = IllegalStateException.class)
    public void duplicateLocationId() throws Exception {
        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addStorageLocation(LOC1_ID, LOC2_NAME, LOC2_BASE);

        initializeManager();
    }

    @Test(expected = IllegalStateException.class)
    public void duplicateMappingContainerId() throws Exception {
        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addStorageLocation(LOC2_ID, LOC2_NAME, LOC2_BASE);
        PID unitPid = makePid();
        helper.addMapping(unitPid.getId(), LOC1_ID);
        helper.addMapping(unitPid.getRepositoryPath(), LOC2_ID);

        initializeManager();
    }

    @Test(expected = UnknownStorageLocationException.class)
    public void mappingToNonexistentLocation() throws Exception {
        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        PID unitPid = makePid();
        helper.addMapping(unitPid.getId(), LOC2_ID);

        initializeManager();
    }

    @Test(expected = UnknownStorageLocationException.class)
    public void getDefaultStorageLocationNoMappings() throws Exception {
        PID unitPid = makePid();
        mockAncestors(unitPid, getContentRootPid());

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);

        initializeManager();

        locManager.getDefaultStorageLocation(unitPid);
    }

    @Test
    public void getDefaultStorageLocationFromRepoDefault() throws Exception {
        PID unitPid = makePid();
        mockAncestors(unitPid, getContentRootPid());

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addMapping(getContentRootPid().getId(), LOC1_ID);

        initializeManager();

        StorageLocation loc = locManager.getDefaultStorageLocation(unitPid);
        assertIsLocation1(loc);
    }

    @Test
    public void getDefaultStorageLocationFromAssigned() throws Exception {
        PID unitPid = makePid();
        mockAncestors(unitPid, getContentRootPid());

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addMapping(unitPid.getId(), LOC1_ID);

        initializeManager();

        StorageLocation loc = locManager.getDefaultStorageLocation(unitPid);
        assertIsLocation1(loc);
    }

    @Test
    public void getDefaultStorageLocationAssignedOverrideRepoDefault() throws Exception {
        PID unitPid = makePid();
        mockAncestors(unitPid, getContentRootPid());

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addStorageLocation(LOC2_ID, LOC2_NAME, LOC2_BASE);
        helper.addMapping(unitPid.getId(), LOC1_ID);
        helper.addMapping(getContentRootPid().getId(), LOC2_ID);

        initializeManager();

        StorageLocation loc = locManager.getDefaultStorageLocation(unitPid);
        assertIsLocation1(loc);
    }

    @Test
    public void getDefaultStorageLocationFromCollection() throws Exception {
        PID unitPid = makePid();
        PID collPid = makePid();
        PID folderPid = makePid();
        PID workPid = makePid();
        mockAncestors(collPid, getContentRootPid(), unitPid);
        mockAncestors(workPid, getContentRootPid(), unitPid, collPid, folderPid);

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addStorageLocation(LOC2_ID, LOC2_NAME, LOC2_BASE);
        helper.addMapping(unitPid.getId(), LOC1_ID);
        helper.addMapping(folderPid.getId(), LOC2_ID);

        initializeManager();

        StorageLocation collLoc = locManager.getDefaultStorageLocation(collPid);
        assertIsLocation1(collLoc);
        StorageLocation workLoc = locManager.getDefaultStorageLocation(workPid);
        // Assignment past the collection should be ignored
        assertIsLocation1(workLoc);
    }

    @Test
    public void getDefaultStorageLocationOverrideOnCollection() throws Exception {
        PID unitPid = makePid();
        PID collPid = makePid();
        mockAncestors(unitPid, getContentRootPid());
        mockAncestors(collPid, getContentRootPid(), unitPid);

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addStorageLocation(LOC2_ID, LOC2_NAME, LOC2_BASE);
        helper.addMapping(getContentRootPid().getId(), LOC1_ID);
        helper.addMapping(collPid.getId(), LOC2_ID);

        initializeManager();

        StorageLocation unitLoc = locManager.getDefaultStorageLocation(unitPid);
        assertIsLocation1(unitLoc);
        StorageLocation collLoc = locManager.getDefaultStorageLocation(collPid);
        assertIsLocation2(collLoc);
    }

    @Test
    public void getStorageLocationWithAssign() throws Exception {
        PID unitPid = makePid();
        PID collPid = makePid();
        mockAncestors(collPid, getContentRootPid(), unitPid);

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addMapping(getContentRootPid().getId(), LOC1_ID);

        initializeManager();

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(collPid.getRepositoryPath());
        resc.addProperty(Cdr.storageLocation, LOC1_ID);
        CollectionObject collObj = mock(CollectionObject.class);
        when(collObj.getPid()).thenReturn(collPid);
        when(collObj.getResource()).thenReturn(resc);

        StorageLocation loc = locManager.getStorageLocation(collObj);
        assertIsLocation1(loc);
    }

    @Test
    public void getStorageLocationWithAssignDifferentFromDefault() throws Exception {
        PID unitPid = makePid();
        PID collPid = makePid();
        mockAncestors(collPid, getContentRootPid(), unitPid);

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addStorageLocation(LOC2_ID, LOC2_NAME, LOC2_BASE);
        helper.addMapping(getContentRootPid().getId(), LOC1_ID);

        initializeManager();

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(collPid.getRepositoryPath());
        resc.addProperty(Cdr.storageLocation, LOC2_ID);
        CollectionObject collObj = mock(CollectionObject.class);
        when(collObj.getPid()).thenReturn(collPid);
        when(collObj.getResource()).thenReturn(resc);

        StorageLocation loc = locManager.getStorageLocation(collObj);
        assertIsLocation2(loc);
    }

    @Test
    public void getStorageLocationWithoutAssign() throws Exception {
        PID unitPid = makePid();
        PID collPid = makePid();
        mockAncestors(collPid, getContentRootPid(), unitPid);

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addMapping(getContentRootPid().getId(), LOC1_ID);

        initializeManager();

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(collPid.getRepositoryPath());
        CollectionObject collObj = mock(CollectionObject.class);
        when(collObj.getPid()).thenReturn(collPid);
        when(collObj.getResource()).thenReturn(resc);

        StorageLocation loc = locManager.getStorageLocation(collObj);
        assertIsLocation1(loc);
    }

    @Test(expected = UnknownStorageLocationException.class)
    public void getStorageLocationWithInvalidLocation() throws Exception {
        PID unitPid = makePid();
        PID collPid = makePid();
        mockAncestors(collPid, getContentRootPid(), unitPid);

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addMapping(getContentRootPid().getId(), LOC1_ID);

        initializeManager();

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(collPid.getRepositoryPath());
        // Loc2 is not configured
        resc.addProperty(Cdr.storageLocation, LOC2_ID);
        CollectionObject collObj = mock(CollectionObject.class);
        when(collObj.getPid()).thenReturn(collPid);
        when(collObj.getResource()).thenReturn(resc);

        locManager.getStorageLocation(collObj);
    }

    @Test
    public void getStorageLocationFromPidWithAssign() throws Exception {
        PID unitPid = makePid();
        mockAncestors(unitPid, getContentRootPid());

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);

        initializeManager();

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(unitPid.getRepositoryPath());
        resc.addProperty(Cdr.storageLocation, LOC1_ID);
        AdminUnit unitObj = mock(AdminUnit.class);
        when(unitObj.getPid()).thenReturn(unitPid);
        when(unitObj.getResource()).thenReturn(resc);

        when(repositoryObjectLoader.getRepositoryObject(unitPid)).thenReturn(unitObj);

        StorageLocation loc = locManager.getStorageLocation(unitPid);
        assertIsLocation1(loc);
    }

    @Test
    public void getStorageLocationById() throws Exception {
        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addStorageLocation(LOC2_ID, LOC2_NAME, LOC2_BASE);

        initializeManager();

        assertIsLocation1(locManager.getStorageLocationById(LOC1_ID));
        assertIsLocation2(locManager.getStorageLocationById(LOC2_ID));
        assertNull(locManager.getStorageLocationById("unrealloc"));
    }

    @Test
    public void getStorageLocationForUriMatchingUris() throws Exception {
        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addStorageLocation(LOC2_ID, LOC2_NAME, LOC2_BASE);

        initializeManager();

        assertIsLocation1(locManager.getStorageLocationForUri(URI.create(LOC1_BASE + "/sub/file")));
        assertIsLocation2(locManager.getStorageLocationForUri(URI.create(LOC2_BASE + "/other/path")));
    }

    @Test(expected = UnknownStorageLocationException.class)
    public void getStorageLocationForUriParentUri() throws Exception {
        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);

        initializeManager();

        locManager.getStorageLocationForUri(URI.create("file://some"));
    }

    @Test(expected = UnknownStorageLocationException.class)
    public void getStorageLocationForUriNoMatching() throws Exception {
        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);

        initializeManager();

        locManager.getStorageLocationForUri(URI.create("file://random/location"));
    }

    @Test
    public void listAvailableStorageLocationsFromAncestor() throws Exception {
        PID unitPid = makePid();
        mockAncestors(unitPid, getContentRootPid());

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addMapping(getContentRootPid().getId(), LOC1_ID);

        initializeManager();

        List<StorageLocation> locs = locManager.listAvailableStorageLocations(unitPid);
        assertEquals(1, locs.size());
        assertIsLocation1(locs.get(0));
    }

    @Test
    public void listAvailableStorageLocationsNoneAvailable() throws Exception {
        PID unitPid = makePid();
        mockAncestors(unitPid, getContentRootPid());

        initializeManager();

        List<StorageLocation> locs = locManager.listAvailableStorageLocations(unitPid);
        assertEquals(0, locs.size());
    }

    @Test
    public void listAvailableStorageLocationsMultiple() throws Exception {
        PID unitPid = makePid();
        mockAncestors(unitPid, getContentRootPid());

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addStorageLocation(LOC2_ID, LOC2_NAME, LOC2_BASE);
        helper.addMapping(getContentRootPid().getId(), LOC1_ID);
        helper.addMapping(unitPid.getId(), LOC2_ID);

        initializeManager();

        List<StorageLocation> locs = locManager.listAvailableStorageLocations(unitPid);
        assertEquals(2, locs.size());

        StorageLocation loc1 = findStorageLocationById(locs, LOC1_ID);
        assertIsLocation1(loc1);

        StorageLocation loc2 = findStorageLocationById(locs, LOC2_ID);
        assertIsLocation2(loc2);
    }

    @Test
    public void listAvailableStorageLocationsDuplicateLocation() throws Exception {
        PID unitPid = makePid();
        mockAncestors(unitPid, getContentRootPid());

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addMapping(getContentRootPid().getId(), LOC1_ID);
        helper.addMapping(unitPid.getId(), LOC1_ID);

        initializeManager();

        List<StorageLocation> locs = locManager.listAvailableStorageLocations(unitPid);
        assertEquals(1, locs.size());
        assertIsLocation1(locs.get(0));
    }

    @Test
    public void listAvailableStorageLocationsAssignedAfterCollection() throws Exception {
        PID unitPid = makePid();
        PID collPid = makePid();
        PID folderPid = makePid();
        mockAncestors(folderPid, getContentRootPid(), unitPid, collPid);

        helper.addStorageLocation(LOC1_ID, LOC1_NAME, LOC1_BASE);
        helper.addStorageLocation(LOC2_ID, LOC2_NAME, LOC2_BASE);
        helper.addMapping(collPid.getId(), LOC1_ID);
        helper.addMapping(folderPid.getId(), LOC2_ID);

        initializeManager();

        List<StorageLocation> locs = locManager.listAvailableStorageLocations(folderPid);
        assertEquals(1, locs.size());
        assertIsLocation1(locs.get(0));
    }

    private StorageLocation findStorageLocationById(List<StorageLocation> locs, String id) {
        return locs.stream().filter(l -> l.getId().equals(id)).findFirst().orElse(null);
    }

    private void assertIsLocation1(StorageLocation loc) {
        assertEquals(LOC1_ID, loc.getId());
        assertEquals(LOC1_NAME, loc.getName());
        assertEquals(StorageType.FILESYSTEM, loc.getStorageType());
        assertTrue(loc.isValidUri(URI.create(LOC1_BASE + "/afile")));
    }

    private void assertIsLocation2(StorageLocation loc) {
        assertEquals(LOC2_ID, loc.getId());
        assertEquals(LOC2_NAME, loc.getName());
        assertEquals(StorageType.FILESYSTEM, loc.getStorageType());
        assertTrue(loc.isValidUri(URI.create(LOC2_BASE + "/bfile")));
    }

    private void mockAncestors(PID target, PID... ancestors) {
        when(pathFactory.getAncestorPids(target)).thenReturn(
                new ArrayList<>(asList(ancestors)));
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
