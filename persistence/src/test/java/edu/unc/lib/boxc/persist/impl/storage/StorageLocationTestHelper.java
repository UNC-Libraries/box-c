package edu.unc.lib.boxc.persist.impl.storage;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static java.lang.Runtime.getRuntime;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.services.ContentPathFactoryImpl;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationManagerImpl.StorageLocationMapping;

/**
 * @author bbpennel
 *
 */
public class StorageLocationTestHelper {
    public final static String LOC1_ID = "loc1";

    private List<StorageLocationMapping> mappingList;
    private List<Map<String, String>> locationList;
    // Hardcoded base storage path for usage across tests and containerized fedora
    private Path baseStoragePath = Paths.get("/tmp/boxc_test_storage");
    private StorageLocationManager locationManager;

    public StorageLocationTestHelper() {
        mappingList = new ArrayList<>();
        locationList = new ArrayList<>();
    }

    public static StorageLocationTestHelper newStorageLocationTestHelper() {
        return new StorageLocationTestHelper();
    }

    public void addStorageLocation(String id, String name, String base) throws IOException {
        Map<String, String> info = new HashMap<>();
        info.put("id", id);
        info.put("name", name);
        info.put("type", HashedFilesystemStorageLocation.TYPE_NAME);
        info.put("base", base);

        locationList.add(info);
    }

    public void addMapping(String id, String defaultLoc) {
        StorageLocationMapping mapping = new StorageLocationMapping();
        mapping.setId(id);
        mapping.setDefaultLocation(defaultLoc);
        mappingList.add(mapping);
    }

    public String serializeLocationConfig() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        File jsonFile = Files.createTempFile("locConfig", ".json").toFile();
        objectMapper.writeValue(jsonFile, locationList);
        return jsonFile.getAbsolutePath();
    }

    public String serializeLocationMappings() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        File jsonFile = Files.createTempFile("locMapping", ".json").toFile();
        objectMapper.writeValue(jsonFile, mappingList);
        return jsonFile.getAbsolutePath();
    }

    /**
     * Generates a basic test location mapped to the provided id and storage path
     *
     * @param mappedPid
     * @param locPath
     * @return
     * @throws Exception
     */
    public StorageLocationTestHelper addTestLocation(PID mappedPid, Path locPath) throws Exception {
        addStorageLocation(LOC1_ID, "Location 1", locPath.toString());
        addMapping(mappedPid.getId(), LOC1_ID);
        return this;
    }

    /**
     * Generate a test location assigned to the root of the repository in a newly created temp dir
     *
     * @return
     * @throws Exception
     */
    public StorageLocationTestHelper addTestLocation() throws Exception {
        if (Files.notExists(baseStoragePath)) {
            Files.createDirectories(baseStoragePath);
        }
        Path locPath = createTempDirectory(baseStoragePath, "loc1");
        // Cleanup the temp dir after the tests end
        getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    deleteDirectory(locPath.toFile());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        addStorageLocation(LOC1_ID, "Location 1", locPath.toString());
        addMapping(getContentRootPid().getId(), LOC1_ID);
        return this;
    }

    /**
     * Construct a storage location manager with a mocked content path factory which
     * returns the root of the repo as the ancestor for everything.
     *
     * @param repoObjLoader
     * @return
     * @throws Exception
     */
    public StorageLocationManager createLocationManager(RepositoryObjectLoader repoObjLoader)
            throws Exception {
        ContentPathFactory pathFactory = mock(ContentPathFactoryImpl.class);
        when(pathFactory.getAncestorPids(any(PID.class))).thenReturn(new ArrayList<>(asList((getContentRootPid()))));

        return createLocationManager(repoObjLoader, pathFactory);
    }

    /**
     * Construct and initialize a storage location manager from the config in this helper using
     * the provided dependencies
     *
     * @param repoObjLoader
     * @param pathFactory
     * @return
     * @throws Exception
     */
    public StorageLocationManager createLocationManager(RepositoryObjectLoader repoObjLoader,
            ContentPathFactory pathFactory) throws Exception {
        var locationManager = new StorageLocationManagerImpl();
        locationManager.setConfigPath(serializeLocationConfig());
        locationManager.setMappingPath(serializeLocationMappings());
        locationManager.setRepositoryObjectLoader(repoObjLoader);
        locationManager.setPathFactory(pathFactory);
        locationManager.init();
        this.locationManager = locationManager;
        return locationManager;
    }

    public static StorageLocationManager createLocationManagerWithBasicConfig(RepositoryObjectLoader repoObjLoader)
            throws Exception {

        return newStorageLocationTestHelper()
                    .addTestLocation()
                    .createLocationManager(repoObjLoader);
    }

    public static StorageLocationTestHelper createWithBasicConfig() throws Exception {
        return newStorageLocationTestHelper()
                .addTestLocation();
    }

    /**
     * Get the base storage location path for the first location in the list
     * @return
     */
    public Path getFirstStorageLocationPath() {
        return Paths.get(locationList.get(0).get("base"));
    }

    /**
     * Create a binary file in the test storage location for the given PID with the given content
     * @param pid
     * @param content
     * @return
     * @throws IOException
     */
    public URI createTestBinary(PID pid, String content) throws IOException {
        URI binaryUri = makeTestStorageUri(pid);
        Files.write(Path.of(binaryUri), content.getBytes());
        return binaryUri;
    }

    public URI makeTestStorageUri(PID pid) throws IOException {
        var storageLoc = locationManager.getStorageLocationById(LOC1_ID);
        URI binaryUri = storageLoc.getNewStorageUri(pid);
        Path binaryPath = Paths.get(binaryUri);
        Files.createDirectories(binaryPath.getParent());
        return binaryUri;
    }

    public void cleanupStorageLocations() throws IOException {
        try {
            for (Map<String, String> location : locationList) {
                Path locPath = Paths.get(location.get("base"));
                deleteDirectory(locPath.toFile());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public StorageLocation getTestStorageLocation() {
        return locationManager.getStorageLocationById(LOC1_ID);
    }

    public StorageLocationManager getLocationManager() {
        return locationManager;
    }

    public void setBaseStoragePath(Path baseStoragePath) {
        this.baseStoragePath = baseStoragePath;
    }

    public Path getBaseStoragePath() {
        return baseStoragePath;
    }
}
