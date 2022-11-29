package edu.unc.lib.boxc.services.camel.importxml;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.samskivert.mustache.Template;

import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.importxml.ImportXMLJob;
import edu.unc.lib.boxc.operations.impl.importxml.ImportXMLRequest;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;

/**
 * Processor for performing bulk xml imports
 *
 * @author bbpennel
 *
 */
public class ImportXMLProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(ImportXMLProcessor.class);

    private UpdateDescriptionService updateService;
    private JavaMailSender mailSender;
    private Template updateCompleteTemplate;
    private Template updateFailedTemplate;
    private String fromAddress;
    private String adminAddress;
    private BinaryTransferService transferService;
    private StorageLocationManager locationManager;

    private static final ObjectReader MAPPER = new ObjectMapper().readerFor(ImportXMLRequest.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        log.debug("Processing xml import request");
        final Message in = exchange.getIn();

        ImportXMLRequest request = MAPPER.readValue((String) in.getBody());
        ImportXMLJob job = createJob(request);
        job.run();
    }

    private ImportXMLJob createJob(ImportXMLRequest request) {
        ImportXMLJob job = new ImportXMLJob(request);
        job.setCompleteTemplate(updateCompleteTemplate);
        job.setFailedTemplate(updateFailedTemplate);
        job.setFromAddress(fromAddress);
        job.setAdminAddress(adminAddress);
        job.setLocationManager(locationManager);
        job.setMailSender(mailSender);
        job.setTransferService(transferService);
        job.setUpdateService(updateService);
        return job;
    }

    public void setUpdateService(UpdateDescriptionService updateService) {
        this.updateService = updateService;
    }

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void setUpdateCompleteTemplate(Template updateCompleteTemplate) {
        this.updateCompleteTemplate = updateCompleteTemplate;
    }

    public void setUpdateFailedTemplate(Template updateFailedTemplate) {
        this.updateFailedTemplate = updateFailedTemplate;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public void setAdminAddress(String adminAddress) {
        this.adminAddress = adminAddress;
    }

    public void setTransferService(BinaryTransferService transferService) {
        this.transferService = transferService;
    }

    public void setLocationManager(StorageLocationManager locationManager) {
        this.locationManager = locationManager;
    }
}
