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
package edu.unc.lib.boxc.model.fcrepo.services;

import static edu.unc.lib.boxc.model.api.DatastreamType.getByIdentifier;
import static edu.unc.lib.boxc.model.api.StoragePolicy.EXTERNAL;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static org.springframework.util.Assert.notNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Service which locates and returns file information about derivatives for
 * repository objects.
 *
 * @author bbpennel
 *
 */
public class DerivativeService {

    private String derivativeDir;

    private volatile static List<DatastreamType> derivativeTypes;

    public DerivativeService() {
    }

    /**
     * Gets the derivative of type dsType for the object identified by pid.
     *
     * @param pid pid of the object. Required.
     * @param dsType type of the derivative to retrieve.
     * @return Derivative of type dsType for object pid, or null if the
     *         derivative does not exist.
     */
    public Derivative getDerivative(PID pid, DatastreamType dsType) {
        Path derivPath = getDerivativePath(pid, dsType);

        // If the derivative file does not exist, then return no result
        if (Files.notExists(derivPath)) {
            return null;
        }

        return new Derivative(dsType, derivPath.toFile());
    }

    /**
     * Gets the path where the specified derivative should be stored
     *
     * @param pid pid of the object. Required.
     * @param dsType type of the derivative to retrieve.
     * @return path where the derivative should be stored
     */
    public Path getDerivativePath(PID pid, DatastreamType dsType) {
        notNull(pid, "Must provide a pid");
        notNull(dsType, "Must specify a datastream type");

        String id = pid.getId();
        String hashedPath = idToPath(id, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        String filename = id;
        if (dsType.getExtension() != null) {
            filename += "." + dsType.getExtension();
        }

        // Construct the full path of the derivative
        return Paths.get(derivativeDir, dsType.getId(), hashedPath, filename);
    }

    /**
     * Generates a list of all derivatives present for the object identified by
     * pid.
     *
     * @param pid pid of the object. Required.
     * @return list of derivatives for pid
     */
    public List<Derivative> getDerivatives(PID pid) {
        notNull(pid, "Must provide a pid");

        return listDerivativeTypes().stream()
            .map(derivType -> getDerivative(pid, derivType))
            .filter(deriv -> deriv != null)
            .collect(Collectors.toList());
    }

    /**
     * List all DatastreamTypes which are considered derivatives.
     *
     * @return
     */
    public static List<DatastreamType> listDerivativeTypes() {
        if (derivativeTypes == null) {
            derivativeTypes = Arrays.stream(DatastreamType.values())
                    .filter(dt -> EXTERNAL.equals(dt.getStoragePolicy()))
                    .collect(Collectors.toList());
        }

        return derivativeTypes;
    }

    /**
     * Returns true if the datastream provided is a derivative type
     *
     * @param dsName
     * @return
     */
    public static boolean isDerivative(String dsName) {
        return listDerivativeTypes().contains(getByIdentifier(dsName));
    }

    /**
     * @param derivativeDir the derivativeDir to set
     */
    public void setDerivativeDir(String derivativeDir) {
        this.derivativeDir = derivativeDir;
    }

    /**
     * A derivative datastream.
     *
     * @author bbpennel
     *
     */
    public static class Derivative {
        private DatastreamType type;
        private File file;

        public Derivative(DatastreamType type, File file) {
            this.type = type;
            this.file = file;
        }

        /**
         * @return the datastream type of this derivative
         */
        public DatastreamType getType() {
            return type;
        }

        /**
         * @return the derivative file
         */
        public File getFile() {
            return file;
        }

    }
}
