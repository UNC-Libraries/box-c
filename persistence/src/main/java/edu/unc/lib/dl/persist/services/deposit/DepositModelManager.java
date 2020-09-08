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
package edu.unc.lib.dl.persist.services.deposit;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

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
import org.apache.jena.tdb.transaction.TDBTransactionException;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.update.UpdateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.exceptions.InterruptedRuntimeException;
import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fedora.PID;

/**
 * Manager which provides access to a common deposit dataset used for
 * deposit models.
 *
 * @author bbpennel
 */
public class DepositModelManager {

    private static final Logger log = LoggerFactory.getLogger(DepositModelManager.class);

    private Path tdbBasePath;

    private Dataset dataset;

    /**
     * Construct a deposit model manager
     * @param tdbBaseDir path to the tdb directory
     */
    public DepositModelManager(String tdbBaseDir) {
        this(Paths.get(tdbBaseDir));
    }

    /**
     * Construct and initialize a deposit model manager
     * @param depositsPtdbBasePathath
     */
    public DepositModelManager(Path tdbBasePath) {
        Objects.requireNonNull(tdbBasePath, "Must provide a base path for TDB dataset");
        this.tdbBasePath = tdbBasePath;
        loadDataset();
    }

    private DepositModelManager() {
        dataset = TDB2Factory.createDataset();
    }

    /**
     * Construct an in-memory deposit model manager for testing purposes
     */
    public static DepositModelManager inMemoryManager() {
        return new DepositModelManager();
    }

    public void loadDataset() {
        long start = System.currentTimeMillis();
        if (Files.notExists(tdbBasePath)) {
            try {
                Files.createDirectories(tdbBasePath);
            } catch (IOException e) {
                throw new RepositoryException("Failed to create dataset directory for deposit", e);
            }
        }
        dataset = TDB2Factory.connectDataset(tdbBasePath.toString());
        log.debug("Loaded dataset at {} in {}ms",
                tdbBasePath, (System.currentTimeMillis() - start));
    }

    /**
     * Close the managed dataset
     */
    public void close() {
        dataset.close();
    }

    /**
     * Start a write transaction for the deposit model/dataset
     *
     * @param depositPid pid of the deposit
     * @return
     */
    public Model getWriteModel(PID depositPid) {
        String depositUri = depositPid.getURI();

        long start = System.currentTimeMillis();
        try {
            dataset.begin(ReadWrite.WRITE);
            if (!dataset.containsNamedModel(depositUri)) {
                dataset.addNamedModel(depositUri, createDefaultModel());
            }
            Model model = dataset.getNamedModel(depositUri);
            log.debug("Created write model for {} in {}ms", depositUri, (System.currentTimeMillis() - start));
            return model;
        } catch (TDBTransactionException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw new InterruptedRuntimeException("Interrupted while waiting for TDB write lock for deposit "
                        + depositUri, e);
            }
            throw e;
        }
    }

    /**
     * Get a read only model for a deposit
     *
     * @param depositPid pid of the deposit
     * @return the model
     */
    public Model getReadModel(PID depositPid) {
        long start = System.currentTimeMillis();
        String depositUri = depositPid.getURI();

        try {
            dataset.begin(ReadWrite.READ);
            Model model = dataset.getNamedModel(depositUri);
            log.debug("Created write model for {} in {}ms", depositUri, (System.currentTimeMillis() - start));
            return model;
        } catch (TDBTransactionException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw new InterruptedRuntimeException("Interrupted while waiting for TDB read lock for deposit "
                        + depositUri, e);
            }
            throw e;
        }
    }

    /**
     * Removes and closes the model
     * @param depositPid
     */
    public synchronized void removeModel(PID depositPid) {
        String uri = depositPid.getURI();
        // Start a write transaction if one isn't already active
        ReadWrite txType = dataset.transactionMode();
        if (!ReadWrite.WRITE.equals(txType)) {
            // End a read transaction if active
            if (txType != null) {
                dataset.end();
            }
            dataset.begin(ReadWrite.WRITE);
        }
        dataset.removeNamedModel(uri);
        dataset.commit();
    }

    /**
     * Add triples from the provided model to the deposit model
     *
     * @param depositPid pid of the deposit
     * @param model
     */
    public synchronized void addTriples(PID depositPid, Model model) {
        addTriples(depositPid, model, null, null);
    }

    /**
     * Add triples from the provided model to the deposit model, inserting the
     * new resource as the child of the provided parent
     *
     * @param depositPid pid of the deposit
     * @param model
     * @param newPid
     * @param parentPid
     */
    public synchronized void addTriples(PID depositPid, Model model, PID newPid, PID parentPid) {
        Model depositModel = getWriteModel(depositPid);
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
     * @param depositPid pid of the deposit
     * @param query sparql update query
     */
    public synchronized void performUpdate(PID depositPid, String query) {
        Model depositModel = getWriteModel(depositPid);
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
     * @param depositPid pid of the deposit
     * @param queryString sparql query
     * @return results of the query, serialized as csv in an output stream
     * @throws IOException
     */
    public synchronized String performQuery(PID depositPid, String queryString) throws IOException {
        Model depositModel = getReadModel(depositPid);

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
     * Perform the provided actions and commits the changes to the dataset
     * @param dataset
     * @param actions Runnable which performs actions on the dataset to commit
     */
    public void commit(Runnable actions) {
        commit(actions, true);
    }

    /**
     * Perform the provided actions and commits the changes to the dataset
     * @param actions Runnable which performs actions on the dataset to commit
     * @param inTx if true, the dataset will be assumed to already be in a read transaction
     */
    public void commit(Runnable actions, boolean inTx) {
        try {
            if (inTx) {
                dataset.end();
            }
            dataset.begin(ReadWrite.WRITE);
            actions.run();
            dataset.commit();
            if (inTx) {
                dataset.begin(ReadWrite.READ);
            }
        } catch (Exception e) {
            throw new RepositoryException("Failed to commit to deposit model", e);
        }
    }

    /**
     * Commit the current transaction
     */
    public void commit() {
        if (dataset.isInTransaction()) {
            dataset.commit();
            dataset.end();
        }
    }

    /**
     * Commit or abort changes in the dataset
     * @param abort if true, the commit will be aborted
     */
    public void commitOrAbort(boolean abort) {
        if (dataset.isInTransaction()) {
            if (abort) {
                dataset.abort();
            } else {
                dataset.commit();
            }
            dataset.end();
        }
    }

    /**
     * End a transaction on the dataset
     */
    public void end() {
        dataset.end();
    }

    public void setDepositsPath(Path depositsPath) {
        this.tdbBasePath = depositsPath;
    }
}
