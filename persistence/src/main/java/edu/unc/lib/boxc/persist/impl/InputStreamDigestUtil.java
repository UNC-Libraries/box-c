package edu.unc.lib.boxc.persist.impl;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.persist.api.DigestAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

/**
 * Utilities for using digests with InputStreams
 * @author bbpennel
 */
public class InputStreamDigestUtil {
    private InputStreamDigestUtil(){
    }

    /**
     * Calculates the digest of provided inputStream using the default digest algorithm
     * @param inputStream Contents to compute digest of. InputStream will be consumed.
     * @return String representation of the digest
     */
    public static String computeDigest(InputStream inputStream) {
        return computeDigest(inputStream, DigestAlgorithm.DEFAULT_ALGORITHM);
    }

    /**
     * Calculates the digest of provided inputStream.
     * @param inputStream Contents to compute digest of. InputStream will be consumed.
     * @param algorithm
     * @return String representation of the digest
     */
    public static String computeDigest(InputStream inputStream, DigestAlgorithm algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm.getName());
            // Consume the inputStream
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return encodeHexString(digest.digest());
        } catch (final IOException | NoSuchAlgorithmException e) {
            throw new RepositoryException("Failed to read content stream while calculating digests", e);
        }
    }
}
