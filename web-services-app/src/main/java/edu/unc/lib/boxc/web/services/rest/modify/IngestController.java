package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.deposit.api.DepositMethod;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.deposit.impl.submit.DepositSubmissionService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.api.PackagingType;
import edu.unc.lib.boxc.persist.api.exceptions.UnsupportedPackagingTypeException;
import edu.unc.lib.boxc.web.common.auth.AccessLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling ingest submission requests
 *
 * @author bbpennel
 *
 */
@Controller
public class IngestController {
    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    @Autowired
    private DepositSubmissionService depositService;

    @Autowired
    private Path uploadStagingPath;

    @PostMapping(value = "edit/ingest/{pid}")
    public @ResponseBody
    ResponseEntity<Object> ingestPackageController(@PathVariable("pid") String pid,
            @RequestParam("type") String type, @RequestParam(value = "name", required = false) String name,
            @RequestParam("file") MultipartFile ingestFile) {

        PID destPid = PIDs.get(pid);

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        PackagingType packaging = PackagingType.getPackagingType(type);

        DepositData deposit = null;
        try {
            deposit = new DepositData(ingestFile.getInputStream(),
                    ingestFile.getOriginalFilename(),
                    ingestFile.getContentType(),
                    packaging,
                    DepositMethod.CDRAPI1.getLabel(),
                    agent);
            deposit.setDepositorEmail(GroupsThreadStore.getEmail());
            deposit.setSlug(name);
        } catch (IOException e) {
            log.error("Failed to get submitted file", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        PID depositPid;
        try {
            depositPid = depositService.submitDeposit(destPid, deposit);
        } catch (AccessRestrictionException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (UnsupportedPackagingTypeException e) {
            log.debug("Cannot handle deposit with packaging {}", type);
            return new ResponseEntity<>("Unsupported deposit package type " + type, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed to submit deposit", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("action", "ingest");
        result.put("destination", pid);
        result.put("depositId", depositPid.getId());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping(value = "edit/ingest/stageFile")
    public @ResponseBody
    ResponseEntity<Object> stageFile(@RequestParam(value = "formKey", required = false) String formKey,
                                     @RequestParam(value = "path", required = false) String filePath,
                                     @RequestParam("file") MultipartFile ingestFile,
                                     @SessionAttribute("accessLevel") AccessLevel accessLevel) throws IOException {
        log.error("highest roll {} {}", accessLevel.getHighestRole(), accessLevel.getUsername());
        // Since this is not depositing to a specific destination, we check that the user staff permissions anywhere.
        if (!accessLevel.isViewAdmin()) {
            throw new AccessRestrictionException("Insufficient permissions to stage file");
        }

        Path stagedPath = Files.createTempFile(uploadStagingPath, "ingest-", ".tmp");
        log.info("Staging file {} from form {} to {}", ingestFile.getOriginalFilename(), formKey, stagedPath);
        Files.copy(ingestFile.getInputStream(), stagedPath, StandardCopyOption.REPLACE_EXISTING);

        Map<String, Object> result = new HashMap<>();
        result.put("tmp", stagedPath.getFileName().toString());
        result.put("originalName", ingestFile.getOriginalFilename());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping(value = "edit/ingest/removeStagedFile")
    public ResponseEntity<Object> removeStagedFile(@RequestBody RemoveUploadedFileRequest request,
                                                   @SessionAttribute("accessLevel") AccessLevel accessLevel)
            throws IOException{
        if (!accessLevel.isViewAdmin()) {
            throw new AccessRestrictionException("Insufficient permissions to remove staged file");
        }
        String tempFileName = request.getFile();
        // Check that the filePath is a valid path
        if (tempFileName.contains("..") || tempFileName.contains("/")) {
            log.error("Invalid staged file path {}", tempFileName);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Path stagedPath = uploadStagingPath.resolve(tempFileName);
        log.info("Removing staged file {} from form {}", stagedPath, request.getFormKey());
        Files.deleteIfExists(stagedPath);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public static class RemoveUploadedFileRequest {
        private String file;
        private String path;
        private String formKey;

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getFormKey() {
            return formKey;
        }

        public void setFormKey(String formKey) {
            this.formKey = formKey;
        }
    }
}
