package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author bbpennel
 */
public class WorkFormToBagJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory.getLogger(WorkFormToBagJob.class);

    @Autowired
    private Path uploadStagingPath;
    @Autowired
    private WorkFormModsTransformer modsTransformer;
    private XMLOutputter xmlOutputter;

    public WorkFormToBagJob() {
        super(null, null);
    }

    public WorkFormToBagJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
        xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(Format.getPrettyFormat());
    }

    @Override
    public void runJob() {
        Model depModel = getReadOnlyModel();
        // Cache all the changes for committing at the end
        Model model = ModelFactory.createDefaultModel().add(depModel);

        URI sourceUri = URI.create(getDepositField(RedisWorkerConstants.DepositField.sourceUri));
        Path sourcePath = Paths.get(sourceUri);
        var formData = deserializeFormJson(sourcePath);

        interruptJobIfStopped();

        validateFormData(formData);

        // Move staged files into deposit
        gatherFiles(formData);
        // Transform form structure into RDF bag
        var workPid = populateDepositModel(model, formData);
        // Transform descriptive form data into MODS
        populateDescription(workPid, formData);

        commit(() -> depModel.add(model));
    }

    private void populateDescription(PID workPid, WorkFormData workFormData) {
        try {
            var modsDoc = modsTransformer.transform(workFormData);
            var modsPath = depositDirectoryManager.getModsPath(workPid, true);
            var modsString = xmlOutputter.outputString(modsDoc);
            Files.writeString(modsPath, modsString, StandardCharsets.UTF_8);
        } catch (TemplateException | IOException | JDOMException e) {
            throw buildFailJob(e, "Failed to transform work form data into MODS");
        }
    }

    private void gatherFiles(WorkFormData workFormData) {
        try {
            for (var file : workFormData.getFile()) {
                Path storedPath = uploadStagingPath.resolve(file.getTmp());
                if (Files.notExists(storedPath)) {
                    throw buildFailJob("File " + file.getTmp() + " does not exist in staging location", "");
                }
                Files.move(storedPath, getDataDirPathForFile(file));
            }
        } catch (IOException e) {
            throw buildFailJob(e, "Failed to move file to deposit data directory");
        }
    }

    private Path getDataDirPathForFile(WorkFormData.FileInfo fileInfo) {
        return depositDirectoryManager.getDataDir().resolve(fileInfo.getTmp());
    }

    private PID populateDepositModel(Model model, WorkFormData formData) {
        Bag depositBag = model.createBag(getDepositPID().getRepositoryPath());
        Bag workBag = null;
        // Create work object
        PID workPid = pidMinter.mintContentPid();
        workBag = model.createBag(workPid.getRepositoryPath());
        model.add(workBag, RDF.type, Cdr.Work);
        model.add(workBag, CdrDeposit.label, formData.getTitle());
        depositBag.add(workBag);

        for (var file : formData.getFile()) {
            PID filePid = pidMinter.mintContentPid();
            Resource fileResource = model.createResource(filePid.getRepositoryPath());
            fileResource.addProperty(RDF.type, Cdr.FileObject);
            fileResource.addProperty(CdrDeposit.label, file.getOriginalName());
            workBag.add(fileResource);

            // Add in the original binary resource with
            Resource originalResc = DepositModelHelpers.addDatastream(fileResource, ORIGINAL_FILE);
            model.add(originalResc, CdrDeposit.stagingLocation, getDataDirPathForFile(file).toUri().toString());
        }
        return workPid;
    }

    private WorkFormData deserializeFormJson(Path formJsonPath) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        try (var formStream = Files.newInputStream(formJsonPath)) {
            return mapper.readValue(formStream, WorkFormData.class);
        } catch (IOException e) {
            throw buildFailJob(e, "Failed to deserialize form JSON");
        }
    }

    private void validateFormData(WorkFormData formData) {
        if (StringUtils.isBlank(formData.getTitle())) {
            throw buildFailJob("Form data is missing title", "");
        }
        if (formData.getFile().isEmpty()) {
            throw buildFailJob("Form data is missing files", "");
        }
    }

    public void setUploadStagingPath(Path uploadStagingPath) {
        this.uploadStagingPath = uploadStagingPath;
    }
}
