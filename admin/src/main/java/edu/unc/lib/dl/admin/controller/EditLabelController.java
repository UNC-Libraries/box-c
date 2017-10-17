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
package edu.unc.lib.dl.admin.controller;

import org.springframework.stereotype.Controller;

/**
 *
 * @author sreenug
 *
 */
@Controller
public class EditLabelController {

//    @RequestMapping(value = "editlabel/{pid}", method = RequestMethod.POST)
//    public @ResponseBody
//    Object saveLabel(@PathVariable("pid") String pid,
//            @RequestParam("label") String label) throws IngestException {
//
//        if (label != null && label.trim().length() > 0) {
//            try {
//                PremisEventLogger logger = new PremisEventLogger(pid);
//                PID pidObject = PIDs.get(pid);
//
//                this.client.modifyObject(pidObject, label, null, null, null);
//
//                Element event = logger.logEvent(PremisEventLogger.Type.MIGRATION,
//                        "Object renamed to " + label, pidObject);
//                PremisEventLogger.addDetailedOutcome(event, "success", "Object renamed successfully", null);
//                this.client.writePremisEventsToFedoraObject(logger, pidObject);
//            } catch (FedoraException e) {
//                throw new IngestException("Could not update label for " + pid, e);
//            }
//        } else {
//            Map<String, String> response = new HashMap<>();
//            response.put("message", "error");
//            return response ;
//        }
//        Map<String, String> response = new HashMap<>();
//        response.put("message", "success");
//        return response;
//    }
}
