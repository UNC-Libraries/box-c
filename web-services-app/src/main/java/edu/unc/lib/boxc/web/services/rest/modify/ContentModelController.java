package edu.unc.lib.boxc.web.services.rest.modify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * @author bbpennel
 * @date Jun 1, 2015
 */
@Controller
public class ContentModelController {

    private static final Logger log = LoggerFactory.getLogger(ContentModelController.class);

    @RequestMapping(value = "edit/editType", method = RequestMethod.POST)
    public @ResponseBody Object editResourceType(@RequestBody EditResourceTypeRequest editRequest,
            HttpServletResponse response) {

        Map<String, Object> results = new HashMap<>();

        if (editRequest.newType == null || editRequest.newType.equals(ResourceType.File)) {
            results.put("error", "Invalid type " + editRequest.newType
                    + " specified as the new type.  Only container types are supported currently.");
            response.setStatus(400);
            return results;
        }

        editRequest.user = GroupsThreadStore.getUsername();
        editRequest.groupSet = GroupsThreadStore.getGroups();

        EditTypeRunnable editType = new EditTypeRunnable(editRequest);
        Thread editThread = new Thread(editType);
        editThread.start();

        results.put("message", "Operation to edit " + editRequest.pids.size() + " objects to type "
                + editRequest.newType + " has begun");

        response.setStatus(200);
        return results;
    }

    public static class EditResourceTypeRequest {
        private List<PID> pids;
        private ResourceType newTypeObject;
        private String newType;
        private String user;
        private AccessGroupSet groupSet;

        public ResourceType getNewType() {
            return newTypeObject;
        }

        public void setNewType(String newType) {
            this.newType = newType;
            this.newTypeObject = ResourceType.valueOf(newType);
        }

        public void setPids(List<String> pids) {
            this.pids = new ArrayList<>(pids.size());
            for (String id : pids) {
                this.pids.add(PIDs.get(id));
            }
        }

        public List<PID> getPids() {
            return this.pids;
        }
    }

    private class EditTypeRunnable implements Runnable {

        private final EditResourceTypeRequest editRequest;

        public EditTypeRunnable(EditResourceTypeRequest editRequest) {
            this.editRequest = editRequest;
        }

        @Override
        public void run() {
            Long start = System.currentTimeMillis();

//            try {
//                GroupsThreadStore.storeGroups(editRequest.groupSet);
//                GroupsThreadStore.storeUsername(editRequest.user);
//
//                try {
//                    dom.editResourceType(editRequest.pids, editRequest.getNewType(), editRequest.user);
//                } catch (UpdateException e) {
//                    log.warn("Failed to edit model to {}", editRequest.newType, e);
//                }
//            } finally {
//                GroupsThreadStore.clearStore();
//            }

            log.info("Finished changing content models for {} object(s) in {}ms",
                    editRequest.pids.size(), (System.currentTimeMillis() - start));
        }

    }
}
