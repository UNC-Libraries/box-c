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

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.deposit.work.AbstractDepositJob;

/**
 * Abstract deposit normalization job which processes walks file system paths to interpret them into n3 and MODS for
 * deposit
 *
 * @author lfarrell
 */
public abstract class AbstractFileServerToBagJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory
            .getLogger(AbstractFileServerToBagJob.class);

    private Map<String, Bag> pathToFolderBagCache;

    public AbstractFileServerToBagJob() {
        pathToFolderBagCache = new HashMap<>();
    }

    public AbstractFileServerToBagJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);

        pathToFolderBagCache = new HashMap<>();
    }

    @Override
    public abstract void runJob();

    protected Bag getSourceBag(Bag depositBag, Path sourceFile) {
        Model model = depositBag.getModel();
        Map<String, String> status = getDepositStatus();

        PID containerPID = createPID();
        Bag bagFolder = model.createBag(containerPID.getURI());
        // Determine the label to use for this the root directory of the deposit package
        String label = status.get(DepositField.depositSlug.name());
        if (label == null) {
            label = status.get(DepositField.fileName.name());
        }
        if (label == null) {
            label = sourceFile.getFileName().toString();
        }
        model.add(bagFolder, CdrDeposit.label, label);
        model.add(bagFolder, RDF.type, Cdr.Folder);
        depositBag.add(bagFolder);

        // Cache the source bag folder
        pathToFolderBagCache.put(sourceFile.getFileName().toString(), bagFolder);

        addTitle(containerPID, label);

        return bagFolder;
    }

    /**
     * Creates and returns a Jena Resource for the given path representing a file,
     * adding it to the hierarchy for the deposit
     *
     * @param sourceBag
     * @param filepath
     * @return the resource representing the original binary for the created file resource
     */
    protected Resource getFileResource(Bag sourceBag, Path filepath) {
        String filename = filepath.getFileName().toString();
        Bag parentBag = getParentBag(sourceBag, filepath);

        // Create work object
        PID workPid = createPID();
        Model model = parentBag.getModel();
        Bag workBag = model.createBag(workPid.getURI());
        model.add(workBag, RDF.type, Cdr.Work);
        model.add(workBag, CdrDeposit.label, filename);

        // Generate the file object and add to the work
        PID pid = createPID();
        Resource fileResource = workBag.getModel().createResource(pid.getURI());
        fileResource.addProperty(RDF.type, Cdr.FileObject);
        fileResource.addProperty(CdrDeposit.label, filename);
        workBag.add(fileResource);

        // Add in the original binary resource
        Resource originalResc = DepositModelHelpers.addDatastream(fileResource, ORIGINAL_FILE);

        workBag.addProperty(Cdr.primaryObject, fileResource);
        parentBag.add(workBag);

        return originalResc;
    }

    /**
     * Creates and returns a Jena Bag for the given filepath representing a folder, and adds
     * it to the hierarchy for the deposit
     *
     * @param sourceBag
     * @param filepath
     * @return
     */
    protected Bag getFolderBag(Bag sourceBag, Path filepath) {
        Bag parentBag = getParentBag(sourceBag, filepath);

        PID pid = createPID();

        Bag bagFolder = sourceBag.getModel().createBag(pid.getURI());
        parentBag.add(bagFolder);

        pathToFolderBagCache.put(filepath.toString(), bagFolder);
        return bagFolder;
    }

    private PID createPID() {
        UUID uuid = UUID.randomUUID();
        PID pid = PIDs.get(uuid.toString());

        return pid;
    }

    /**
     * Returns a Jena Bag object for the parent folder of the given filepath, creating the parent if it is not present.
     *
     * @param sourceBag
     * @param filepath
     * @return
     */
    protected Bag getParentBag(Bag sourceBag, Path filepath) {
        // Retrieve the bag from the cache by base filepath if available.
        String basePath = filepath.getParent().toString();
        if (pathToFolderBagCache.containsKey(basePath)) {
            return pathToFolderBagCache.get(basePath);
        }

        Model model = sourceBag.getModel();

        // find or create a folder resource for the filepath
        String[] pathSegments = basePath.split("/");

        // Nothing to do with paths that only have data
        if (pathSegments.length <= 1) {
            return sourceBag;
        }

        Bag currentNode = sourceBag;

        for (int i = 1; i < pathSegments.length; i++) {

            String segment = pathSegments[i];
            String folderPath = StringUtils.join(Arrays.copyOfRange(pathSegments, 0, i + 1), "/");

            if (pathToFolderBagCache.containsKey(folderPath)) {
                currentNode = pathToFolderBagCache.get(folderPath);
                continue;
            }

            log.debug("No cached folder bag for {}, creating new one", folderPath);
            // No existing folder was found, create one
            PID pid = createPID();

            Bag childBag = model.createBag(pid.getURI());
            currentNode.add(childBag);

            model.add(childBag, CdrDeposit.label, segment);
            model.add(childBag, RDF.type, Cdr.Folder);

            pathToFolderBagCache.put(folderPath, childBag);

            currentNode = childBag;
        }

        return currentNode;
    }

    /**
     * Set title from provided label
     *
     * @param containerPID
     * @param label
     */
    public void addTitle(PID containerPID, String label) {
        Document doc = new Document();
        Element mods = new Element("mods", JDOMNamespaceUtil.MODS_V3_NS);
        doc.addContent(mods);

        Element titleInfo = new Element("titleInfo", JDOMNamespaceUtil.MODS_V3_NS);
        Element title = new Element("title", JDOMNamespaceUtil.MODS_V3_NS);
        title.setText(label);
        titleInfo.addContent(title);
        mods.addContent(titleInfo);

        File modsFile = getModsPath(containerPID, true).toFile();
        try (FileOutputStream fos = new FileOutputStream(modsFile)) {
            new XMLOutputter(org.jdom2.output.Format.getPrettyFormat()).output(mods.getDocument(), fos);
        } catch (IOException e) {
            failJob(e, "Unable to write title metadata for bag deposit {0}", getDepositPID());
        }
    }
}