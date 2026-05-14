package edu.unc.lib.boxc.services.camel.pdf;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.pdf.AggregatePdfService;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequestSerializationHelper;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Processor which validates and prepares PDF objects for producing aggregate PDF
 * @author krwong
 */
public class AggregatePdfProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AggregatePdfProcessor.class);

    private AccessControlService aclService;
    private PIDMinter pidMinter;
    private RepositoryObjectLoader repositoryObjectLoader;
    private StorageLocationManager locationManager;
    private AggregatePdfService aggregatePdfService;

    public AggregatePdfProcessor() {
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        var request = deserializeRequest(exchange);
        var agent = request.getAgent();
        var workPid = PIDs.get(request.getWorkPid());

        aclService.assertHasAccess("User does not have permission to generate aggregate PDF",
                workPid, agent.getPrincipals(), Permission.runEnhancements);

        var pdfPid = pidMinter.mintContentPid();
        var originalFilePid = DatastreamPids.getOriginalFilePid(workPid);
        var pdfStorageUri = Paths.get(locationManager.getDefaultStorageLocation(workPid)
                .getNewStorageUri(originalFilePid));

        var workObject = loadWorkObject(workPid);

        try {
            Path pdfTmpPath = aggregatePdfService.generateAggregatePdf(request);
            moveFile(pdfTmpPath, pdfStorageUri);

            Model model = ModelFactory.createDefaultModel();
            model.getResource("").addProperty(RDF.type, Cdr.AggregateFile);
            workObject.addDataFile(pdfPid, pdfStorageUri.toUri(), pdfStorageUri.getFileName().toString(),
                    "application/pdf", null, null, model);
        } catch (IOException e) {
            log.error("Failed to generate aggregate PDF for {}", workPid, e);
            throw e;
        }
    }

    private PdfRequest deserializeRequest(Exchange exchange) {
        Message in = exchange.getIn();
        try {
            return PdfRequestSerializationHelper.toRequest(in.getBody(String.class));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize aggregate PDF request", e);
        }
    }

    /**
     *  Throws an IllegalArgumentException if the file is not eligible for having pdf derivatives generated from it
     * @param pid work pid
     */
    private WorkObject loadWorkObject(PID pid) {
        try {
            return repositoryObjectLoader.getWorkObject(pid);
        } catch (ObjectTypeMismatchException e) {
            throw new IllegalArgumentException("Object is not a Work Object");
        }
    }

    private void moveFile(Path pdfTmpPath, Path pdfFinalPath)
            throws IOException {
        Files.createDirectories(pdfFinalPath.getParent());

        log.debug("Moving aggregate PDF file from source {} to destination {}",
                pdfTmpPath, pdfFinalPath);

        Files.move(pdfTmpPath, pdfFinalPath, REPLACE_EXISTING);
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setPidMinter(PIDMinter pidMinter) {
        this.pidMinter = pidMinter;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setLocationManager(StorageLocationManager locationManager) {
        this.locationManager = locationManager;
    }

    public void setAggregatePdfService(AggregatePdfService aggregatePdfService) {
        this.aggregatePdfService = aggregatePdfService;
    }
}
