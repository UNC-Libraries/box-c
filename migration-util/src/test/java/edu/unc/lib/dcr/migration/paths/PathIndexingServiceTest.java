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
package edu.unc.lib.dcr.migration.paths;

import static edu.unc.lib.dcr.migration.paths.PathIndex.FITS_TYPE;
import static edu.unc.lib.dcr.migration.paths.PathIndex.MANIFEST_TYPE;
import static edu.unc.lib.dcr.migration.paths.PathIndex.OBJECT_TYPE;
import static edu.unc.lib.dcr.migration.paths.PathIndex.ORIGINAL_TYPE;
import static edu.unc.lib.dcr.migration.paths.PathIndex.PREMIS_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 */
public class PathIndexingServiceTest {

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private PathIndex pathIndex;

    private PathIndexingService service;

    @Before
    public void setup() {
        pathIndex = new PathIndex();
        pathIndex.setDatabaseUrl("mem:test;DB_CLOSE_DELAY=-1");

        service = new PathIndexingService();
        service.setPathIndex(pathIndex);

        service.createIndexTable();
    }

    @Test
    public void indexObjects() throws Exception {
        String uuid1 = "8f7e4761-bc50-48bf-9335-f89c4854feb5";
        String path1 = "/path/to/fedora/objects/2010/0330/16/58/uuid_8f7e4761-bc50-48bf-9335-f89c4854feb5";
        String uuid2 = "09f6b320-d183-480c-a93a-d9a6c4a8fd2e";
        String path2 = "/path/to/fedora/objects/2016/1122/01/09/uuid_09f6b320-d183-480c-a93a-d9a6c4a8fd2e";
        Path listPath = createListFile(
                path1,
                path2);

        service.indexObjects(listPath);

        assertPathEquals(path1, pathIndex.getPath(PIDs.get(uuid1)));
        assertPathEquals(path2, pathIndex.getPath(PIDs.get(uuid2)));
        assertEquals(2, pathIndex.countFiles(OBJECT_TYPE));
    }

    @Test
    public void indexObjectsWithNonUuid() throws Exception {
        String path1 = "/path/to/fedora/objects/2010/0330/16/58/admin_REPOSITORY_SOFTWARE";
        String uuid2 = "09f6b320-d183-480c-a93a-d9a6c4a8fd2e";
        String path2 = "/path/to/fedora/objects/2016/1122/01/09/uuid_09f6b320-d183-480c-a93a-d9a6c4a8fd2e";
        Path listPath = createListFile(
                path1,
                path2);

        service.indexObjects(listPath);

        assertPathEquals(path2, pathIndex.getPath(PIDs.get(uuid2)));
        assertEquals(1, pathIndex.countFiles(OBJECT_TYPE));
    }

    @Test
    public void indexDatastreams() throws Exception {
        PID pid1 = PIDs.get("de4a7ac8-acd2-40cc-9ac2-213519f00a90");
        String path1 = "/path/to/fedora/datastreams/2010/0330/16/58/uuid_de4a7ac8-acd2-40cc-9ac2-213519f00a90+MD_EVENTS+MD_EVENTS.2";
        PID pid2 = PIDs.get("06dc7e72-7c12-412b-bc05-75077316ec06");
        String path2 = "/path/to/fedora/datastreams/2016/1122/01/09/uuid_06dc7e72-7c12-412b-bc05-75077316ec06+DATA_FILE+DATA_FILE.0";
        PID pid3 = PIDs.get("5653dff7-49eb-448f-927e-c4d81b3d80d1");
        String path3 = "/path/to/fedora/datastreams/2012/0322/10/18/uuid_5653dff7-49eb-448f-927e-c4d81b3d80d1+DATA_MANIFEST+DATA_MANIFEST.0";
        PID pid4 = PIDs.get("bb95a9df-7bc1-41dc-a7fb-e7fd2a60c0ef");
        String path4 = "/path/to/fedora/datastreams/2013/0306/11/09/uuid_bb95a9df-7bc1-41dc-a7fb-e7fd2a60c0ef+MD_TECHNICAL+MD_TECHNICAL.0";
        Path listPath = createListFile(
                path1,
                path2,
                path3,
                path4);

        service.indexDatastreams(listPath);

        assertPathEquals(path1, pathIndex.getPath(pid1, PREMIS_TYPE));
        assertPathEquals(path2, pathIndex.getPath(pid2, ORIGINAL_TYPE));
        assertPathEquals(path3, pathIndex.getPath(pid3, MANIFEST_TYPE));
        assertPathEquals(path4, pathIndex.getPath(pid4, FITS_TYPE));
        assertEquals(1, pathIndex.countFiles(PREMIS_TYPE));
        assertEquals(1, pathIndex.countFiles(FITS_TYPE));
        assertEquals(1, pathIndex.countFiles(ORIGINAL_TYPE));
        assertEquals(1, pathIndex.countFiles(MANIFEST_TYPE));
    }

    @Test
    public void indexDatastreamsWithIgnoredType() throws Exception {
        PID pid1 = PIDs.get("de4a7ac8-acd2-40cc-9ac2-213519f00a90");
        String path1 = "/path/to/fedora/datastreams/2010/0330/16/58/uuid_de4a7ac8-acd2-40cc-9ac2-213519f00a90+MD_EVENTS+MD_EVENTS.2";
        String path2 = "/path/to/fedora/datastreams/2013/0306/11/19/uuid_88c30f9e-50d2-4fe7-ac84-91640af6ea57+IMAGE_JP2000+IMAGE_JP2000.0";

        Path listPath = createListFile(
                path1,
                path2);

        service.indexDatastreams(listPath);

        assertPathEquals(path1, pathIndex.getPath(pid1, PREMIS_TYPE));
        assertEquals(1, pathIndex.countFiles());
    }

    @Test
    public void indexMultipleSameObject() throws Exception {
        PID pid1 = PIDs.get("de4a7ac8-acd2-40cc-9ac2-213519f00a90");
        PID pid2 = PIDs.get("06dc7e72-7c12-412b-bc05-75077316ec06");

        String pathObject = "/path/to/fedora/objects/2016/1122/01/09/uuid_de4a7ac8-acd2-40cc-9ac2-213519f00a90";
        Path objectListPath = createListFile(pathObject);

        service.indexObjects(objectListPath);

        String path1 = "/path/to/fedora/datastreams/2016/1122/01/09/uuid_de4a7ac8-acd2-40cc-9ac2-213519f00a90+MD_EVENTS+MD_EVENTS.1";
        String path2 = "/path/to/fedora/datastreams/2016/1122/01/09/uuid_de4a7ac8-acd2-40cc-9ac2-213519f00a90+DATA_FILE+DATA_FILE.0";
        String path3 = "/path/to/fedora/datastreams/2016/1122/01/09/uuid_de4a7ac8-acd2-40cc-9ac2-213519f00a90+MD_TECHNICAL+MD_TECHNICAL.0";
        String pathUnaffiliated = "/path/to/fedora/datastreams/2016/1122/01/08/uuid_06dc7e72-7c12-412b-bc05-75077316ec06+DATA_FILE+DATA_FILE.0";
        Path dsListPath = createListFile(
                path1,
                path2,
                path3,
                pathUnaffiliated);

        service.indexDatastreams(dsListPath);

        Map<Integer, Path> objPaths = pathIndex.getPaths(pid1);
        assertEquals(4, objPaths.size());
        assertPathEquals(pathObject, objPaths.get(OBJECT_TYPE));
        assertPathEquals(path1, objPaths.get(PREMIS_TYPE));
        assertPathEquals(path2, objPaths.get(ORIGINAL_TYPE));
        assertPathEquals(path3, objPaths.get(FITS_TYPE));

        assertPathEquals(pathUnaffiliated, pathIndex.getPath(pid2, ORIGINAL_TYPE));
    }

    @Test
    public void indexMultipleVersionsSameObject() throws Exception {
        PID pid1 = PIDs.get("de4a7ac8-acd2-40cc-9ac2-213519f00a90");

        String path1 = "/path/to/fedora/datastreams/2016/1122/01/09/uuid_de4a7ac8-acd2-40cc-9ac2-213519f00a90+DATA_FILE+DATA_FILE.1";
        String path2 = "/path/to/fedora/datastreams/2016/1122/01/09/uuid_de4a7ac8-acd2-40cc-9ac2-213519f00a90+DATA_FILE+DATA_FILE.2";
        String path3 = "/path/to/fedora/datastreams/2016/1122/01/09/uuid_de4a7ac8-acd2-40cc-9ac2-213519f00a90+DATA_FILE+DATA_FILE.0";
        Path dsListPath = createListFile(path1, path2, path3);

        service.indexDatastreams(dsListPath);

        Path originalPath = pathIndex.getPath(pid1, ORIGINAL_TYPE);
        assertPathEquals(path2, originalPath);

        List<Path> allOriginals = pathIndex.getPathVersions(pid1, ORIGINAL_TYPE);
        assertEquals(3, allOriginals.size());
        assertTrue(allOriginals.containsAll(asList(Paths.get(path1), Paths.get(path2), Paths.get(path3))));
    }

    private Path createListFile(String... paths) throws Exception {
        File listFile = tmpDir.newFile();
        FileUtils.writeStringToFile(listFile, String.join("\n", paths), UTF_8);
        return listFile.toPath();
    }

    private void assertPathEquals(String expected, Path result) {
        assertEquals(expected, result.toString());
    }
}
