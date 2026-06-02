package edu.unc.lib.boxc.operations.impl.mimetype;

import com.apicatalog.jsonld.StringUtils;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import io.dropwizard.metrics5.Timer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.springframework.util.InvalidMimeTypeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.util.Assert.notNull;

/**
 * Service for correcting mimetypes that don't match the actual file type
 * @author krwong
 */
public class CorrectMimetypesService {
    private static final Logger log = LoggerFactory.getLogger(CorrectMimetypesService.class);

    private AccessControlService aclService;
    private PremisLoggerFactory premisLoggerFactory;
    private OperationsMessageSender operationsMessageSender;
    private RepositoryObjectFactory repositoryObjectFactory;
    private RepositoryObjectLoader repositoryObjectLoader;
    private TransactionManager txManager;

    private static final Timer timer = TimerFactory.createTimerForClass(CorrectMimetypesService.class);

    public static final String[] CSV_HEADERS = new String[] {"id", "mimetype"};

    public CorrectMimetypesService() {
    }

    /**
     * Receive csv of file object ids and mimetypes to update in fedora
     * @param csvInputStream inputStream for csv
     * @param agent security principals of the agent making request
     */
    public List<PID> correctMimetypes(InputStream csvInputStream, AgentPrincipals agent) {
        notNull(agent, "Must provide an agent for this operation");
        notNull(csvInputStream, "Must provide a CSV input stream for this operation");

        List<PID> pids = new ArrayList<>();

        try (var csvParser = parser(csvInputStream)) {
            for (CSVRecord csvRecord : csvParser) {
                if (!StringUtils.isBlank(csvRecord.get(0)) && !StringUtils.isBlank(csvRecord.get(1))) {
                    pids.add(processRow(csvRecord.get(0), csvRecord.get(1), agent));
                } else {
                    throw new NotFoundException("Missing file object id or mimetype");
                }
            }
        } catch (IOException e) {
            throw new ServiceException("Failed to read correct mimetype CSV", e);
        }

        return pids;
    }

    /**
     * Corrects a given file object's mimetype and creates a premis event marking the change
     * @param agent security principals of the agent making request
     * @param id the pid of the object
     * @param mimetype the new mimetype
     */
    private PID processRow(String id, String mimetype, AgentPrincipals agent) {
        if (!isValidMimetype(mimetype)) {
            throw new InvalidMimeTypeException(mimetype, "Invalid mimetype");
        }

        aclService.assertHasAccess(
                "User does not have permissions to edit mimetypes",
                PIDs.get(id), agent.getPrincipals(), Permission.editDescription);

        FileObject obj = repositoryObjectLoader.getFileObject(PIDs.get(id));

        FedoraTransaction tx = txManager.startTransaction();

        try (Timer.Context context = timer.time()) {
            // Get original_file and its mimetype
            BinaryObject binaryObject = obj.getOriginalFile();
            String oldMimetype = binaryObject.getMimetype();

            // Update original_file's mimetype
            repositoryObjectFactory.createExclusiveRelationship(binaryObject, Ebucore.hasMimeType, mimetype);

            premisLoggerFactory.createPremisLogger(obj)
                    .buildEvent(Premis.MetadataModification)
                    .addImplementorAgent(AgentPids.forPerson(agent))
                    .addEventDetail("Object mimetype updated from " + oldMimetype + " to " + mimetype)
                    .writeAndClose();
        } catch (RuntimeException e) {
            tx.cancel(e);
            throw e;
        } finally {
            tx.close();
        }

        // Send message that the action completed
        operationsMessageSender.sendAddOperation(agent.getUsername(), Collections.singletonList(obj.getParentPid()),
                Collections.singletonList(PIDs.get(id)), null, null);
        return PIDs.get(id);
    }

    private boolean isValidMimetype(String mimetype) {
        try {
            return mimetype.contains("/");
        } catch (Exception e) {
            return false;
        }
    }

    private CSVParser parser(InputStream csvInputStream) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvInputStream, StandardCharsets.UTF_8));
        return new CSVParser(reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withHeader(CSV_HEADERS)
                .withTrim());
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    /**
     * @param operationsMessageSender the operationsMessageSender to set
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    /**
     * @param premisLoggerFactory the premisLoggerFactory to set
     */
    public void setPremisLoggerFactory(PremisLoggerFactory premisLoggerFactory) {
        this.premisLoggerFactory = premisLoggerFactory;
    }

    /**
     * @param repositoryObjectLoader the repositoryObjectLoader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    /**
     * @param repositoryObjectFactory the factory to set
     */
    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    /**
     * @param txManager the transaction manager to set
     */
    public void setTransactionManager(TransactionManager txManager) {
        this.txManager = txManager;
    }
}
