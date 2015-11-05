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
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipFile;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.CollectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import gov.loc.repository.bagit.BagHelper;

/**
 * Loads and manages ingest sources, which are preconfigured locations to find packages for deposit.
 * 
 * @author bbpennel
 * @date Oct 22, 2015
 */
public class IngestSourceManager {
	
	private static final Logger log = LoggerFactory.getLogger(IngestSourceManager.class);

	private List<IngestSourceConfiguration> configs;
	
	private TripleStoreQueryService tripleService;
	
	private String configPath;

	public void init() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		CollectionType type = mapper.getTypeFactory()
				.constructCollectionType(List.class, IngestSourceConfiguration.class);
		configs = mapper.readValue(new File(configPath), type);
	}

	/**
	 * Retrieves a list of ingest sources which contain or match the destination object provided.
	 * 
	 * @param destination
	 * @return
	 */
	public List<IngestSourceConfiguration> listSources(PID destination) {
		List<PID> ancestors = tripleService.lookupAllContainersAbove(destination);

		// Determine which sources apply to the selected destination
		List<IngestSourceConfiguration> applicableSources = new ArrayList<>();
		for (IngestSourceConfiguration source : configs) {
			for (String container : source.getContainers()) {
				PID containerPID = new PID(container);
				if (containerPID.equals(destination) || ancestors.contains(containerPID)) {
					applicableSources.add(source);
					continue;
				}
			}
		}

		return applicableSources;
	}

	/**
	 * Retrieves a list of candidate file information for ingestable packages from sources which are
	 * applicable to the destination provided.
	 * 
	 * @param destination
	 * @return
	 */
	public List<Map<String, Object>> listCandidates(PID destination) {

		List<IngestSourceConfiguration> applicableSources = listSources(destination);

		final List<Map<String, Object>> candidates = new ArrayList<>();
		for (final IngestSourceConfiguration source : applicableSources) {
			final String base = source.getBase();
			
			// Gathering candidates per pattern within a particular base directory
			for (String pattern : source.getPatterns()) {
				final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + base + pattern);
				try {
					Files.walkFileTree(Paths.get(base), new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							if (matcher.matches(dir)) {
								log.debug("Matched dir {} for source {}", dir, source.getId());
								addCandidate(candidates, dir, source, base);
								return FileVisitResult.SKIP_SUBTREE;
							}
							return FileVisitResult.CONTINUE;
						}
						
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							if (matcher.matches(file)) {
								log.debug("Matched file {} for source {}", file, source.getId());
								addCandidate(candidates, file, source, base);
							}
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					log.error("Failed to gather candidate files for source {}", source.getId(), e);
				}
			}
		}
		
		return candidates;
	}

	/**
	 * Adds information about applicable packages to the list of candidates
	 * 
	 * @param candidates
	 * @param filePath
	 * @param source
	 * @param base
	 * @throws IOException
	 */
	private void addCandidate(List<Map<String, Object>> candidates, Path filePath,
			IngestSourceConfiguration source, String base) throws IOException {
		
		File file = filePath.toFile();
		// Only bags are candidates currently
		String version = BagHelper.getVersion(file);
		if (version == null) {
			return;
		}
		
		Map<String, Object> candidate = new HashMap<>();
		
		candidate.put("sourceId", source.getId());
		candidate.put("base", base);
		candidate.put("patternMatched", Paths.get(base).relativize(filePath).toString());
		candidate.put("type", "bag");
		candidate.put("version", version);
		if (file.isDirectory()) {
			addDirectoryStats(candidate, filePath, true);
		} else {
			if (file.getName().endsWith(".zip")) {
				try (ZipFile zip = new ZipFile(file)) {
					candidate.put("files", zip.size());
				}
			}
			candidate.put("size", file.length());
		}
		
		candidates.add(candidate);
	}

	/**
	 * Add aggregate file statistics about a directory to a candidates information.  If an object is a
	 * bag folder, then only the files within its data directory will be added.
	 * 
	 * @param fileInfo
	 * @param filePath
	 * @param isBag
	 * @throws IOException
	 */
	private void addDirectoryStats(Map<String, Object> fileInfo, Path filePath, final boolean isBag) throws IOException {
		final AtomicLong size = new AtomicLong(0);
		final AtomicInteger count = new AtomicInteger(0);
		
		final PathMatcher bagDataMatcher = FileSystems.getDefault().getPathMatcher("glob:" + filePath + "/data/**");

		Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				size.addAndGet(attrs.size());
				if (isBag && bagDataMatcher.matches(file)) {
					count.incrementAndGet();
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				// Skip folders that can't be traversed
				return FileVisitResult.CONTINUE;
			}
		});
		
		fileInfo.put("size", size.get());
		fileInfo.put("files", count.get());
	}

	public void setConfigs(List<IngestSourceConfiguration> configs) {
		this.configs = configs;
	}

	public void setTripleService(TripleStoreQueryService tripleService) {
		this.tripleService = tripleService;
	}

	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}
}
