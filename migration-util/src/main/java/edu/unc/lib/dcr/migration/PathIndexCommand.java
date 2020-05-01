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
package edu.unc.lib.dcr.migration;

import static edu.unc.lib.dcr.migration.MigrationConstants.OUTPUT_LOGGER;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

import edu.unc.lib.dcr.migration.paths.PathIndex;
import edu.unc.lib.dcr.migration.paths.PathIndexingService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Commands for populating or pulling data from the path index
 *
 * @author bbpennel
 */
@Command(name = "path_index", aliases = {"pi"},
    description = "Interact with the index of paths for migration")
public class PathIndexCommand implements Callable<Integer> {

    private static final Logger output = getLogger(OUTPUT_LOGGER);

    private PathIndex pathIndex;

    @ParentCommand
    private MigrationCLI parentCommand;

    @Command(name = "populate",
            description = "Populate the index of file paths")
    public int populateIndex(
            @Parameters(index = "0", description = "Path to file listing FOXML documents.")
            Path objectListPath,
            @Parameters(index = "1", description = "Path to file listing datastream files.")
            Path dsListPath,
            @Option(names = {"-l", "--list-dirs"}, description =
                    "If set, the object and datastream paths will be treated as"
                    + " directories, recursively indexing the files within them")
            boolean listDirectories) {

        long start = System.currentTimeMillis();
        PathIndexingService service = getPathIndexingService();

        output.info(BannerUtility.getChompBanner("Populating path index"));

        output.info("Creating index at path {}", parentCommand.databaseUrl);
        service.createIndexTable();
        output.info("Populating object files from {}", objectListPath);
        if (listDirectories) {
            service.indexObjectsFromPath(objectListPath);
        } else {
            service.indexObjects(objectListPath);
        }
        output.info("Populating datastream files from {}", dsListPath);
        if (listDirectories) {
            service.indexDatastreamsFromPath(dsListPath);
        } else {
            service.indexDatastreams(dsListPath);
        }
        output.info("Finished populating index in {}ms", System.currentTimeMillis() - start);
        output.info("{} files were indexed", getPathIndex().countFiles());
        pathIndex.close();

        return 0;
    }

    @Command(name = "get_paths",
            description = "Get all paths for a uuid")
    public int getPaths(
            @Parameters(index = "0", description = "UUID of the object to get paths for")
            String uuid) {

        output.info("Paths for {}:", uuid);
        String paths = getPathIndex().getPaths(uuid).values().stream()
                .map(Path::toString)
                .collect(joining("\n"));
        output.info(paths);

        getPathIndex().close();

        return 0;
    }

    @Command(name = "list_paths",
            description = "List file paths for objects and datastreams")
    public int listPaths(
            @Option(names = { "-p", "--pids-file" }, description = "File containing the list of object ids to list")
            Path listPath) {

        if (listPath == null) {
            output.info("Listing all paths:");
            String paths = getPathIndex().listPaths().stream()
                    .map(Path::toString)
                    .collect(joining("\n"));
            output.info(paths);
        } else {
            output.info("Listing paths objects listed in {}:", listPath);
            try {
                Files.lines(listPath).forEach(id -> {
                    id = id.replace("uuid:", "");
                    String paths = getPathIndex().getPaths(id).values().stream()
                            .map(Path::toString)
                            .collect(joining("\n"));
                    output.info(paths);
                });
            } catch (IOException e) {
                output.error("Failed to read list file: {}", e.getMessage());
            }
        }

        getPathIndex().close();

        return 0;
    }

    @Command(name = "delete",
            description = "Delete the index and all its files")
    public int deleteIndex() {
        output.info(BannerUtility.getChompBanner("Deleting path index"));

        getPathIndex().deleteIndex();
        return 0;
    }

    @Command(name = "count",
            description = "Count the files indexed")
    public int countFiles(
            @Option(names = {"-t"}, description = "Only count files of the specified type")
            String type) {

        PathIndex index = getPathIndex();

        try {
            if (type == null) {
                output.info("{} files are indexed", index.countFiles());
            } else {
                output.info("{} files of type {} are indexed", index.countFiles(Integer.parseInt(type)), type);
            }
        } finally {
            index.close();
        }

        return 0;
    }

    @Override
    public Integer call() throws Exception {
        return 0;
    }

    private PathIndex getPathIndex() {
        if (pathIndex == null) {
            pathIndex = new PathIndex();
            pathIndex.setDatabaseUrl(parentCommand.databaseUrl);
        }
        return pathIndex;
    }

    private PathIndexingService getPathIndexingService() {
        PathIndexingService service = new PathIndexingService();
        service.setPathIndex(getPathIndex());
        return service;
    }
}
