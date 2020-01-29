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
package edu.unc.lib.dcr.migration;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import edu.unc.lib.dcr.migration.premis.TransformContentPremisService;
import edu.unc.lib.dcr.migration.premis.TransformDepositPremisService;
import edu.unc.lib.dcr.migration.premis.PremisTransformationService;
import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Command for premis transformations
 *
 * @author bbpennel
 */
@Command(name = "transform_premis", aliases = {"tp"},
    description = "Transform a list of PREMIS 2 XML documents into PREMIS 3 serialized as n-triples")
public class TransformPremis implements Callable<Integer> {

    private static final String DEPOSIT_RECORD_TYPE = "deposit";
    private static final String CONTENT_TYPE = "content";

    @Parameters(index = "0",
            description = "Type of objects having their PREMIS transformed. Options: deposit or content")
    private String objectType;

    @Parameters(index = "1",
            description = "Path to file listing PREMIS documents to transform."
                    + " The file names must include the PID of the object they describe.")
    private Path premisListPath;

    @Parameters(index = "2", description = "Path where transformed logs will be stored")
    private Path outputPath;

    @Override
    public Integer call() throws Exception {
        PremisLoggerFactory premisLoggerFactory = new PremisLoggerFactory();
        RepositoryPIDMinter pidMinter = new RepositoryPIDMinter();

        PremisTransformationService transformationService;
        if (DEPOSIT_RECORD_TYPE.equals(objectType)) {
            transformationService = new TransformDepositPremisService(premisListPath, outputPath);
        } else if (CONTENT_TYPE.equals(objectType)) {
            transformationService = new TransformContentPremisService(premisListPath, outputPath);
        } else {
            throw new IllegalArgumentException("Invalid object type " + objectType);
        }
        transformationService.setPidMinter(pidMinter);
        transformationService.setPremisLoggerFactory(premisLoggerFactory);

        return transformationService.perform();
    }
}
