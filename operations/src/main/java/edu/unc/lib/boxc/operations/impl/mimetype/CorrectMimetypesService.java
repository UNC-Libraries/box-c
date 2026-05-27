package edu.unc.lib.boxc.operations.impl.mimetype;

import com.apicatalog.jsonld.StringUtils;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
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

        FedoraTransaction tx = txManager.startTransaction();

        try (Timer.Context context = timer.time()) {
            aclService.assertHasAccess(
                    "User does not have permissions to edit mimetypes",
                    PIDs.get(id), agent.getPrincipals(), Permission.editDescription);

            RepositoryObject obj = repositoryObjectLoader.getRepositoryObject(PIDs.get(id));

            // Verify object is a file object
            if (!(obj instanceof FileObject)) {
                throw new IllegalArgumentException("Cannot update mimetype for non-file object " + obj.getPid());
            }

            // Get original_file and its mimetype
            BinaryObject binaryObject = ((FileObject) obj).getOriginalFile();
            String oldMimetype = binaryObject.getMimetype();

            // Update original_file's mimetype
            updateMimetype(binaryObject, mimetype);

            premisLoggerFactory.createPremisLogger(obj)
                    .buildEvent(Premis.MetadataModification)
                    .addImplementorAgent(AgentPids.forPerson(agent))
                    .addEventDetail("Object mimetype updated from " + oldMimetype + " to " + mimetype)
                    .writeAndClose();
        } catch (AccessRestrictionException e) {
            throw new AccessRestrictionException("Permission denied for " + id);
        } catch (ObjectTypeMismatchException e) {
            throw new ObjectTypeMismatchException("Object {} is not a File Object" + id);
        } catch (RuntimeException e) {
            tx.cancel(e);
            throw e;
        } finally {
            tx.close();
        }

        // Send message that the action completed
        operationsMessageSender.sendUpdateDescriptionOperation(agent.getUsername(),
                Collections.singletonList(PIDs.get(id)));
        return PIDs.get(id);
    }

    /**
     * Update the mimetype
     * @param binaryObject file object to update
     * @param mimetype the new mimetype of the given object
     */
    private void updateMimetype(BinaryObject binaryObject, String mimetype) {
        // Update a copy of the model for this object
        Model model = ModelFactory.createDefaultModel().add(binaryObject.getModel(true));
        Resource binaryResource = model.getResource(binaryObject.getPid().getRepositoryPath());

        // Clear out existing mimetypes
        binaryResource.removeAll(CdrDeposit.mimetype);

        // Add new mimetypes
        binaryResource.addProperty(CdrDeposit.mimetype, mimetype);

        // Push the updated model back to fedora
        repositoryObjectFactory.createOrTransformObject(binaryObject.getUri(), model);
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
