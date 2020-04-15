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

import static edu.unc.lib.dcr.migration.MigrationConstants.FITS_DS;
import static edu.unc.lib.dcr.migration.MigrationConstants.MANIFEST_DS;
import static edu.unc.lib.dcr.migration.MigrationConstants.ORIGINAL_DS;
import static edu.unc.lib.dcr.migration.MigrationConstants.PREMIS_DS;
import static edu.unc.lib.dcr.migration.MigrationConstants.extractUUIDFromPath;
import static edu.unc.lib.dcr.migration.paths.PathIndex.FITS_TYPE;
import static edu.unc.lib.dcr.migration.paths.PathIndex.MANIFEST_TYPE;
import static edu.unc.lib.dcr.migration.paths.PathIndex.OBJECT_TYPE;
import static edu.unc.lib.dcr.migration.paths.PathIndex.ORIGINAL_TYPE;
import static edu.unc.lib.dcr.migration.paths.PathIndex.PREMIS_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import edu.unc.lib.dl.exceptions.RepositoryException;

/**
 * Creates an index for the lookup of file paths
 *
 * @author bbpennel
 *
 */
public class PathIndexingService {
    private static final Logger log = getLogger(PathIndexingService.class);

    private static final String INSERT_PATH_QUERY =
            "insert into PathIndex values(?, ?, ?)";

    private PathIndex pathIndex;

    /**
     * Index object files
     *
     * @param listPath path to the file listing object file paths
     */
    public void indexObjects(Path listPath) {
        try (Stream<String> stream = Files.lines(listPath)) {
            indexObjects(stream);
        } catch (IOException e) {
            throw new RepositoryException("Failed to open list file " + listPath, e);
        }
    }

    /**
     * Index object files found within the given path, recursively.
     *
     * @param objsPath Path to directory containing object files
     */
    public void indexObjectsFromPath(Path objsPath) {
        try (Stream<Path> walk = Files.walk(objsPath)) {
            Stream<String> dsStream = walk
                    .filter(Files::isRegularFile)
                    .map(Path::toString);

            indexObjects(dsStream);
        } catch (IOException e) {
            throw new RepositoryException("Failed to list object files from " + objsPath, e);
        }
    }

    private void indexObjects(Stream<String> objsStream) {
        try (Connection conn = pathIndex.getConnection()) {
            objsStream.forEach(filePath -> insertEntry(conn, filePath, OBJECT_TYPE));
        } catch (SQLException e) {
            throw new RepositoryException("Database connection error", e);
        }
    }

    /**
     * Index datastream files
     *
     * @param listPath path to the file listing datastream file paths
     */
    public void indexDatastreams(Path listPath) {
        try (Stream<String> stream = Files.lines(listPath)) {
            indexDatastreams(stream);
        } catch (IOException e) {
            throw new RepositoryException("Failed to open list file " + listPath, e);
        }
    }

    /**
     * Index datastream files found within the given path, recursively.
     *
     * @param datastreamsPath Path to directory containing ds files
     */
    public void indexDatastreamsFromPath(Path datastreamsPath) {
        try (Stream<Path> walk = Files.walk(datastreamsPath)) {
            Stream<String> dsStream = walk
                    .filter(Files::isRegularFile)
                    .map(Path::toString);

            indexDatastreams(dsStream);
        } catch (IOException e) {
            throw new RepositoryException("Failed to list datastreams files from " + datastreamsPath, e);
        }
    }

    private void indexDatastreams(Stream<String> dsStream) {
        try (Connection conn = pathIndex.getConnection()) {
            dsStream.forEach(filePath -> {
                int objectType = getFileType(filePath);
                if (objectType > 0) {
                    insertEntry(conn, filePath, objectType);
                } else {
                    log.debug("Skipping datastream {}", filePath);
                }
            });
        } catch (SQLException e) {
            throw new RepositoryException("Database connection error", e);
        }
    }

    /**
     * Create the table for the index
     */
    public void createIndexTable() {
        try (Connection conn = pathIndex.getConnection()) {
            String createTableSql = IOUtils.toString(getClass().getResourceAsStream("/create_path_index.sql"), UTF_8);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSql);
            }
        } catch (IOException | SQLException e) {
            throw new RepositoryException("Failed to create table", e);
        }
    }

    private void insertEntry(Connection conn, String path, int fileType) {
        String uuid;
        try {
            uuid = extractUUIDFromPath(path);
        } catch (IllegalArgumentException e) {
            log.info("Skipping path not containing a UUID: {}", path);
            return;
        }

        try (PreparedStatement insert = conn.prepareStatement(INSERT_PATH_QUERY)) {
            insert.setString(1, path);
            insert.setString(2, uuid);
            insert.setInt(3, fileType);
            insert.execute();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert entry for " + path, e);
        }
    }

    private int getFileType(String path) {
        if (path.contains(PREMIS_DS)) {
            return PREMIS_TYPE;
        }
        if (path.contains(ORIGINAL_DS)) {
            return ORIGINAL_TYPE;
        }
        if (path.contains(FITS_DS)) {
            return FITS_TYPE;
        }
        if (path.contains(MANIFEST_DS)) {
            return MANIFEST_TYPE;
        }
        return -1;
    }

    public void setPathIndex(PathIndex pathIndex) {
        this.pathIndex = pathIndex;
    }
}
