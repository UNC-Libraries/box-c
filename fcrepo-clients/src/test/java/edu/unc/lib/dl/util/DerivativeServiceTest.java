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

import static edu.unc.lib.dl.model.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.dl.model.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.dl.model.DatastreamType.THUMBNAIL_SMALL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamType;
import edu.unc.lib.dl.util.DerivativeService.Derivative;

/**
 *
 * @author bbpennel
 *
 */
public class DerivativeServiceTest {

    private static final String ID = "c9d57df5-f67c-4330-917e-b46c1d0bec26";
    private static final String HASHED_ID = "c9/d5/7d/f5/c9d57df5-f67c-4330-917e-b46c1d0bec26";

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private File derivativeDir;
    private String derivativePath;

    private PID pid;

    private DerivativeService derivativeService;

    @Before
    public void init() throws Exception {
        derivativeDir = tmpDir.getRoot();
        derivativePath = derivativeDir.getAbsolutePath();

        derivativeService = new DerivativeService();
        derivativeService.setDerivativeDir(derivativePath);

        pid = PIDs.get(ID);
    }

    @Test
    public void testGetDerivative() throws Exception {
        File originalDerivFile = createDerivative(pid, THUMBNAIL_SMALL);

        Derivative deriv = derivativeService.getDerivative(pid, THUMBNAIL_SMALL);

        assertEquals(originalDerivFile, deriv.getFile());
        assertEquals(THUMBNAIL_SMALL, deriv.getType());
    }

    @Test
    public void testGetDerivativeNotExist() throws Exception {
        Derivative deriv = derivativeService.getDerivative(pid, THUMBNAIL_SMALL);

        assertNull(deriv);
    }

    @Test
    public void testGetDerivatives() throws Exception {
        File originalDerivFile1 = createDerivative(pid, THUMBNAIL_SMALL);
        File originalDerivFil21 = createDerivative(pid, JP2_ACCESS_COPY);

        List<Derivative> derivs = derivativeService.getDerivatives(pid);
        assertEquals(2, derivs.size());

        Derivative thumbDeriv = findDerivative(derivs, THUMBNAIL_SMALL);
        Derivative jp2Deriv = findDerivative(derivs, JP2_ACCESS_COPY);

        assertNotNull(thumbDeriv);
        assertNotNull(jp2Deriv);

        assertEquals(originalDerivFile1, thumbDeriv.getFile());
        assertEquals(originalDerivFil21, jp2Deriv.getFile());
    }

    @Test
    public void testGetDerivativesIgnoreNonDerivatives() throws Exception {
        File originalDerivFile1 = createDerivative(pid, THUMBNAIL_SMALL);
        createDerivative(pid, ORIGINAL_FILE);

        List<Derivative> derivs = derivativeService.getDerivatives(pid);
        assertEquals(1, derivs.size());

        Derivative thumbDeriv = findDerivative(derivs, THUMBNAIL_SMALL);
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
