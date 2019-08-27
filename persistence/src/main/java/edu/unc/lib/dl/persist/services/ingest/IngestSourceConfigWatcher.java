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
package edu.unc.lib.dl.persist.services.ingest;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

/**
 * Loads ingest source configuration and watches for changes to the configuration file.
 *
 * @author bbpennel
 *
 */
public class IngestSourceConfigWatcher implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(IngestSourceConfigWatcher.class);

    private final File configFile;
    private final ObjectMapper mapper;
    private final CollectionType type;
    private final IngestSourceManager sourceManager;

    public IngestSourceConfigWatcher(String configPath, IngestSourceManager sourceManager) {
        this.sourceManager = sourceManager;
        configFile = new File(configPath);
        mapper = new ObjectMapper();
        type = mapper.getTypeFactory()
                .constructCollectionType(List.class, IngestSourceConfiguration.class);
    }

    public void loadConfig() throws IOException {
        sourceManager.setConfigs(mapper.readValue(configFile, type));
    }

    @Override
    public void run() {
        final Path path = configFile.toPath();
        // Monitor config file for changes to allow for reloading without restarts
        try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
            // Register watcher on parent directory of config to detect file modifications
            path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                final WatchKey wk = watchService.take();
                for (WatchEvent<?> event : wk.pollEvents()) {
                    final Path changed = (Path) event.context();

                    if (changed.toString().equals(configFile.getName())) {
                        log.warn("Ingest source configuration has changed, reloading: {}",
                                configFile.getAbsolutePath());
                        // Config file changed, reload the mappings
                        synchronized (sourceManager) {
                            loadConfig();
                        }
                    }
                }

                // reset the key so that we can continue monitor for future events
                boolean valid = wk.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.info("Interrupted watcher for updates to ingest source configuration");
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error("Failed to establish watcher for ingest source configuration");
        }
    }
}
