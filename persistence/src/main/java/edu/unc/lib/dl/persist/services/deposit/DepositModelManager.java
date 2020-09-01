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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

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
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.transaction.TDBTransactionException;
import org.apache.jena.update.UpdateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Striped;

import edu.unc.lib.dl.exceptions.InterruptedRuntimeException;
import edu.unc.lib.dl.fedora.PID;

/**
 * Manager which provides synchronized access to a common deposit model, allowing
 * multiple transformation jobs to write to it at the same time.
 *
 * @author bbpennel
 */
public class DepositModelManager {

    private static final Logger log = LoggerFactory.getLogger(DepositModelManager.class);
    private static final String TDB_SUBDIR = "jena-tdb-dataset";
    private static final int DEPOSIT_LOCK_STRIPES = 5;
    // expire cache entries after 15 minutes
    private static final long DATASET_CACHE_TTL = 1000 * 60 * 15;

    private Path depositsPath;
    // Locks to prevent simultaneous attempts to get the same dataset
    private Striped<Lock> depositLocker;
    private LoadingCache<PID, Dataset> datasetCache;

    /**
     * Construct a deposit model manager
     * @param depositsDir path to the deposits directory
     */
    public DepositModelManager(String depositsDir) {
        this(Paths.get(depositsDir));
    }

    /**
     * Construct and initialize a deposit model manager
     * @param depositsPath
     */
    public DepositModelManager(Path depositsPath) {
        this.depositsPath = depositsPath;
        init();
    }

    /**
     * Construct a deposit model manager
     */
    public DepositModelManager() {
    }

    /**
     * Initialize the manager
     */
    public void init() {
        depositLocker = Striped.lazyWeakLock(DEPOSIT_LOCK_STRIPES);
        datasetCache = CacheBuilder.newBuilder()
                .expireAfterAccess(DATASET_CACHE_TTL, TimeUnit.MILLISECONDS)
                .build(new DatasetCacheLoader());
    }

    private class DatasetCacheLoader extends CacheLoader<PID, Dataset> {
        @Override
        public Dataset load(PID key) throws Exception {
            long start = System.currentTimeMillis();
            Path datasetTdbPath = depositsPath.resolve(key.getId()).resolve(TDB_SUBDIR);
            if (Files.notExists(datasetTdbPath)) {
                Files.createDirectories(datasetTdbPath);
            }
            log.info("Initiating deposit dataset at {}", datasetTdbPath);
            Dataset dataset = TDBFactory.createDataset(datasetTdbPath.toString());
            log.warn("Loaded dataset for {} in {}ms", key.getId(), (System.currentTimeMillis() - start));
            return dataset;
        }
    }

    public void closeModel(PID depositPid) {
        Dataset dataset = datasetCache.getIfPresent(depositPid);
        if (dataset != null) {
            dataset.close();
        }
        datasetCache.invalidate(depositPid);
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

            Dataset dataset = datasetCache.getUnchecked(depositPid);
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
            log.warn("Created write model for {} in {}ms", depositUri, (System.currentTimeMillis() - start));
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

            Dataset dataset = datasetCache.getUnchecked(depositPid);
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
            log.warn("Created write model for {} in {}ms", depositUri, (System.currentTimeMillis() - start));
        }
    }

    /**
     * Removes the model for a deposit from the manager
     *
     * @param depositPid
     */
    public synchronized void removeModel(PID depositPid) {
        String uri = depositPid.getURI();
        Dataset dataset = datasetCache.getUnchecked(depositPid);
        if (!dataset.isInTransaction()) {
            dataset.begin(ReadWrite.WRITE);
        }
        dataset.removeNamedModel(uri);
        dataset.commit();
        dataset.end();
        datasetCache.invalidate(depositPid);
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

            Dataset dataset = datasetCache.getUnchecked(depositPid);

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

            Dataset dataset = datasetCache.getUnchecked(depositPid);
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
     * Commit changes to the dataset for the specified deposit,
     * ending the transaction.
     * @param depositPid
     */
    public void commit(PID depositPid) {
        commit(depositPid, true);
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

            Dataset dataset = datasetCache.getUnchecked(depositPid);
            commit(dataset, endTx);
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        } finally {
            lock.unlock();
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

                Dataset cachedDataset = datasetCache.getUnchecked(depositPid);
                commitOrAbort(cachedDataset, abort);
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
     * End a transaction for a deposit model
     * @param depositPid pid of the deposit
     */
    public void end(PID depositPid) {
        String depositUri = depositPid.getURI();

        Lock lock = depositLocker.get(depositUri);
        try {
            lock.lockInterruptibly();

            Dataset dataset = datasetCache.getUnchecked(depositPid);
            dataset.end();
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    public void setDepositsPath(Path depositsPath) {
        this.depositsPath = depositsPath;
    }
}
