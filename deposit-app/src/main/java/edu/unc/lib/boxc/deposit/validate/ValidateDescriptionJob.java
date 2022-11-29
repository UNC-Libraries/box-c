package edu.unc.lib.boxc.deposit.validate;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.exceptions.MetadataValidationException;
import edu.unc.lib.boxc.operations.impl.validation.MODSValidator;

/**
 * Asserts that all descriptions in the deposit comply with repository schema
 * and vocabulary requirements.
 *
 * @author count0
 * @author bbpennel
 *
 */
public class ValidateDescriptionJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory.getLogger(ValidateDescriptionJob.class);

    private MODSValidator modsValidator;

    public ValidateDescriptionJob() {
        super();
    }

    public ValidateDescriptionJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        int invalidCount = 0;

        File descriptionDir = getDescriptionDir();
        if (!descriptionDir.exists()) {
            log.debug("MODS directory does not exist");
            return;
        }

        // List all description files at any depth
        List<File> modsFiles = null;
        try {
            modsFiles = Files.walk(descriptionDir.toPath())
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .map(Path::toFile)
                    .collect(toList());
        } catch (IOException e) {
            failJob(e, "Failed to list description files in {0}", descriptionDir);
        }

        StringBuilder errors = new StringBuilder();

        setTotalClicks(modsFiles.size());
        for (File f : modsFiles) {
            PID p = getPIDFromFile(f);

            try {
                modsValidator.validate(f);
            } catch (MetadataValidationException e) {
                errors.append("Description for object ").append(p)
                    .append(" is invalid:\n")
                    .append(e.getDetailedMessage());

                invalidCount++;
                continue;
            } catch (IOException e) {
                failJob(e, "Failed to read description for {0} at path {1}", p, f.getAbsolutePath());
            }

            addClicks(1);
        }

        if (invalidCount > 0) {
            failJob("Descriptive metadata (MODS) for " + invalidCount + " object(s) did not meet requirements.",
                    errors.toString());
        }
    }

    private PID getPIDFromFile(File file) {
        String path = file.getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1,
                path.lastIndexOf('.'));
        return PIDs.get(uuid);
    }

    /**
     * @return the modsValidator
     */
    public MODSValidator getModsValidator() {
        return modsValidator;
    }

    /**
     * @param modsValidator the modsValidator to set
     */
    public void setModsValidator(MODSValidator modsValidator) {
        this.modsValidator = modsValidator;
    }
}
