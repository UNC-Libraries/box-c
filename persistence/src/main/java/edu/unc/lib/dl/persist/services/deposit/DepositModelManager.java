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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
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
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.transaction.TDBTransactionException;
import org.apache.jena.update.UpdateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Striped;

import edu.unc.lib.dl.exceptions.InterruptedRuntimeException;
import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fedora.PID;

/**
 * Manager which provides synchronized access to a common deposit model, allowing
 * multiple transformation jobs to write to it at the same time.
 *
 * @author bbpennel
 */
public class DepositModelManager {

    private static final Logger log = LoggerFactory.getLogger(DepositModelManager.class);
    private static final int DEPOSIT_LOCK_STRIPES = 5;

    private Path tdbBasePath;
    // Locks to prevent simultaneous attempts to get the same dataset
    private Striped<Lock> depositLocker;

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
        this();
        this.tdbBasePath = tdbBasePath;
    }

    /**
     * Cleanup leftover empty dataset directories in the tdb directory
     */
    public void cleanupEmptyDatasets() {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(tdbBasePath)) {
            for (Path childPath : dirStream) {
                // delete the child if it is both a directory and is empty
                if (Files.isDirectory(childPath)) {
                    try (DirectoryStream<Path> childDirStream = Files.newDirectoryStream(childPath)) {
                        if (!childDirStream.iterator().hasNext()) {
                            Files.delete(childPath);
                        }
                    } catch (DirectoryNotEmptyException e) {
                        // Ignore attempts to delete directories that contain files
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to cleanup empty directories in {}", tdbBasePath, e);
        }
    }

    /**
     * Construct a deposit model manager
     */
    public DepositModelManager() {
        depositLocker = Striped.lazyWeakLock(DEPOSIT_LOCK_STRIPES);
    }

    private Dataset loadDataset(PID depositPid) {
        long start = System.currentTimeMillis();
        Path datasetTdbPath = getDatasetPath(depositPid);
        if (Files.notExists(datasetTdbPath)) {
            try {
                Files.createDirectories(datasetTdbPath);
            } catch (IOException e) {
                throw new RepositoryException("Failed to create dataset directory for deposit", e);
            }
        }
        Dataset dataset = TDBFactory.createDataset(datasetTdbPath.toString());
        log.debug("Loaded dataset for {} at {} in {}ms",
                depositPid.getId(), datasetTdbPath, (System.currentTimeMillis() - start));
        return dataset;
    }

    private Path getDatasetPath(PID depositPid) {
        return tdbBasePath.resolve(depositPid.getId());
    }

    /**
     * Close the model and dataset for a deposit
     * @param depositPid
     */
    public void close(PID depositPid) {
        Path datasetTdbPath = getDatasetPath(depositPid);
        // Skip further closing if the dataset does not exist
        if (Files.notExists(datasetTdbPath)) {
            return;
        }
        Dataset dataset = loadDataset(depositPid);
        dataset.close();
    }

    /**
     * Close the provided model
     * @param model must be a DatasetModelDecorator
     */
    public void close(Model model) {
        if (!(model instanceof DatasetModelDecorator)) {
            throw new IllegalArgumentException("Must provide a DatasetModelDecorator");
        }
        ((DatasetModelDecorator) model).getDataset().close();
    }

    /**
     * Start a write transaction for the deposit model/dataset
     *
     * @param depositPid pid of the deposit
     * @return
     */
    public Model getWriteModel(PID depositPid) {
        String depositUri = depositPid.getURI();

        Lock lock = depositLocker.get(depositUri);
        try {
            lock.lockInterruptibly();

            Dataset dataset = loadDataset(depositPid);
            return getWriteModel(depositUri, dataset);
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private Model getWriteModel(String depositUri, Dataset dataset) {
        long start = System.currentTimeMillis();
        try {
            dataset.begin(ReadWrite.WRITE);
            if (!dataset.containsNamedModel(depositUri)) {
                dataset.addNamedModel(depositUri, createDefaultModel());
            }
            return new DatasetModelDecorator(dataset.getNamedModel(depositUri), dataset);
        } catch (TDBTransactionException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw new InterruptedRuntimeException("Interrupted while waiting for TDB write lock for deposit "
                        + depositUri, e);
            }
            throw e;
        } finally {
            log.debug("Created write model for {} in {}ms", depositUri, (System.currentTimeMillis() - start));
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

        Lock lock = depositLocker.get(depositUri);
        try {
            lock.lockInterruptibly();

            Dataset dataset = loadDataset(depositPid);
            dataset.begin(ReadWrite.READ);
            return new DatasetModelDecorator(dataset.getNamedModel(depositUri), dataset);
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        } catch (TDBTransactionException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw new InterruptedRuntimeException("Interrupted while waiting for TDB read lock for deposit "
                        + depositUri, e);
            }
            throw e;
        } finally {
            lock.unlock();
            log.debug("Created write model for {} in {}ms", depositUri, (System.currentTimeMillis() - start));
        }
    }

    /**
     * Removes and closes the model for a deposit from the manager
     *
     * @param depositPid
     */
    public synchronized void removeModel(PID depositPid) {
        Dataset dataset = loadDataset(depositPid);
        removeModel(depositPid, dataset);
    }

    /**
     * Removes and closes the model for the given dataset
     * @param depositPid
     * @param dataset
     */
    public void removeModel(PID depositPid, Dataset dataset) {
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
        dataset.end();
        dataset.close();

        Path datasetPath = getDatasetPath(depositPid);
        try {
            FileUtils.deleteDirectory(datasetPath.toFile());
        } catch (IOException e) {
            log.debug("Unable to delete TDB directory {}", datasetPath);
        }
    }

    /**
     * Add triples from the provided model to the deposit model
     *
     * @param depositPid pid of the deposit
     * @param model
     */
    public void addTriples(PID depositPid, Model model) {
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
    public void addTriples(PID depositPid, Model model, PID newPid, PID parentPid) {
        String depositUri = depositPid.getURI();

        Lock lock = depositLocker.get(depositUri);
        try {
            lock.lockInterruptibly();

            Dataset dataset = loadDataset(depositPid);

            Model depositModel = getWriteModel(depositUri, dataset);
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
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Perform a sparql update against the deposit model
     *
     * @param depositPid pid of the deposit
     * @param query sparql update query
     */
    public void performUpdate(PID depositPid, String query) {
        String depositUri = depositPid.getURI();

        Lock lock = depositLocker.get(depositUri);
        try {
            lock.lockInterruptibly();

            Dataset dataset = loadDataset(depositPid);
            Model depositModel = getWriteModel(depositUri, dataset);
            try {
                UpdateAction.parseExecute(query, depositModel);
                dataset.commit();
            } finally {
                dataset.end();
            }
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        } finally {
            lock.unlock();
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
    public String performQuery(PID depositPid, String queryString) throws IOException {
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
     * Commit changes to the dataset
     * @param depositPid pid of the deposit
     * @param dataset dataset to commit, or null, in which case the dataset will be retrieved
     * @param endTx end the transaction if true
     */
    public void commit(PID depositPid, Dataset dataset, boolean endTx) {
        if (dataset == null) {
            commit(depositPid, endTx);
        } else {
            commit(dataset, endTx);
        }
    }

    /**
     * Commit changes to the dataset for the specified deposit
     * @param depositPid pid of the deposit
     * @param endTx end the transaction if true
     */
    public void commit(PID depositPid, boolean endTx) {
        String depositUri = depositPid.getURI();

        Lock lock = depositLocker.get(depositUri);
        try {
            lock.lockInterruptibly();

            Dataset dataset = loadDataset(depositPid);
            commit(dataset, endTx);
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Perform the provided actions and commits the changes to the dataset
     * @param dataset
     * @param actions Runnable which performs actions on the dataset to commit
     */
    public void commit(Dataset dataset, Runnable actions) {
        commit(dataset, actions, true);
    }

    /**
     * Perform the provided actions and commits the changes to the dataset
     * @param dataset
     * @param actions Runnable which performs actions on the dataset to commit
     * @param inTx if true, the dataset will be assumed to already be in a read transaction
     */
    public void commit(Dataset dataset, Runnable actions, boolean inTx) {
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

    private void commit(Dataset dataset, boolean endTx) {
        if (dataset.isInTransaction()) {
            dataset.commit();
            if (endTx) {
                dataset.end();
            }
        }
    }

    /**
     * Commit or abort changes in the dataset
     * @param depositPid pid of the deposit
     * @param dataset dataset to commit, or null, in which case the dataset will be retrieved
     * @param abort if true, the commit will be aborted
     */
    public void commitOrAbort(PID depositPid, Dataset dataset, boolean abort) {
        if (dataset == null) {
            String depositUri = depositPid.getURI();

            Lock lock = depositLocker.get(depositUri);
            try {
                lock.lockInterruptibly();

                Dataset loadedDataset = loadDataset(depositPid);
                commitOrAbort(loadedDataset, abort);
            } catch (InterruptedException e) {
                throw new InterruptedRuntimeException(e);
            } finally {
                lock.unlock();
            }
        } else {
            commitOrAbort(dataset, abort);
        }
    }

    private void commitOrAbort(Dataset dataset, boolean abort) {
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
     * End a transaction on the dataset associated with the given model
     * @param model must be of type DatasetModelDecorator
     */
    public void end(Model model) {
        if (!(model instanceof DatasetModelDecorator)) {
            throw new IllegalArgumentException("Must provide a DatasetModelDecorator");
        }
        ((DatasetModelDecorator) model).getDataset().end();
    }

    public void setDepositsPath(Path depositsPath) {
        this.tdbBasePath = depositsPath;
    }
}
