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
package edu.unc.lib.dcr.migration.content;

import static edu.unc.lib.dcr.migration.utils.DisplayProgressUtil.displayProgress;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dcr.migration.deposit.DepositModelManager;
import edu.unc.lib.dcr.migration.paths.PathIndex;
import edu.unc.lib.dcr.migration.utils.DisplayProgressUtil;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;

/**
 * Manager for content object transformers
 *
 * @author bbpennel
 */
public class ContentObjectTransformerManager {
    private static final Logger log = LoggerFactory.getLogger(ContentObjectTransformerManager.class);

    private PathIndex pathIndex;
    private DepositModelManager modelManager;
    private boolean topLevelAsUnit;
    private RepositoryPIDMinter pidMinter;

    private BlockingQueue<ContentObjectTransformer> createdTransformers;
    private AtomicInteger totalAdded;

    public ContentObjectTransformerManager() {
        this.createdTransformers = new LinkedBlockingQueue<>();
        totalAdded = new AtomicInteger();
    }

    /**
     * Construct a new content object transformer
     *
     * @param pid
     * @param parentType
     * @return
     */
    public ContentObjectTransformer createTransformer(PID pid, Resource parentType) {
        ContentObjectTransformer transformer = new ContentObjectTransformer(pid, parentType);
        transformer.setModelManager(modelManager);
        transformer.setPathIndex(pathIndex);
        transformer.setTopLevelAsUnit(topLevelAsUnit);
        transformer.setManager(this);
        transformer.setPidMinter(pidMinter);

        createdTransformers.add(transformer);
        totalAdded.incrementAndGet();
        return transformer;
    }

    /**
     * Waits for all transformers to complete execution, logging any errors
     * which occur
     */
    public int awaitTransformers() {
        int result = 0;
        long completed = 0;
        do {
            ContentObjectTransformer transformer = createdTransformers.poll();
            try {
                displayProgress(completed, totalAdded.get());
                if (transformer == null) {
                    DisplayProgressUtil.finishProgress();
                    return result;
                }
                transformer.join();
                completed++;
            } catch (RuntimeException e) {
                log.error("Failed to transform {}", transformer.getPid(), e);
                result = 1;
            }
        } while (true);
    }

    public void setPathIndex(PathIndex pathIndex) {
        this.pathIndex = pathIndex;
    }

    public void setModelManager(DepositModelManager modelManager) {
        this.modelManager = modelManager;
    }

    public void setTopLevelAsUnit(boolean topLevelAsUnit) {
        this.topLevelAsUnit = topLevelAsUnit;
    }

    public void setPidMinter(RepositoryPIDMinter pidMinter) {
        this.pidMinter = pidMinter;
    }
}
