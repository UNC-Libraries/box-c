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
package edu.unc.lib.dcr.migration.deposit;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.tdb.TDBFactory.createDataset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;

/**
 * Manager which provides synchronized access to a common deposit model, allowing
 * multiple transformation jobs to write to it at the same time.
 *
 * @author bbpennel
 */
public class DepositModelManager {

    private static final Logger log = LoggerFactory.getLogger(DepositModelManager.class);

    private PID depositPid;
    private Dataset dataset;
    private String tdbDir;

    /**
     * @param depositPid
     * @param tdbDir
     * @throws IOException
     */
    public DepositModelManager(PID depositPid, String tdbDir) throws IOException {
        this.depositPid = depositPid;
        this.tdbDir = tdbDir;

        Path tdbPath = Paths.get(tdbDir).toAbsolutePath();
        if (Files.notExists(tdbPath)) {
            Files.createDirectories(tdbPath);
        }

        initDataset();
    }

    /**
     * Start a write transaction for the deposit model/dataset
     */
    public synchronized Model getWriteModel() {
        String uri = depositPid.getURI();
        dataset.begin(ReadWrite.WRITE);
        if (!dataset.containsNamedModel(uri)) {
            dataset.addNamedModel(uri, createDefaultModel());
        }
        return dataset.getNamedModel(uri);
    }

    public synchronized Model getReadModel() {
        String uri = depositPid.getURI();
        dataset.begin(ReadWrite.READ);
        return dataset.getNamedModel(uri);
    }

    /**
     * Add triples from the provided model to the deposit model
     *
     * @param model
     */
    public synchronized void addTriples(Model model) {
        Model depositModel = getWriteModel();
        try {
            log.debug("Adding triples to deposit model: {}", model);
            depositModel.add(model);
            dataset.commit();
        } finally {
            dataset.end();
        }
    }

    /**
     * Commit changes to the dataset
     */
    public synchronized void commit() {
        if (dataset.isInTransaction()) {
            dataset.commit();
            dataset.end();
        }
    }

    private void initDataset() {
        log.info("Initiating deposit dataset at {}", tdbDir);
        dataset = createDataset(tdbDir);
    }
}
