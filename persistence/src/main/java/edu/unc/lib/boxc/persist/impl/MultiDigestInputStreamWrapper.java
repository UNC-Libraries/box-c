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
package edu.unc.lib.boxc.persist.impl;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.persist.api.DigestAlgorithm;
import edu.unc.lib.dl.exceptions.InvalidChecksumException;
import edu.unc.lib.dl.exceptions.UnsupportedAlgorithmException;

/**
 * Wrapper for an InputStream that allows for the computation and evaluation
 * of multiple digests at once.
 *
 * @author bbpennel
 */
public class MultiDigestInputStreamWrapper {
    private static final Logger log = LoggerFactory.getLogger(MultiDigestInputStreamWrapper.class);

    private final InputStream sourceStream;

    private final Map<DigestAlgorithm, String> algToDigest;

    private final Map<DigestAlgorithm, DigestInputStream> algToDigestStream;

    private boolean streamRetrieved;

    private Map<DigestAlgorithm, String> computedDigests;

    /**
     * Construct a MultiDigestInputStreamWrapper
     *
     * @param sourceStream the original source input stream
     * @param digests map of provided digests for the input stream
     * @param wantDigests list of additional digest algorithms to compute for the input stream
     */
    public MultiDigestInputStreamWrapper(InputStream sourceStream, Map<DigestAlgorithm, String> digests,
            Collection<DigestAlgorithm> wantDigests) {
        this.sourceStream = sourceStream;
        algToDigest = digests;
        algToDigestStream = new HashMap<>();

        // Merge the list of wanted digest algorithms with set of provided digests
        if (wantDigests != null) {
            for (DigestAlgorithm wantDigest : wantDigests) {
                if (!algToDigest.containsKey(wantDigest)) {
                    algToDigest.put(wantDigest, null);
                }
            }
        }
    }

    /**
     * Get the InputStream wrapped to produce the requested digests
     *
     * @return wrapped input stream
     */
    public InputStream getInputStream() {
        streamRetrieved = true;
        InputStream digestStream = sourceStream;
        for (DigestAlgorithm algorithm : algToDigest.keySet()) {
            try {
                // Progressively wrap the original stream in layers of digest streams
                digestStream = new DigestInputStream(
                        digestStream, MessageDigest.getInstance(algorithm.getName()));
            } catch (final NoSuchAlgorithmException e) {
                throw new UnsupportedAlgorithmException("Unsupported digest algorithm: " + algorithm, e);
            }

            algToDigestStream.put(algorithm, (DigestInputStream) digestStream);
        }
        return digestStream;
    }

    /**
     * After consuming the inputstream, verify that all of the computed digests
     * matched the provided digests.
     *
     * Note: the wrapped InputStream will be consumed if it has not already been read.
     *
     * @throws InvalidChecksumException thrown if any of the digests did not match
     */
    public void checkFixity() throws InvalidChecksumException {
        log.debug("Preparing to verify fixity of wrapped stream");
        calculateDigests();

        log.debug("Comparing computed digest values to expected digests");
        algToDigest.forEach((algorithm, originalDigest) -> {
            // Skip any algorithms which were calculated but no digest was provided for verification
            if (originalDigest == null) {
                return;
            }
            final String computed = computedDigests.get(algorithm);

            if (!originalDigest.equalsIgnoreCase(computed)) {
                throw new InvalidChecksumException(String.format(
                        "Checksum mismatch, computed %s digest %s did not match expected value %s",
                        algorithm, computed, originalDigest));
            }
        });

    }

    /**
     * Returns the list of digests calculated for the wrapped InputStream
     *
     * Note: the wrapped InputStream will be consumed if it has not already been read.
     *
     * @return list of digests calculated from the wrapped InputStream, in URN format.
     */
    public Map<DigestAlgorithm, String> getDigests() {
        calculateDigests();

        return computedDigests;
    }

    private void calculateDigests() {
        if (computedDigests != null) {
            log.debug("Digests already calculated");
            return;
        }

        if (!streamRetrieved) {
            log.debug("Reading inputstream to compute digests");
            // Stream not previously consumed, consume it now in order to calculate digests
            try (final InputStream is = getInputStream()) {
                log.debug("Inputstream open, beginning read");
                byte[] buffer = new byte[4096];
                while (is.read(buffer) > -1) {
                }
            } catch (final IOException e) {
                throw new RepositoryException("Failed to read content stream while calculating digests", e);
            }
            log.debug("Finished consuming inputstream for digest generation");
        }

        log.debug("Processing computed digests");
        computedDigests = new HashMap<>();
        algToDigestStream.forEach((algorithm, digestStream) -> {
            final String computed = encodeHexString(digestStream.getMessageDigest().digest());
            computedDigests.put(algorithm, computed);
        });

        log.debug("Finished populating digests: {}", computedDigests);
    }
}
