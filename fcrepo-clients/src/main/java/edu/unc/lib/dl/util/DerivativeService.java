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
package edu.unc.lib.dl.util;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.idToPath;
import static edu.unc.lib.dl.model.DatastreamType.getByIdentifier;
import static edu.unc.lib.dl.model.StoragePolicy.EXTERNAL;
import static org.springframework.util.Assert.notNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamType;

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
        notNull(pid);
        notNull(dsType);

        String id = pid.getId();
        String hashedPath = idToPath(id, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);

        // Construct the full path of the derivative
        Path derivPath = Paths.get(derivativeDir, dsType.getId(), hashedPath, id + "." + dsType.getExtension());
        File derivFile = derivPath.toFile();

        // If the derivative file does not exist, then return no result
        if (!derivFile.exists()) {
            return null;
        }

        return new Derivative(dsType, derivFile);
    }

    /**
     * Generates a list of all derivatives present for the object identified by
     * pid.
     *
     * @param pid pid of the object. Required.
     * @return list of derivatives for pid
     */
    public List<Derivative> getDerivatives(PID pid) {
        notNull(pid);

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
