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

import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;

import edu.unc.lib.dl.fedora.PID;

/**
 * Manager which provides synchronized access to a common deposit model, allowing
 * multiple transformation jobs to write to it at the same time.
 *
 * @author bbpennel
 */
public class DepositModelManager {

    private PID depositPid;
    private Dataset dataset;
    private Model depositModel;
    private String tdbDir;

    /**
     * @param depositPid
     * @param tdbDir
     */
    public DepositModelManager(PID depositPid, String tdbDir) {
        this.depositPid = depositPid;
        this.tdbDir = tdbDir;
        initDataset();
    }

    /**
     * Start a write transaction for the deposit model/dataset
     */
    public synchronized void startWriteModel() {
        String uri = depositPid.getURI();
        dataset.begin(ReadWrite.WRITE);
        if (!dataset.containsNamedModel(uri)) {
            dataset.addNamedModel(uri, createDefaultModel());
        }
        depositModel = dataset.getNamedModel(uri);
    }

    /**
     * Add triples to the managed deposit model
     *
     * @param triples
     */
    public void addTriples(List<Statement> triples) {
        synchronized(depositModel) {
            depositModel.add(triples);
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
        dataset = createDataset(tdbDir);
    }
}
