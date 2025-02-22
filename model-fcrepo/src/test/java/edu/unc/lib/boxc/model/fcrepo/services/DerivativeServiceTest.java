package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService.Derivative;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static edu.unc.lib.boxc.model.api.DatastreamType.AUDIO_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.FULLTEXT_EXTRACTION;
import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 * @author bbpennel
 *
 */
public class DerivativeServiceTest {

    private static final String ID = "c9d57df5-f67c-4330-917e-b46c1d0bec26";
    private static final String HASHED_ID = "c9/d5/7d/f5/c9d57df5-f67c-4330-917e-b46c1d0bec26";

    @TempDir
    public File tmpDir;

    private File derivativeDir;
    private String derivativePath;

    private PID pid;

    private DerivativeService derivativeService;

    @BeforeEach
    public void init() throws Exception {
        derivativeDir = tmpDir;
        derivativePath = derivativeDir.getAbsolutePath();

        derivativeService = new DerivativeService();
        derivativeService.setDerivativeDir(derivativePath);

        pid = PIDs.get(ID);
    }

    @Test
    public void testGetDerivative() throws Exception {
        File originalDerivFile = createDerivative(pid, JP2_ACCESS_COPY);

        Derivative deriv = derivativeService.getDerivative(pid, JP2_ACCESS_COPY);

        assertEquals(originalDerivFile, deriv.getFile());
        assertEquals(JP2_ACCESS_COPY, deriv.getType());
    }

    @Test
    public void testGetDerivativeNotExist() throws Exception {
        Derivative deriv = derivativeService.getDerivative(pid, JP2_ACCESS_COPY);

        assertNull(deriv);
    }

    @Test
    public void testGetDerivatives() throws Exception {
        File originalDerivFile1 = createDerivative(pid, FULLTEXT_EXTRACTION);
        File originalDerivFil21 = createDerivative(pid, JP2_ACCESS_COPY);
        File originalDerivFile3 = createDerivative(pid, AUDIO_ACCESS_COPY);

        List<Derivative> derivs = derivativeService.getDerivatives(pid);
        assertEquals(3, derivs.size());

        Derivative textDeriv = findDerivative(derivs, FULLTEXT_EXTRACTION);
        Derivative jp2Deriv = findDerivative(derivs, JP2_ACCESS_COPY);
        Derivative audioDeriv = findDerivative(derivs, AUDIO_ACCESS_COPY);

        assertNotNull(textDeriv);
        assertNotNull(jp2Deriv);
        assertNotNull(audioDeriv);

        assertEquals(originalDerivFile1, textDeriv.getFile());
        assertEquals(originalDerivFil21, jp2Deriv.getFile());
        assertEquals(originalDerivFile3, audioDeriv.getFile());
    }

    @Test
    public void testGetDerivativesIgnoreNonDerivatives() throws Exception {
        File originalDerivFile1 = createDerivative(pid, JP2_ACCESS_COPY);
        createDerivative(pid, ORIGINAL_FILE);

        List<Derivative> derivs = derivativeService.getDerivatives(pid);
        assertEquals(1, derivs.size());

        Derivative thumbDeriv = findDerivative(derivs, JP2_ACCESS_COPY);
        assertEquals(originalDerivFile1, thumbDeriv.getFile());
    }

    private Derivative findDerivative(List<Derivative> derivs, DatastreamType dsType) {
        return derivs.stream().filter(d -> dsType.equals(d.getType()))
                .findFirst().get();
    }

    private File createDerivative(PID pid, DatastreamType dsType) throws Exception {
        Path derivPath = Paths.get(derivativePath, dsType.getId(), HASHED_ID + "." + dsType.getExtension());
        File derivFile = derivPath.toFile();
        derivFile.getParentFile().mkdirs();

        FileUtils.write(derivFile, "content", "UTF-8");

        return derivFile;
    }
}
