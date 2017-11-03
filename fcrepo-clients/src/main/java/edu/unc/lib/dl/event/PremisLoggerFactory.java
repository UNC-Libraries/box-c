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
package edu.unc.lib.dl.event;

import java.io.File;

import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectDriver;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;

/**
 * A factory class for creating PremisLogger instances
 *
 * @author harring
 */

public class PremisLoggerFactory {

    private RepositoryPIDMinter pidMinter;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repoObjFactory;
    private RepositoryObjectDriver repoObjDriver;

    public PremisLogger createPremisLogger(PID pid, File file) {
        return new FilePremisLogger(pid, file, pidMinter, repoObjLoader, repoObjFactory, repoObjDriver);
    }

    public PremisLogger createPremisLogger(RepositoryObject repoObject) {
        return new RepositoryPremisLogger(repoObject, pidMinter, repoObjLoader, repoObjFactory);
    }

    /**
     * @param pidMinter the pidMinter to set
     */
    public void setPidMinter(RepositoryPIDMinter pidMinter) {
        this.pidMinter = pidMinter;
    }

    /**
     * @param repoObjLoader the repoObjLoader to set
     */
    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    /**
     * @param repoObjFactory the repoObjFactory to set
     */
    public void setRepoObjFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    /**
     * @param repoObjDriver the repoObjDriver to set
     */
    public void setRepoObjDriver(RepositoryObjectDriver repoObjDriver) {
        this.repoObjDriver = repoObjDriver;
    }

}
