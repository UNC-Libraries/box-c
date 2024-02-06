package edu.unc.lib.boxc.persist.impl;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.persist.api.DigestAlgorithm;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
public class InputStreamDigestUtilTest {
    @Test
    public void computeDigestTest() throws Exception {
        String input = "Hello Boxc";
        String expectedSha1 = "890a5e69ae90a0766f98a9f4d82e25fa7dbb8049";
        String output = InputStreamDigestUtil.computeDigest(new ByteArrayInputStream(input.getBytes()));
        assertEquals(expectedSha1, output);
    }

    @Test
    public void computeDigestInvalidAlgorithmTest() throws Exception {
        var inputStream = mock(InputStream.class);
        when(inputStream.read(any())).thenThrow(new IOException());
        assertThrows(RepositoryException.class, () -> {
            InputStreamDigestUtil.computeDigest(inputStream, DigestAlgorithm.MD5);
        });
    }
}
