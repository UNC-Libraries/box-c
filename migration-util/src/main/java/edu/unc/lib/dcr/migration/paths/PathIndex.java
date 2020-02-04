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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fedora.PID;

/**
 * Index for retrieving file paths for objects and datastreams
 *
 * @author bbpennel
 */
public class PathIndex {

    public static final int OBJECT_TYPE = 0;
    public static final int ORIGINAL_TYPE = 1;
    public static final int PREMIS_TYPE = 2;
    public static final int FITS_TYPE = 3;
    public static final int MANIFEST_TYPE = 4;

    private static final String SELECT_UUID_QUERY =
            "select path from PathIndex where uuid = ? and file_type = ?";
    private static final String SELECT_ALL_PATHS_UUID_QUERY =
            "select path, file_type from PathIndex where uuid = ?";
    private static final String COUNT_FILES_QUERY =
            "select COUNT(*) from PathIndex where file_type = ?";
    private static final String COUNT_ALL_FILES_QUERY =
            "select COUNT(*) from PathIndex";

    private String databaseUrl;

    private Connection connection;

    /**
     * Open a connection to the path index
     *
     * @return connection
     * @throws SQLException if unable to open connection
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:h2:" + databaseUrl, "test", "test");
        }
        return connection;
    }

    /**
     * Lookup the path for the specified object
     *
     * @param pid pid of the object to seek
     * @return the path, or null if not found
     */
    public Path getPath(PID pid) {
        return getPath(pid, OBJECT_TYPE);
    }

    /**
     * Lookup the path for the file of the provided type from the specified object
     *
     * @param pid pid of the object to seek
     * @param fileType the file type to retrieve
     * @return the path, or null if not found
     */
    public Path getPath(PID pid, int fileType) {
        try (PreparedStatement select = getConnection().prepareStatement(SELECT_UUID_QUERY)) {
            select.setString(1, pid.getId());
            select.setInt(2, fileType);
            try (ResultSet results = select.executeQuery()) {
                while (results.next()) {
                    return Paths.get(results.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to look up path for " + pid, e);
        }
        return null;
    }

    /**
     * Retrieve all the paths associated with an object
     *
     * @param pid
     * @return map of file type to
     */
    public Map<Integer, Path> getPaths(PID pid) {
        Map<Integer, Path> result = new HashMap<>();

        try (PreparedStatement select = getConnection().prepareStatement(SELECT_ALL_PATHS_UUID_QUERY)) {
            select.setString(1, pid.getId());
            try (ResultSet results = select.executeQuery()) {
                while (results.next()) {
                    result.put(Integer.valueOf(results.getInt(2)), Paths.get(results.getString(1)));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to look up path for " + pid, e);
        }
        return result;
    }

    /**
     * @return count of all indexed files
     */
    public long countFiles() {
        try (PreparedStatement stmt = getConnection().prepareStatement(COUNT_ALL_FILES_QUERY)) {
            try (ResultSet results = stmt.executeQuery()) {
                while (results.next()) {
                    return results.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to calculate file count", e);
        }
        throw new RepositoryException("Failed to calculate file count");
    }

    /**
     * @param fileType file type to count
     * @return count of indexed files of the provided type
     */
    public long countFiles(int fileType) {
        try (PreparedStatement stmt = getConnection().prepareStatement(COUNT_FILES_QUERY)) {
            stmt.setInt(1, fileType);
            try (ResultSet results = stmt.executeQuery()) {
                while (results.next()) {
                    return results.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to calculate counts for " + fileType, e);
        }
        throw new RepositoryException("Failed to calculate counts for " + fileType);
    }

    public void setDatabaseUrl(String url) {
        this.databaseUrl = url;
    }
}
