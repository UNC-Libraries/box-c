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
package edu.unc.lib.dl.cdr.services.fixity;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author mdaines
 *
 */
public class FixityLogService {

     private static final Log LOG = LogFactory.getLog(FixityLogService.class);

     protected ExecutorService executorService = null;
     private FixityLogTaskFactory fixityLogTaskFactory = null;
     private Timer pollingTimer = null;
     private long pollingIntervalSeconds = 60;

     public void init() {
          this.executorService = Executors.newSingleThreadExecutor();

          this.pollingTimer = new Timer();
          this.pollingTimer.schedule(new ExecuteTask(), 0, pollingIntervalSeconds * 1000);
     }

     public void destroy() {
          this.executorService.shutdown();
          this.pollingTimer.cancel();
     }

     class ExecuteTask extends TimerTask {
          public void run() {
               executeFixityLogTask();
          }
     }

     private void executeFixityLogTask() {
          LOG.debug("Creating and executing fixity log task");

          FixityLogTask task = this.fixityLogTaskFactory.createTask();
          this.executorService.execute(task);
     }

     public FixityLogTaskFactory getFixityLogTaskFactory() {
          return fixityLogTaskFactory;
     }

     public void setFixityLogTaskFactory(FixityLogTaskFactory fixityLogTaskFactory) {
          this.fixityLogTaskFactory = fixityLogTaskFactory;
     }

     public long getPollingIntervalSeconds() {
          return pollingIntervalSeconds;
     }

     public void setPollingIntervalSeconds(long pollingIntervalSeconds) {
          this.pollingIntervalSeconds = pollingIntervalSeconds;
     }

}
