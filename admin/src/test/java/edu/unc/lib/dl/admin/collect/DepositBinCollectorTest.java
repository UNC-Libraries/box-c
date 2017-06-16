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
package edu.unc.lib.dl.admin.collect;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import edu.unc.lib.dl.admin.collect.DepositBinCollector.ListFilesResult;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.PackagingType;

/**
 * Note: powermock prevents some test coverage tools from working
 *
 * @author bbpennel
 * @date Jun 13, 2014
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ FileUtils.class, DepositBinCollector.class })
public class DepositBinCollectorTest {

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private File depositsDirectory;
    private File binDirectory;

    @Mock
    private DepositStatusFactory depositStatusFactory;

    @Mock
    private DepositBinConfiguration config;

    private DepositBinCollector manager;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        depositsDirectory = tmpFolder.newFolder("deposits");
        binDirectory = tmpFolder.newFolder("bin");

        List<String> binPaths = Arrays.asList(binDirectory.getAbsolutePath());
        when(config.getPaths()).thenReturn(binPaths);
        when(config.getKeyLock()).thenReturn(new ReentrantLock());

        Map<String, DepositBinConfiguration> configs = new HashMap<String, DepositBinConfiguration>();
        configs.put("etd", config);

        manager = new DepositBinCollector();
        setField(manager, "depositsDirectory", depositsDirectory);
        setField(manager, "depositStatusFactory", depositStatusFactory);
        setField(manager, "configs", configs);
    }

    @Test
    public void configTest() throws Exception {

        manager.setConfigPath("src/test/resources/depositBinConfig.json");
        manager.init();

        DepositBinConfiguration etdConfig = manager.getConfiguration("etd");

        assertNotNull("Configuration was not loaded", etdConfig);

        assertEquals("Config name not assigned", "ETDs", etdConfig.getName());
        assertNotNull("File name pattern not assigned", etdConfig.getFilePattern());
        assertEquals("Packaging type was incorrect", PackagingType.PROQUEST_ETD.getUri(), etdConfig.getPackageType());

        assertNotNull("Configuration was not loaded", manager.getConfiguration("stuff"));

        assertNull("Null expected for non-existent configuration", manager.getConfiguration("absent"));
    }

    @Test
    public void listFilesInvalidKeyTest() {
        assertNull(manager.listFiles("bad"));
    }

    @Test
    public void listFilesTest() throws Exception {
        addBinFiles();

        when(config.hasFileFilters()).thenReturn(false);

        ListFilesResult files = manager.listFiles("etd");
        List<File> appFiles = files.applicable;
        Collections.sort(appFiles);

        assertNotNull("Must return a list of files", files);
        assertEquals("Incorrect number of files return", 3, appFiles.size());
        assertEquals("Expected file was not returned", binDirectory.getAbsolutePath() + "/testPackage1", appFiles.get(1)
                .getAbsolutePath());
        assertEquals("Expected file was not returned", binDirectory.getAbsolutePath() + "/test2Package", appFiles.get(0)
                .getAbsolutePath());
        assertEquals("Expected file was not returned", binDirectory.getAbsolutePath() + "/testPackage3", appFiles.get(2)
                .getAbsolutePath());
    }

    @Test
    public void listFilesExcludeFileNameTest() throws Exception {
        addBinFiles();

        when(config.getFilePattern()).thenReturn(Pattern.compile("testPackage\\d+$"));
        when(config.hasFileFilters()).thenReturn(true);

        ListFilesResult files = manager.listFiles("etd");
        List<File> appFiles = files.applicable;
        Collections.sort(appFiles);

        assertEquals("Incorrect number of files return", 2, appFiles.size());
        assertEquals("Expected file was not returned", binDirectory.getAbsolutePath() + "/testPackage1", appFiles.get(0)
                .getAbsolutePath());
        assertEquals("Expected file was not returned", binDirectory.getAbsolutePath() + "/testPackage3", appFiles.get(1)
                .getAbsolutePath());

        assertEquals("Incorrect number of files return", 1, files.nonapplicable.size());
        assertEquals("Expected nonapplicable file was not returned", binDirectory.getAbsolutePath() + "/test2Package",
                files.nonapplicable.get(0).getAbsolutePath());
    }

    @Test
    public void listFilesExcludeFileSizeTest() throws Exception {
        addBinFiles();

        when(config.getMaxBytesPerFile()).thenReturn(1024L);
        when(config.hasFileFilters()).thenReturn(true);

        ListFilesResult files = manager.listFiles("etd");
        List<File> appFiles = files.applicable;
        Collections.sort(appFiles);

        assertEquals("Incorrect number of files return", 2, appFiles.size());
        assertEquals("Expected filed was not returned", binDirectory.getAbsolutePath() + "/testPackage1", appFiles.get(1)
                .getAbsolutePath());
        assertEquals("Expected filed was not returned", binDirectory.getAbsolutePath() + "/test2Package", appFiles.get(0)
                .getAbsolutePath());
    }

    @Test
    public void listFilesBinDoesNotExistTest() throws Exception {

        binDirectory.delete();

        ListFilesResult files = manager.listFiles("etd");

        verify(config).getPaths();
        assertNull("Non-existent bin directory should return null", files);
    }

    @Test
    public void collectInvalidKeyTest() throws Exception {
        manager.collect(null, "bad", null);

        assertEquals("No deposits should have been created", 0, depositsDirectory.list().length);
    }

    @Test
    public void collectAllTest() throws Exception {
        addBinFiles();

        manager.collect(null, "etd", new HashMap<String, String>());

        File dataDir = new File(depositsDirectory.listFiles()[0], "data");

        assertEquals("Incorrect number of files moved to data dir", 3, dataDir.list().length);
        assertEquals("Moved files should no longer be in the bin", 0, binDirectory.list().length);

        verify(depositStatusFactory).save(anyString(), anyMapOf(String.class, String.class));
    }

    @Test
    public void collectFilteredTest() throws Exception {
        addBinFiles();

        when(config.getFilePattern()).thenReturn(Pattern.compile("testPackage\\d+$"));
        when(config.hasFileFilters()).thenReturn(true);

        manager.collect(null, "etd", new HashMap<String, String>());

        File dataDir = new File(depositsDirectory.listFiles()[0], "data");

        assertEquals("Incorrect number of files moved to data dir", 2, dataDir.list().length);
        assertEquals("Filtered out file should remain", 1,
                binDirectory.list().length);

        verify(depositStatusFactory).save(anyString(), anyMapOf(String.class, String.class));
    }

    @Test
    public void collectProvidedListTest() throws Exception {
        addBinFiles();

        File binFiles[] = binDirectory.listFiles();
        List<String> targetFiles = new ArrayList<String>();
        for (File binFile : binFiles) {
            if (!"testPackage3".equals(binFile.getName())) {
                targetFiles.add(binFile.getAbsolutePath());
            }
        }

        manager.collect(targetFiles, "etd", new HashMap<String, String>());

        File dataDir = new File(depositsDirectory.listFiles()[0], "data");

        assertEquals("Incorrect number of files moved to data dir", 2, dataDir.list().length);
        assertEquals("Filtered out file should remain in bin", 1,
                binDirectory.list().length);
        assertTrue("testPackage3".equals(binDirectory.list()[0]));

        verify(depositStatusFactory).save(anyString(), anyMapOf(String.class, String.class));
    }

    @Test(expected = IOException.class)
    public void collectInvalidTargetTest() throws Exception {
        addBinFiles();

        File binFiles[] = binDirectory.listFiles();
        List<String> targetFiles = new ArrayList<String>();
        for (File binFile : binFiles) {
            targetFiles.add(binFile.getAbsolutePath());
        }

        File misplaced = tmpFolder.newFile("wrongPlace");
        targetFiles.add(misplaced.getAbsolutePath());

        try {
            manager.collect(targetFiles, "etd", new HashMap<String, String>());
        } finally {

            assertEquals("Deposit directory should not have been created", 0, depositsDirectory.list().length);
            assertEquals("All files should have remained in bin", 3, binDirectory.list().length);
            assertTrue("File in non-bin path should be unaffected", misplaced.exists());

            verify(depositStatusFactory, never()).save(anyString(), anyMapOf(String.class, String.class));
        }
    }

    @Test(expected = IOException.class)
    public void collectNonExistentTargetTest() throws Exception {
        addBinFiles();

        File binFiles[] = binDirectory.listFiles();
        List<String> targetFiles = new ArrayList<String>();
        for (File binFile : binFiles) {
            targetFiles.add(binFile.getAbsolutePath());
        }
        targetFiles.add("/does/not/exist/dsfgkldmsflgkmdslfg");

        try {
            manager.collect(targetFiles, "etd", new HashMap<String, String>());
        } finally {

            assertEquals("Deposit directory should not have been created", 0, depositsDirectory.list().length);
            assertEquals("All files should have remained in bin", 3, binDirectory.list().length);

            verify(depositStatusFactory, never()).save(anyString(), anyMapOf(String.class, String.class));
        }
    }

    @Test(expected = IOException.class)
    public void collectNotApplicableTargetTest() throws Exception {
        addBinFiles();

        File binFiles[] = binDirectory.listFiles();
        List<String> targetFiles = new ArrayList<String>();
        for (File binFile : binFiles) {
            targetFiles.add(binFile.getAbsolutePath());
        }

        when(config.getFilePattern()).thenReturn(Pattern.compile("testPackage\\d+$"));
        when(config.hasFileFilters()).thenReturn(true);

        try {
            manager.collect(targetFiles, "etd", new HashMap<String, String>());
        } finally {

            assertEquals("Deposit directory should not have been created", 0, depositsDirectory.list().length);
            assertEquals("All files should have remained in bin", 3, binDirectory.list().length);

            verify(depositStatusFactory, never()).save(anyString(), anyMapOf(String.class, String.class));
        }
    }

    @Test(expected = IOException.class)
    public void collectCopyProblemTest() throws Exception {
        addBinFiles();

        mockStatic(FileUtils.class);

        when(FileUtils.contentEquals(any(File.class), any(File.class))).thenReturn(true).thenReturn(false);

        try {
            manager.collect(null, "etd", new HashMap<String, String>());
        } finally {
            verifyStatic(times(2));
            FileUtils.contentEquals(any(File.class), any(File.class));
            verifyStatic();
            FileUtils.deleteDirectory(any(File.class));
            verify(depositStatusFactory, never()).save(anyString(), anyMapOf(String.class, String.class));

            assertEquals("No files should have been removed from bin", 3, binDirectory.list().length);
        }
    }

    private void addBinFiles() throws Exception {
        File packageFile = new File(binDirectory, "testPackage1");
        populateFile(packageFile, 1000);

        packageFile = new File(binDirectory, "test2Package");
        populateFile(packageFile, 700);

        packageFile = new File(binDirectory, "testPackage3");
        populateFile(packageFile, 2000);
    }

    private void populateFile(File file, long numberBytes) throws Exception {
        int byteLength = 512;

        Random random = new Random();
        long bytesRemaining = numberBytes;

        try(OutputStream fos = new FileOutputStream(file)) {
            for (; bytesRemaining > byteLength; bytesRemaining = bytesRemaining - byteLength) {
                byte bytes[] = new byte[byteLength];
                random.nextBytes(bytes);
                fos.write(bytes);
            }

            if (bytesRemaining > 0) {
                byte bytes[] = new byte[(int) bytesRemaining];
                random.nextBytes(bytes);
                fos.write(bytes);
            }
        }
    }
}
