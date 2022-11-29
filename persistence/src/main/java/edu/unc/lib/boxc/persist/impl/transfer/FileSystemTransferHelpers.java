package edu.unc.lib.boxc.persist.impl.transfer;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.DigestAlgorithm;
import edu.unc.lib.boxc.persist.api.storage.BinaryDetails;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferException;
import edu.unc.lib.boxc.persist.impl.storage.BinaryDetailsImpl;

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
        URI destUri = storageLoc.getCurrentStorageUri(binPid);
        if (destUri == null) {
            return null;
        }
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

    /**
     * Get the most recent storage URI for files beginning with the provided base URI
     *
     * @param baseUri Base URI of the file, without version suffixes
     * @return Most recent storage URI, or null if there is no existing URI
     */
    public static URI getMostRecentStorageUri(URI baseUri) {
        Path basePath = Paths.get(baseUri);
        Path dirPath = basePath.getParent();
        String namePrefix = basePath.getFileName().toString();
        try (Stream<Path> stream = Files.list(dirPath)) {
            Path recentPath = stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.getFileName().toString().startsWith(namePrefix))
                    .max(Comparator.comparing(String::valueOf))
                    .get();
            return recentPath.toFile().toURI().normalize();
        } catch (NoSuchFileException | NoSuchElementException e) {
            return null;
        } catch (IOException e) {
            throw new BinaryTransferException("Failed to retrieve most recent URI for " + baseUri, e);
        }
    }

    /**
     * @param baseUri Base URI of the file, without version suffixes
     * @return Stream of all binary URIs that share the given base URI, or an empty stream.
     */
    public static List<URI> getAllStorageUris(URI baseUri) {
        Path basePath = Paths.get(baseUri);
        Path dirPath = basePath.getParent();
        String namePrefix = basePath.getFileName().toString();
        try (Stream<Path> stream = Files.list(dirPath)) {
            return stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.getFileName().toString().startsWith(namePrefix))
                    .map(path -> path.toFile().toURI().normalize())
                    .collect(Collectors.toList());
        } catch (NoSuchFileException | NoSuchElementException e) {
            return Collections.emptyList();
        } catch (IOException e) {
            throw new BinaryTransferException("Failed to retrieve binary URIs for " + baseUri, e);
        }
    }

    /**
     * @param filePath Path of the file to check for existing versions of.
     * @return True if any files exist in the parent directory of filePath which begin with
     *    the same filename prefix. This includes filePath itself, if it exists.
     */
    public static boolean versionsExist(Path filePath) {
        Path dirPath = filePath.getParent();
        if (Files.notExists(dirPath)) {
            return false;
        }
        String namePrefix = getBaseBinaryPath(filePath.getFileName());
        try (Stream<Path> stream = Files.list(dirPath)) {
            return stream.filter(path -> !Files.isDirectory(path))
                    .anyMatch(path -> path.getFileName().toString().startsWith(namePrefix));
        } catch (IOException e) {
            throw new BinaryTransferException("Failed to check for storage URI " + filePath, e);
        }
    }

    /**
     * @param filePath
     * @return the base form of the provided path, as a string
     */
    public static String getBaseBinaryPath(Path filePath) {
        return StringUtils.substringBeforeLast(filePath.toString(), ".");
    }
}
