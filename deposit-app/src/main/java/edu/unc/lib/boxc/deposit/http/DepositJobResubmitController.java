package edu.unc.lib.boxc.deposit.http;

import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.jms.DepositJobMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.UUID;

/**
 * Controller for resubmitting deposit jobs to the JMS queue.
 * Intended for recovery scenarios where jobs were skipped due to bugs.
 *
 * @author bbpennel
 */
@Controller
public class DepositJobResubmitController {
    private static final Logger log = LoggerFactory.getLogger(DepositJobResubmitController.class);

    @Autowired
    private DepositJobMessageService depositJobMessageService;

    /**
     * Resubmit deposit jobs for a list of deposit IDs using the specified job class name.
     * The job class name must be one of the valid deposit job classes.
     *
     * @param request object containing the list of deposit IDs and the job class name to submit
     * @return response indicating how many messages were submitted, or an error if the job class is invalid
     */
    @RequestMapping(value = "/admin/resubmitDepositJobs", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<ResubmitResponse> resubmitDepositJobs(
            @RequestBody ResubmitRequest request) {
        String jobClassName = request.getJobClassName();
        List<String> depositIds = request.getDepositIds();

        if (jobClassName == null || jobClassName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ResubmitResponse("jobClassName must be provided", 0));
        }
        if (!DepositJobMessageFactory.VALID_DEPOSIT_JOBS.contains(jobClassName)) {
            return ResponseEntity.badRequest()
                    .body(new ResubmitResponse("Invalid job class name: " + jobClassName, 0));
        }
        if (depositIds == null || depositIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ResubmitResponse("depositIds must be provided and non-empty", 0));
        }

        int submitted = 0;
        for (String depositId : depositIds) {
            if (depositId == null || depositId.isBlank()) {
                log.warn("Skipping blank deposit ID in resubmit request");
                continue;
            }
            var message = new DepositJobMessage();
            message.setDepositId(depositId);
            message.setJobClassName(jobClassName);
            message.setUsername("admin");
            message.setJobId(UUID.randomUUID().toString());
            log.info("Resubmitting job {} for deposit {}", jobClassName, depositId);
            depositJobMessageService.sendDepositJobMessage(message);
            submitted++;
        }

        return new ResponseEntity<>(
                new ResubmitResponse("Submitted " + submitted + " job message(s)", submitted),
                HttpStatus.OK);
    }

    public void setDepositJobMessageService(DepositJobMessageService depositJobMessageService) {
        this.depositJobMessageService = depositJobMessageService;
    }

    /**
     * Request body for resubmitting deposit jobs
     */
    public static class ResubmitRequest {
        private String jobClassName;
        private List<String> depositIds;

        public String getJobClassName() {
            return jobClassName;
        }

        public void setJobClassName(String jobClassName) {
            this.jobClassName = jobClassName;
        }

        public List<String> getDepositIds() {
            return depositIds;
        }

        public void setDepositIds(List<String> depositIds) {
            this.depositIds = depositIds;
        }
    }

    /**
     * Response body for the resubmit endpoint
     */
    public static class ResubmitResponse {
        private String message;
        private int submitted;

        public ResubmitResponse() {
        }

        public ResubmitResponse(String message, int submitted) {
            this.message = message;
            this.submitted = submitted;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getSubmitted() {
            return submitted;
        }

        public void setSubmitted(int submitted) {
            this.submitted = submitted;
        }
    }
}

