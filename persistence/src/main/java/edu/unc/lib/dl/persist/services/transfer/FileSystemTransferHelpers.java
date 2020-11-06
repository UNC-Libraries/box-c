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
package edu.unc.lib.dl.persist.services.transfer;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;

import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.BinaryDetails;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferException;
import edu.unc.lib.dl.persist.services.storage.BinaryDetailsImpl;
import edu.unc.lib.dl.util.DigestAlgorithm;

/**
 * Helpers for file system transfer client operations
 *
 * @author bbpennel
 */
public class FileSystemTransferHelpers {

    private FileSystemTransferHelpers() {
    }

    /**
     * Get binary details from a filesystem storage location
     *
     * @param storageLoc storage location where binary is stored
     * @param binPid pid of the binary
     * @return details of stored binary, or null if not found
     */
    public static BinaryDetails getStoredBinaryDetails(StorageLocation storageLoc, PID binPid) {
        URI destUri = storageLoc.getStorageUri(binPid);
        return getBinaryDetails(destUri);
    }

    /**
     * Get binary details for the provided file uri
     *
     * @param binUri URI of the file
     * @return details of the binary, or null if not found
     */
    public static BinaryDetails getBinaryDetails(URI binUri) {
        Path path = Paths.get(binUri);

        try {
            if (Files.notExists(path)) {
                return null;
            }

            long size = Files.size(path);
            Date lastModified = Date.from(Files.getLastModifiedTime(path).toInstant());
            String digest = encodeHexString(DigestUtils.digest(MessageDigest.getInstance(
                    DigestAlgorithm.DEFAULT_ALGORITHM.getName()), path.toFile()));

            return new BinaryDetailsImpl(binUri, lastModified, size, digest);
        } catch (IOException e) {
            throw new BinaryTransferException("Failed to retrieve binary details for " + binUri, e);
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException(e);
        }
    }
}
