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
package edu.unc.lib.dl.services.camel.longleaf;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.fcrepo.client.FcrepoClient;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamPids;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.test.TestHelper;

/**
 * @author bbpennel
 * @author smithjp
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class RegisterToLongleafProcessorIT {
    private static final String TEXT1_BODY = "Some content";
    private static final String TEXT1_SHA1 = DigestUtils.sha1Hex(TEXT1_BODY);
    private static final String TEXT1_MD5 = DigestUtils.md5Hex(TEXT1_BODY);
    private static final String TEXT2_BODY = "Another content file";
    private static final String TEXT2_SHA1 = DigestUtils.sha1Hex(TEXT2_BODY);
    private static final String TEXT2_MD5 = DigestUtils.md5Hex(TEXT2_BODY);
    private static final String TEXT3_BODY = "Probably some metadata";
    private static final String TEXT3_MD5 = DigestUtils.md5Hex(TEXT3_BODY);
    private static final String TEXT4_BODY = "Maybe some more metadata";
    private static final String TEXT4_MD5 = DigestUtils.md5Hex(TEXT4_BODY);

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Autowired
    private String baseAddress;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private RepositoryPIDMinter pidMinter;
    @Autowired
    private FcrepoClient fcrepoClient;
    @Autowired
    private StorageLocationManager locManager;
    @Autowired
    private BinaryTransferService transferService;
    private BinaryTransferSession transferSession;

    private String longleafScript;
    private String outputPath;
    private String outputText;
    private List<String> output;

    private RegisterToLongleafProcessor processor;

    @Before
    public void init() throws Exception {
        TestHelper.setContentBase(baseAddress);
        tmpFolder.create();

        processor = new RegisterToLongleafProcessor();
        processor.setFcrepoClient(fcrepoClient);
        processor.setRepositoryObjectLoader(repoObjLoader);
        outputPath = tmpFolder.newFile().getPath();
        output = null;
        longleafScript = getLongleafScript(outputPath);
        processor.setLongleafBaseCommand(longleafScript);

        StorageLocation loc = locManager.getStorageLocationById("loc1");
        transferSession = transferService.getSession(loc);
    }

    @After
    public void tearDown() throws Exception {
        transferSession.close();
    }

    @Test
    public void registerSingleFileWithSha1() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);

        Exchange exchange = createBatchExchange(origBin);
        processor.process(exchange);

        readOutput();
        assertRegisterCalled(1);
        assertManifestEntry("sha1", TEXT1_SHA1, origBin.getContentUri());
    }

    @Test
    public void registerWorkWithMetadataUsingMd5() throws Exception {
        WorkObject workObj = repoObjFactory.createWorkObject(null);
        PID workPid = workObj.getPid();
        BinaryObject modsBin = createBinary(DatastreamPids.getMdDescriptivePid(workPid),
                TEXT2_BODY, null, TEXT2_MD5);
        URI modsStorageUri = modsBin.getContentUri();
        BinaryObject modsHistoryBin = createBinary(DatastreamPids.getDatastreamHistoryPid(modsBin.getPid()),
                TEXT3_BODY, null, TEXT3_MD5);
        URI modsHistoryStorageUri = modsHistoryBin.getContentUri();
        BinaryObject premisBin = createBinary(DatastreamPids.getMdEventsPid(workPid),
                TEXT4_BODY, null, TEXT4_MD5);
        URI premisStorageUri = premisBin.getContentUri();

        PID filePid = pidMinter.mintContentPid();
        PID originalPid = DatastreamPids.getOriginalFilePid(filePid);
        URI storageUri = transferSession.transfer(originalPid, streamString(TEXT1_BODY));
        FileObject fileObj = workObj.addDataFile(filePid, storageUri, "original", "text/plain", null, TEXT1_MD5, null);
        BinaryObject origBin = fileObj.getOriginalFile();

        Exchange exchange = createBatchExchange(modsBin, modsHistoryBin, premisBin, origBin);
        processor.process(exchange);

        readOutput();
        assertRegisterCalled(1);
        assertManifestEntry("md5", TEXT1_MD5, storageUri);
        assertManifestEntry("md5", TEXT2_MD5, modsStorageUri);
        assertManifestEntry("md5", TEXT3_MD5, modsHistoryStorageUri);
        assertManifestEntry("md5", TEXT4_MD5, premisStorageUri);
    }

    @Test
    public void registerFileWithFitsAndMultipleDigests() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, TEXT1_MD5);

        BinaryObject techBin = createBinary(DatastreamPids.getTechnicalMetadataPid(fileObj.getPid()),
                TEXT2_BODY, TEXT2_SHA1, TEXT2_MD5);

        Exchange exchange = createBatchExchange(origBin, techBin);
        processor.process(exchange);

        readOutput();
        assertRegisterCalled(1);
        assertManifestEntry("sha1", TEXT1_SHA1, origBin.getContentUri());
        assertManifestEntry("md5", TEXT1_MD5, origBin.getContentUri());
        assertManifestEntry("sha1", TEXT2_SHA1, techBin.getContentUri());
        assertManifestEntry("md5", TEXT2_MD5, techBin.getContentUri());
    }

    @Test
    public void registerSingleFileWithNoDigest() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, null, null);

        Exchange exchange = createBatchExchange(origBin);
        processor.process(exchange);

        readOutput();
        assertRegisterCalled(1);
        assertManifestEntry("sha1", TEXT1_SHA1, origBin.getContentUri());
    }

    private BinaryObject createBinary(PID binPid, String content, String sha1, String md5) {
        URI storageUri = transferSession.transfer(binPid, streamString(content));
        return repoObjFactory.createOrUpdateBinary(binPid, storageUri, "text.txt", "text/plain", sha1, md5, null);
    }

    private BinaryObject createOriginalBinary(FileObject fileObj, String content, String sha1, String md5) {
        PID originalPid = DatastreamPids.getOriginalFilePid(fileObj.getPid());
        URI storageUri = transferSession.transfer(originalPid, streamString(content));
        return fileObj.addOriginalFile(storageUri, "original.txt", "plain/text", sha1, md5);
    }

    private String getLongleafScript(String outputPath) throws Exception {
        String scriptContent = "#!/usr/bin/env bash"
                + "\necho $@ >> " + outputPath
                + "\necho \"$(</dev/stdin)\" >> " + outputPath;
        File longleafScript = File.createTempFile("longleaf", ".sh");

        FileUtils.write(longleafScript, scriptContent, "UTF-8");

        longleafScript.deleteOnExit();

        Set<PosixFilePermission> ownerExecutable = PosixFilePermissions.fromString("r-x------");
        Files.setPosixFilePermissions(longleafScript.toPath(), ownerExecutable);

        return longleafScript.getAbsolutePath();
    }

    private void assertRegisterCalled(int expectedCount) {
        int count = 0;
        for (String line : output) {
            if (("register --force -m @-").equals(line)) {
                count++;
            }
        }

        assertEquals(expectedCount, count);
    }

    private void assertManifestEntry(String alg, String expectedDigest, URI storageUri) {
        int algIndex = output.indexOf(alg + ":");
        assertNotEquals("Expected digest algorithm " + alg + " not found in manifest", -1, algIndex);

        String expectedPath = Paths.get(storageUri).toString();
        for (int i = algIndex + 1; i < output.size(); i++) {
            String line = output.get(i);
            if (line.matches("\\S+:")) {
                break;
            }
            if (line.matches(expectedDigest + " +" + expectedPath)) {
                return;
            }
        }
        fail("Did not find entry for " + alg + " "
                + expectedDigest + " " + expectedPath + " in manifest:\n" + outputText);
    }

    private InputStream streamString(String text) {
        return new ByteArrayInputStream(text.getBytes(UTF_8));
    }

    private void readOutput() throws IOException {
        outputText = FileUtils.readFileToString(new File(outputPath), UTF_8);
        output = Arrays.asList(outputText.split("\n"));
    }

    private Exchange createBatchExchange(RepositoryObject... objects) {
        Exchange exchange = mock(Exchange.class);
        Message msg = mock(Message.class);
        when(exchange.getIn()).thenReturn(msg);
        when(msg.getBody(List.class)).thenReturn(Arrays.stream(objects)
                .map(ro -> ro.getPid().getRepositoryPath())
                .collect(Collectors.toList()));
        return exchange;
    }
}
