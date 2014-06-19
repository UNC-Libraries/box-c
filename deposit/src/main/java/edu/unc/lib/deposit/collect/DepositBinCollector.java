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
package edu.unc.lib.deposit.collect;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
		configs = mapper.readValue(this.getClass().getResourceAsStream(configPath), mapType);
	}

	public DepositBinConfiguration getConfiguration(String binKey) {
		return configs.get(binKey);
	}

	/**
	 * Given a bin key, returns a list of all files within the bin directory which match the configuration's file filters
	 *
	 * @param binKey
	 * @return
	 */
	public List<String> listFiles(String binKey) {

		final DepositBinConfiguration config = getConfiguration(binKey);
		if (config == null)
			return null;

		List<String> files = new ArrayList<>();

		for (Path binPath : config.getBinPaths()) {
			File binDirectory = binPath.toFile();
			if (!binDirectory.exists() || !binDirectory.isDirectory()) {
				log.warn("Bin configuration for {} references an invalid directory path {}", binKey, binDirectory);
				return null;
			}

			File listedFiles[];
			// Add all files to the list when no file filter is configured
			if (!config.hasFileFilters()) {
				listedFiles = binDirectory.listFiles();
			} else {
				// Filter files
				listedFiles = binDirectory.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File directory, String fileName) {
						return applicableFile(new File(directory, fileName), config);
					}
				});
			}

			for (File listedFile : listedFiles) {
				files.add(listedFile.getAbsolutePath());
			}
		}

		return files;
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

			List<Path> filePaths;
			// Use the provided list of file paths or the list of all applicable paths
			if (filePathStrings != null) {
				filePaths = new ArrayList<>(filePathStrings.size());

				// Verify that all targets are valid
				for (String filePathString : filePathStrings) {
					File file = new File(filePathString);
					Path filePath = file.toPath();

					if (!(file.exists() && applicableFile(file, config) && pathInBins(filePath, config.getBinPaths()))) {
						throw new IOException("Specified file was not a valid target for bin " + config.getName() + ": "
								+ file.getAbsolutePath());
					}

					filePaths.add(filePath);
				}
			} else {
				// Use the list of all applicable files in the bin
				List<String> fileList = this.listFiles(binKey);
				filePaths = new ArrayList<>(fileList.size());

				for (String filePathString : fileList) {
					filePaths.add(Paths.get(filePathString));
				}
			}

			// Create deposit directory
			DepositInfo info = makeDeposit();

			// Move the files for ingest into the deposit directory
			moveFiles(filePaths, info, config);

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
		String depositorEmail = extras.get(EXTRA_DEPOSITOR_EMAIL);
		status.put(DepositField.depositorEmail.name(), depositorEmail != null ? depositorEmail : owner + "@email.unc.edu");
		status.put(DepositField.containerId.name(), config.getDestination());
		status.put(DepositField.depositMethod.name(), DepositMethod.CDRCollector.getLabel());

		status.put(DepositField.permissionGroups.name(), extras.get(EXTRA_PERMISSION_GROUPS));

		status.put(DepositField.state.name(), DepositState.unregistered.name());
		status.put(DepositField.actionRequest.name(), DepositAction.register.name());

		Set<String> nulls = new HashSet<String>();
		for (String key : status.keySet()) {
			if (status.get(key) == null)
				nulls.add(key);
		}
		for (String key : nulls)
			status.remove(key);
		this.depositStatusFactory.save(info.depositPID.getUUID(), status);
	}

	/**
	 * Moves a list of files to a destination location after verifying that they were safely copied
	 *
	 * @param filePaths
	 * @param destination
	 * @throws IOException
	 */
	private void moveFiles(List<Path> filePaths, DepositInfo depositInfo, DepositBinConfiguration config)
			throws IOException {

		Path destination = depositInfo.dataDir.toPath();
		try {
			// Copy the specified files into the destination path
			for (Path filePath : filePaths) {
				Files.copy(filePath, destination.resolve(filePath.getFileName()), COPY_ATTRIBUTES, NOFOLLOW_LINKS);
			}

			// Verify that the original files all copied over to the destination
			for (Path filePath : filePaths) {
				File destinationFile = destination.resolve(filePath.getFileName()).toFile();

				if (!FileUtils.contentEquals(filePath.toFile(), destinationFile)) {
					throw new IOException("Copied file " + destinationFile.toString() + " did not match the original file "
							+ filePath.toString());
				}
			}
		} catch (Exception e) {
			FileUtils.deleteDirectory(depositInfo.depositDir);
			throw new IOException("Failed to copy bin files to deposit directory, aborting and cleaning up", e);
		}

		try {
			// Clean up the original copies of the files
			for (Path filePath : filePaths) {
				Files.delete(filePath);
			}
		} catch (IOException e) {
			log.warn("Failed to clean up files moved from bin directory {}", e, config.getName());
		}
	}

	private boolean pathInBins(Path path, List<Path> binPaths) {
		for (Path binPath : binPaths) {
			if (path.startsWith(binPath))
				return true;
		}
		return false;
	}

	private boolean applicableFile(File file, DepositBinConfiguration config) {
		if (!config.hasFileFilters())
			return true;

		// Check that the file name is acceptable
		boolean answer = config.getFilePattern() == null || config.getFilePattern().matcher(file.getName()).matches();

		// Check that the file is not too big
		answer = answer
				&& (config.getMaxBytesPerFilee() == null || config.getMaxBytesPerFilee() == 0 || file.length() <= config
						.getMaxBytesPerFilee());

		if (answer)
			return true;

		log.warn("Non-applicable file {} found in bin {}", file.getAbsolutePath(), config.getName());
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
}
