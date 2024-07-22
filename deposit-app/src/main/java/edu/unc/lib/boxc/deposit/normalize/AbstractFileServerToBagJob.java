package edu.unc.lib.boxc.deposit.normalize;

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
import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

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
        Bag bagFolder = model.createBag(containerPID.getRepositoryPath());
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
     * @param sourceBag bag representing the destination to which the contents of this deposit should be added
     * @param filepath
     * @return the resource representing the original binary for the created file resource
     */
    protected Resource getFileResource(Bag sourceBag, Path filepath) {
        String filename = filepath.getFileName().toString();
        Bag parentBag = getParentBag(sourceBag, filepath);
        Model model = parentBag.getModel();

        Bag workBag = null;
        if (!isFileOnlyMode()) {
            // Create work object
            PID workPid = createPID();
            workBag = model.createBag(workPid.getRepositoryPath());
            model.add(workBag, RDF.type, Cdr.Work);
            model.add(workBag, CdrDeposit.label, filename);
        }

        // Generate the file object and add to the work
        PID pid = createPID();
        Resource fileResource = model.createResource(pid.getRepositoryPath());
        fileResource.addProperty(RDF.type, Cdr.FileObject);
        fileResource.addProperty(CdrDeposit.label, filename);
        // Add in the original binary resource
        Resource originalResc = DepositModelHelpers.addDatastream(fileResource, ORIGINAL_FILE);

        if (!isFileOnlyMode()) {
            workBag.add(fileResource);
            parentBag.add(workBag);
        } else {
            parentBag.add(fileResource);
        }

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

        Bag bagFolder = sourceBag.getModel().createBag(pid.getRepositoryPath());
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
     * @param sourceBag bag representing the destination to which the contents of this deposit should be added
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
        if (isFileOnlyMode()) {
            failJob("Subfolders are not allowed for this deposit, encountered subfolder " + filepath, null);
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

            Bag childBag = model.createBag(pid.getRepositoryPath());
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

    private Boolean fileOnlyMode;
    /**
     * @return True if this deposit should only add FileObjects to the model. If other container types
     *      are encountered, the deposit will fail.
     */
    protected boolean isFileOnlyMode() {
        if (fileOnlyMode == null) {
            fileOnlyMode = new Boolean(getDepositField(DepositField.filesOnlyMode));
        }
        return fileOnlyMode;
    }

    /**
     * @return True if a folder should be created to represent the top level container being deposited,
     *      otherwise the contents of the deposit will be added to the destination directly.
     */
    protected boolean shouldCreateParentFolder() {
        return Boolean.parseBoolean(getDepositField(DepositField.createParentFolder)) && !isFileOnlyMode();
    }
}