package edu.unc.lib.boxc.deposit.impl.model;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb2.TDB2Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Manager which provides access to a common deposit dataset used for
 * deposit models.
 *
 * @author bbpennel
 */
public class DepositModelManager implements Closeable {

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
     * @param tdbBasePath
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
    @Override
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
        String depositUri = depositPid.getRepositoryPath();

        long start = System.currentTimeMillis();
        dataset.begin(ReadWrite.WRITE);
        if (!dataset.containsNamedModel(depositUri)) {
            dataset.addNamedModel(depositUri, createDefaultModel());
        }
        Model model = dataset.getNamedModel(depositUri);
        log.debug("Created write model for {} in {}ms", depositUri, (System.currentTimeMillis() - start));
        return model;
    }

    /**
     * Get a read only model for a deposit
     *
     * @param depositPid pid of the deposit
     * @return the model
     */
    public Model getReadModel(PID depositPid) {
        long start = System.currentTimeMillis();
        String depositUri = depositPid.getRepositoryPath();

        dataset.begin(ReadWrite.READ);
        Model model = dataset.getNamedModel(depositUri);
        log.debug("Created read model for {} in {}ms", depositUri, (System.currentTimeMillis() - start));
        return model;
    }

    /**
     * Removes and closes the model
     * @param depositPid
     */
    public synchronized void removeModel(PID depositPid) {
        String uri = depositPid.getRepositoryPath();
        // Start a write transaction if one isn't already active
        ReadWrite txType = dataset.transactionMode();
        if (!ReadWrite.WRITE.equals(txType)) {
            // End a read transaction if active
            if (txType != null) {
                dataset.end();
            }
            dataset.begin(ReadWrite.WRITE);
        }
        log.info("Removing deposit model for {}", uri);
        dataset.removeNamedModel(uri);
        dataset.commit();
        dataset.end();
    }

    /**
     * Perform the provided actions and commits the changes to the dataset
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
            if (inTx && dataset.isInTransaction()) {
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
        if (dataset.isInTransaction()) {
            dataset.end();
        }
    }
}
