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
package edu.unc.lib.deposit.normalize;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * Normalizes a simple directory submission into n3 for deposit
 *
 * @author lfarrell
 */
public class DirectoryToBagJob extends AbstractFileServerToBagJob {
    private static final Logger log = LoggerFactory.getLogger(DirectoryToBagJob.class);

    public DirectoryToBagJob() {
        super();
    }

    public DirectoryToBagJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        Model model = getWritableModel();
        Bag depositBag = model.createBag(getDepositPID().getURI().toString());

        Map<String, String> status = getDepositStatus();
        String sourcePath = status.get(DepositField.sourcePath.name());
        File sourceFile = new File(sourcePath);

        // List all files and directories in the deposit, excluding the base directory
        Collection<File> fileListings =
                FileUtils.listFilesAndDirs(sourceFile, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
        Iterator<File> filesIt = fileListings.iterator();
        while (filesIt.hasNext()) {
            File file = filesIt.next();
            if (file.equals(sourceFile)) {
                filesIt.remove();
                break;
            }
        }

        // Turn the base directory itself into the top level folder for this deposit
        Bag sourceBag = getSourceBag(depositBag, sourceFile);

        int i = 0;
        // Add all of the payload objects into the bag folder
        for (File file : fileListings) {
            log.debug("Adding object {}: {}", i++, file.getName());

            Boolean isDir = file.isDirectory();

            Path filePath = sourceFile.toPath().getParent().relativize(file.toPath());
            String filePathString = filePath.toString();
            String filename = filePath.getFileName().toString();

            if (!isDir) {
                Resource fileResource = getFileResource(sourceBag, filePathString);

                // Find staged path for the file
                Path storedPath = Paths.get(file.getAbsolutePath());
                model.add(fileResource, CdrDeposit.stagingLocation, storedPath.toUri().toString());
            } else {
                Bag folderBag = getFolderBag(sourceBag, filePathString);
                model.add(folderBag, CdrDeposit.label, filename);
                model.add(folderBag, RDF.type, Cdr.Folder);
            }
        }
    }
}