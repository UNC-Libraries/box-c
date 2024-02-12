package edu.unc.lib.boxc.persist.impl;

import edu.unc.lib.boxc.persist.api.DigestAlgorithm;
import edu.unc.lib.boxc.persist.api.exceptions.InvalidChecksumException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author bbpennel
 */
public class MultiDigestInputStreamWrapperTest {
    private static final String CONTENT = "Something to digest";
    private static final String MD5 = "7afbf05666feeebe7fbbf1c9071584e6";
    private static final String SHA1 = "23d51c61a578a8cb00c5eec6b29c12b7da15c8de";
    private final InputStream contentStream = new ByteArrayInputStream(CONTENT.getBytes());

    @Test
    public void checkFixity_SingleDigests_Success() throws Exception {
        var digests = Map.of(DigestAlgorithm.SHA1, SHA1);
        var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, null);

        // Read the stream to allow digest calculation
        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        // Expect no failures
        wrapper.checkFixity();
    }

    @Test
    public void checkFixity_SingleDigests_Mismatch() {
        var digests = Map.of(DigestAlgorithm.SHA1, "ohnothisisrealbad");
        var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, null);

        assertThrows(InvalidChecksumException.class, wrapper::checkFixity);
    }

    @Test
    public void checkFixity_WantAdditionalDigests_Success() throws Exception {
        var digests = new HashMap<DigestAlgorithm, String>();
        digests.put(DigestAlgorithm.SHA1, SHA1);
        var wantDigests = List.of(DigestAlgorithm.MD5);
        var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, wantDigests);

        // Read the stream to allow digest calculation
        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        // Expect no failures
        wrapper.checkFixity();

        var results = wrapper.getDigests();
        assertEquals(MD5, results.get(DigestAlgorithm.MD5));
        assertEquals(SHA1, results.get(DigestAlgorithm.SHA1));
    }
}
