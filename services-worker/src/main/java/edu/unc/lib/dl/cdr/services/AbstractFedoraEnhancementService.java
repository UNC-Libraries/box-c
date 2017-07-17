/**
 * Copyright © 2008 The University of North Carolina at Chapel Hill (cdr@unc.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.processing.MessageDirector;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * 
 * @author mdaines
 *
 */
public abstract class AbstractFedoraEnhancementService implements ObjectEnhancementService, ApplicationContextAware {
     protected static final Logger LOG = LoggerFactory.getLogger(AbstractFedoraEnhancementService.class);

     protected TripleStoreQueryService tripleStoreQueryService = null;
     protected ManagementClient managementClient = null;
     protected boolean active = false;

     private ApplicationContext applicationContext;

     @Override
     public boolean prefilterMessage(EnhancementMessage message) throws EnhancementException {
          if (JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.equals(message.getQualifiedAction())) {
               return true;
          }

          if (JMSMessageUtil.ServicesActions.APPLY_SERVICE.equals(message.getQualifiedAction())
                    && this.getClass().getName().equals(message.getServiceName())) {
               return true;
          }
          return false;
     }

     @Override
     public boolean isActive() {
          return active;
     }

     public void setActive(boolean active) {
          this.active = active;
     }

     @Override
     public void setApplicationContext(ApplicationContext applicationContext) {
          this.applicationContext = applicationContext;
     }

     public MessageDirector getMessageDirector() {
          return this.applicationContext.getBean(MessageDirector.class);
     }

     public TripleStoreQueryService getTripleStoreQueryService() {
          return tripleStoreQueryService;
     }

     public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
          this.tripleStoreQueryService = tripleStoreQueryService;
     }

     public ManagementClient getManagementClient() {
          return managementClient;
     }

     public void setManagementClient(ManagementClient managementClient) {
          this.managementClient = managementClient;
     }
}
