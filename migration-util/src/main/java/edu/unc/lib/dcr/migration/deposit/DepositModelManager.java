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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.UpdateAction;
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
        addTriples(model, null, null);
    }

    /**
     * Add triples from the provided model to the deposit model, inserting the
     * new resource as the child of the provided parent
     *
     * @param model
     * @param newPid
     * @param parentPid
     */
    public synchronized void addTriples(Model model, PID newPid, PID parentPid) {
        Model depositModel = getWriteModel();
        try {
            // Insert reference from parent to new resource
            if (newPid != null && parentPid != null) {
                Resource newResc = model.getResource(newPid.getRepositoryPath());
                Bag parentBag = depositModel.getBag(parentPid.getRepositoryPath());

                parentBag.add(newResc);
            }

            log.debug("Adding triples to deposit model: {}", model);
            depositModel.add(model);
            dataset.commit();
        } finally {
            dataset.end();
        }
    }

    /**
     * Perform a sparql update against the deposit model
     *
     * @param query sparql update query
     */
    public synchronized void performUpdate(String query) {
        Model depositModel = getWriteModel();
        try {
            UpdateAction.parseExecute(query, depositModel);
            dataset.commit();
        } finally {
            dataset.end();
        }
    }

    /**
     * Perform a sparql query against the deposit model
     *
     * @param queryString sparql query
     * @return results of the query, serialized as csv in an output stream
     * @throws IOException
     */
    public synchronized String performQuery(String queryString) throws IOException {
        Model depositModel = getReadModel();

        Query query = QueryFactory.create(queryString);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try (
                QueryExecution qexec = QueryExecutionFactory.create(query, depositModel);
                Writer writer = new PrintWriter(outStream);
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
                ) {
            ResultSet results = qexec.execSelect();
            List<String> varNames = results.getResultVars();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();

                for (String varName : varNames) {
                    printer.print(soln.get(varName));
                }
                printer.println();
            }
        }

        return outStream.toString("UTF-8");
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
