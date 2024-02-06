package edu.unc.lib.boxc.persist.impl;

import edu.unc.lib.boxc.persist.api.DigestAlgorithm;
import edu.unc.lib.boxc.persist.api.exceptions.InvalidChecksumException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author bbpennel
 */
public class MultiDigestInputStreamWrapperTest {

    private static String CONTENT = "Something to digest";

    private static String MD5 = "7afbf05666feeebe7fbbf1c9071584e6";
    private static URI MD5_URI = URI.create("urn:md5:" + MD5);

    private static String SHA1 = "23d51c61a578a8cb00c5eec6b29c12b7da15c8de";
    private static URI SHA1_URI = URI.create("urn:sha1:" + SHA1);

    private static String SHA512 =
            "051c93c9cfd1b0a238c858267789fddb4fd1500c957d0ae609ec2fc2c96b3db9edddde5374be7" +
                    "3b664056ed6281a842041aa43a87fd4e0fbc1b6890676cead6d";
    private static URI SHA512_URI = URI.create("urn:sha-512:" + SHA512);

    private static String SHA512256 = "dea93ec79abc429b0065e73995a4fe0ddb7a3ec65f2b14139e75360a8ab66efc";
    private static URI SHA512256_URI = URI.create("urn:sha-512/256:" + SHA512256);

    private InputStream contentStream = new ByteArrayInputStream(CONTENT.getBytes());

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
    public void checkFixity_SingleDigests_Mismatch() throws Exception {
        var digests = Map.of(DigestAlgorithm.SHA1, "ohnothisisrealbad");
        var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, null);

        assertThrows(InvalidChecksumException.class, () -> wrapper.checkFixity());
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
