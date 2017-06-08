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
package edu.unc.lib.dl.cdr.services;

import static edu.unc.lib.dl.util.JMSMessageUtil.ServicesActions.APPLY_SERVICE;
import static edu.unc.lib.dl.util.JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK;

import java.util.regex.Pattern;

import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.util.JMSMessageUtil.FedoraActions;

/**
 * 
 * @author bbpennel
 *
 */
public abstract class AbstractDatastreamEnhancementService extends AbstractIrodsObjectEnhancementService {

     protected String derivativeDatastream;
     protected Pattern mimetypePattern;

     private final static String OCTET_STREAM_MIMETYPE = "application/octet-stream";

     @Override
     public boolean prefilterMessage(EnhancementMessage message) throws EnhancementException {
          String action = message.getQualifiedAction();

          if (JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.equals(action)) {
               return true;
          }
          if (JMSMessageUtil.ServicesActions.APPLY_SERVICE.equals(action)) {
               return this.getClass().getName().equals(message.getServiceName());
          }

          // If its not a Fedora message at this point, then its not going to match anything else
          if (!(message instanceof FedoraEventMessage)) {
               return false;
          }

          if (JMSMessageUtil.FedoraActions.INGEST.equals(action)) {
               return true;
          }
          if (!JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action)
                    && !JMSMessageUtil.FedoraActions.ADD_DATASTREAM.equals(action)
                    && !JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_VALUE.equals(action)) {
               return false;
          }
          String datastream = ((FedoraEventMessage) message).getDatastream();

          return ContentModelHelper.Datastream.DATA_FILE.equals(datastream);
     }

     /**
      * Applicability check which verifies that the data file is of an appropriate type and is
      * either not present or newer than the derivative to be generated
      * 
      * @param pid
      * @return
      * @throws FedoraException
      */
     protected boolean isDatastreamApplicable(PID pid) throws FedoraException {
          edu.unc.lib.dl.fedora.types.Datastream dataDoc
                    = managementClient.getDatastream(pid, Datastream.DATA_FILE.getName());

          // Don't process if there is no original data
          if (dataDoc == null) {
               return false;
          }

          // Filter out objects with non-applicable mimetypes
          if (!isMimetypeApplicable(pid, dataDoc)) {
               return false;
          }

          edu.unc.lib.dl.fedora.types.Datastream derivDoc
                    = managementClient.getDatastream(pid, derivativeDatastream);

          // No derivative present
          if (derivDoc == null) {
               return true;
          }

          // Derivative is older than the original data, need to reperform the enhancement
          // Dates are in iso8601/UTC format, so lexographic string comparison is sufficient
          return dataDoc.getCreateDate().compareTo(derivDoc.getCreateDate()) > 0;
     }

     /**
      * Applicability check which only verifies that the data file exists and is of a mimetype appropriate for this
      * service.
      *
      * @param pid
      * @return
      * @throws FedoraException
      */
     protected boolean isApplicableType(PID pid) throws FedoraException {
          edu.unc.lib.dl.fedora.types.Datastream dataDoc
                    = managementClient.getDatastream(pid, Datastream.DATA_FILE.getName());

          // Don't process if there is no original data
          if (dataDoc == null) {
               return false;
          }

          // Filter out objects with non-applicable mimetypes
          return isMimetypeApplicable(pid, dataDoc);
     }

     protected boolean isMimetypeApplicable(PID pid, edu.unc.lib.dl.fedora.types.Datastream dataDoc) {
          // Filter out objects with non-applicable mimetypes
          if (mimetypePattern != null) {
               String mimetype = dataDoc.getMIMEType();
               // If the mimetype from fedora isn't informative, retrieve source mimetype instead
               if (OCTET_STREAM_MIMETYPE.equals(mimetype)) {
                    mimetype = tripleStoreQueryService.fetchFirstBySubjectAndPredicate(pid,
                              ContentModelHelper.CDRProperty.hasSourceMimeType.toString());
               }

               if (!mimetypePattern.matcher(mimetype).matches()) {
                    return false;
               }
          }

          return true;
     }

     @Override
     public boolean isApplicable(EnhancementMessage message) throws EnhancementException {
          String action = message.getQualifiedAction();

          // Only need to check further if this is an ingest message or the DATA_FILE was changed
          if (!(FedoraActions.INGEST.equals(action)
                    || FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action)
                    || FedoraActions.ADD_DATASTREAM.equals(action)
                    || FedoraActions.MODIFY_DATASTREAM_BY_VALUE.equals(action)
                    || APPLY_SERVICE_STACK.equals(action)
                    || (APPLY_SERVICE.equals(action) && getClass().getName()
                              .equals(message.getServiceName())))) {
               return false;
          }

          try {
               // For a forced message, only check if the datastream is of an applicable type
               if (message.isForce()) {
                    return isApplicableType(message.getPid());
               }

               // For other messages, determine whether the derivative exists or is stale
               return isDatastreamApplicable(message.getPid());
          } catch (FedoraException e) {
               throw new EnhancementException("Failed to check if enhancement was applicable for "
                       + message.getPid(), e);
          }
     }
}
