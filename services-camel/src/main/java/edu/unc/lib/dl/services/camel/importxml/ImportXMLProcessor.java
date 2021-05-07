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
package edu.unc.lib.dl.services.camel.importxml;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.samskivert.mustache.Template;

import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.services.edit.UpdateDescriptionService;
import edu.unc.lib.dl.persist.services.importxml.ImportXMLJob;
import edu.unc.lib.dl.persist.services.importxml.ImportXMLRequest;

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

    public void setTransferService(BinaryTransferService transferService) {
        this.transferService = transferService;
    }

    public void setLocationManager(StorageLocationManager locationManager) {
        this.locationManager = locationManager;
    }
}
