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
package edu.unc.lib.dl.admin.collect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.MapType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

/**
 * Collects files deposit bins on the file system and adds them to the ingest queue. Bins are defined by a configuration
 * file, which describes source directories, deposit destinations and file filters.
 *
 * @author bbpennel
 * @date Jun 13, 2014
 */
public class DepositBinCollector {

    private static final Logger log = LoggerFactory.getLogger(DepositBinCollector.class);

    public static final String EXTRA_PERMISSION_GROUPS = "permissionGroups";
    public static final String EXTRA_DEPOSITOR_NAME = "depositorName";
    public static final String EXTRA_OWNER_NAME = "ownerName";
    public static final String EXTRA_DEPOSITOR_EMAIL = "depositorEmail";

    private Map<String, DepositBinConfiguration> configs;
    private String configPath;

    @Autowired
    private File depositsDirectory;
    @Autowired
    private DepositStatusFactory depositStatusFactory;

    public void init() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        MapType mapType = mapper.getTypeFactory()
                .constructMapType(Map.class, String.class, DepositBinConfiguration.class);
        configs = mapper.readValue(new File(configPath), mapType);
    }

    public DepositBinConfiguration getConfiguration(String binKey) {
        return configs.get(binKey);
    }

    public Map<String, DepositBinConfiguration> getBinConfigurations() {
        return configs;
    }

    /**
     * Given a bin key, returns a list of all files within the bin directory
     * which match the configuration's file filters
     *
     * @param binKey
     * @return
     */
    public ListFilesResult listFiles(String binKey) {

        final DepositBinConfiguration config = getConfiguration(binKey);
        if (config == null) {
            return null;
        }

        ListFilesResult result = new ListFilesResult();

        for (String binPath : config.getPaths()) {
            File binDirectory = new File(binPath);
            if (!binDirectory.exists() || !binDirectory.isDirectory()) {
                log.warn("Bin configuration for {} references an invalid directory path {}", binKey, binDirectory);
                return null;
            }

            // Add files to lists for applicable or non-applicable files
            boolean hasFileFilters = config.hasFileFilters();
            File listedFiles[] = binDirectory.listFiles();
            for (File listedFile : listedFiles) {
                if (hasFileFilters && !applicableFile(listedFile, config)) {
                    result.nonapplicable.add(listedFile);
                } else {
                    result.applicable.add(listedFile);
                }
            }

        }

        return result;
    }

    /**
     * Begin deposit of a list of files according to the configuration specified by binKey.
     *
     * @param filePathStrings
     * @param binKey
     * @throws IOException
     *            thrown if any of the files specified are invalid targets according to the bin config
     */
    public void collect(List<String> filePathStrings, String binKey, Map<String, String> extras) throws IOException {
        DepositBinConfiguration config = getConfiguration(binKey);
        if (config == null) {
            log.warn("Invalid bin specified: {}", binKey);
            return;
        }

        try {
            config.getKeyLock().lock();

            List<File> targetFiles;
            // Use the provided list of file paths or the list of all applicable paths
            if (filePathStrings != null) {
                targetFiles = new ArrayList<File>(filePathStrings.size());

                // Verify that all targets are valid
                for (String filePathString : filePathStrings) {
                    File file = new File(filePathString);

                    if (!(file.exists() && applicableFile(file, config) && pathInBins(file, config.getPaths()))) {
                        throw new IOException("Specified file was not a valid target for bin " + config.getName() + ": "
                                + file.getAbsolutePath());
                    }

                    targetFiles.add(file);
                }
            } else {
                // Use the list of all applicable files in the bin
                ListFilesResult fileList = this.listFiles(binKey);
                targetFiles = new ArrayList<File>(fileList.applicable);
            }

            // Create deposit directory
            DepositInfo info = makeDeposit();

            // Move the files for ingest into the deposit directory
            moveFiles(targetFiles, info, config);

            // Inform ingest queue that the files are ready for ingest
            registerDeposit(info, config, extras);
        } finally {
            config.getKeyLock().unlock();
        }
    }

    private DepositInfo makeDeposit() {

        DepositInfo info = new DepositInfo();

        UUID depositUUID = UUID.randomUUID();
        info.depositPID = new PID("uuid:" + depositUUID.toString());

        info.depositDir = new File(depositsDirectory, info.depositPID.getUUID());
        info.depositDir.mkdir();

        info.dataDir = new File(info.depositDir, "data");
        info.dataDir.mkdir();

        return info;
    }

    private void registerDeposit(DepositInfo info, DepositBinConfiguration config, Map<String, String> extras) {
        Map<String, String> status = new HashMap<String, String>();
        status.put(DepositField.packagingType.name(), config.getPackageType());

        status.put(DepositField.uuid.name(), info.depositPID.getUUID());
        status.put(DepositField.submitTime.name(), String.valueOf(System.currentTimeMillis()));
        String depositorName = extras.get(EXTRA_DEPOSITOR_NAME);
        String owner = extras.get(EXTRA_OWNER_NAME);
        status.put(DepositField.depositorName.name(), depositorName != null ? depositorName : owner);
        status.put(DepositField.depositorEmail.name(), extras.get(EXTRA_DEPOSITOR_EMAIL));
        status.put(DepositField.containerId.name(), config.getDestination());
        status.put(DepositField.depositMethod.name(), DepositMethod.CDRCollector.getLabel());
        status.put(DepositField.publishObjects.name(), Boolean.toString(config.isPublishObjects()));

        status.put(DepositField.permissionGroups.name(), extras.get(EXTRA_PERMISSION_GROUPS));

        status.put(DepositField.state.name(), DepositState.unregistered.name());
        status.put(DepositField.actionRequest.name(), DepositAction.register.name());

        Set<String> nulls = new HashSet<String>();
        for (String key : status.keySet()) {
            if (status.get(key) == null) {
                nulls.add(key);
            }
        }
        for (String key : nulls) {
            status.remove(key);
        }
        this.depositStatusFactory.save(info.depositPID.getUUID(), status);
    }

    /**
     * Moves a list of files to a destination location after verifying that they were safely copied
     *
     * @param targetFiles
     * @param destination
     * @throws IOException
     */
    private void moveFiles(List<File> targetFiles, DepositInfo depositInfo, DepositBinConfiguration config)
            throws IOException {

        try {
            // Copy the specified files into the destination path
            for (File file : targetFiles) {
                FileUtils.copyFileToDirectory(file, depositInfo.dataDir, true);
            }

            // Verify that the original files all copied over to the destination
            for (File file : targetFiles) {
                File destinationFile = new File(depositInfo.dataDir, file.getName());

                if (!FileUtils.contentEquals(file, destinationFile)) {
                    throw new IOException("Copied file " + destinationFile.toString() +
                            " did not match the original file " + file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            FileUtils.deleteDirectory(depositInfo.depositDir);
            throw new IOException("Failed to copy bin files to deposit directory, aborting and cleaning up", e);
        }

        // Clean up the original copies of the files
        for (File file : targetFiles) {
            boolean success = file.delete();
            if (!success) {
                log.warn("Failed to cleanup file {}", file.getAbsolutePath());
            }
        }
    }

    private boolean pathInBins(File file, List<String> binPaths) {
        String path = file.getAbsolutePath();
        for (String binPath : binPaths) {
            if (path.startsWith(binPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean applicableFile(File file, DepositBinConfiguration config) {
        if (!config.hasFileFilters()) {
            return true;
        }

        // Check that the file name is acceptable
        boolean answer = config.getFilePattern() == null || config.getFilePattern().matcher(file.getName()).matches();

        // Check that the file is not too big
        answer = answer
                && (config.getMaxBytesPerFile() == null || config.getMaxBytesPerFile() == 0 || file.length() <= config
                        .getMaxBytesPerFile());

        if (answer) {
            return true;
        }

        return false;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    private class DepositInfo {
        public PID depositPID;
        public File depositDir;
        public File dataDir;
    }

    public class ListFilesResult {
        public List<File> applicable = new ArrayList<File>();
        public List<File> nonapplicable = new ArrayList<File>();
    }
}
