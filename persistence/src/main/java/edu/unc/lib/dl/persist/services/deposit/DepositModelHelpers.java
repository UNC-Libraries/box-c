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
package edu.unc.lib.dl.persist.services.deposit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamPids;
import edu.unc.lib.dl.model.DatastreamType;
import edu.unc.lib.dl.rdf.CdrDeposit;

/**
 * @author bbpennel
 */
public class DepositModelHelpers {

    private DepositModelHelpers() {
    }

    /**
     * ADds an original datastream resource to the parent
     * @param parentResc
     * @return
     */
    public static Resource addDatastream(Resource parentResc) {
        return addDatastream(parentResc, DatastreamType.ORIGINAL_FILE);
    }

    /**
     * Adds a deposit model resource for the specified datastream type on parent resource
     * @param parentResc
     * @param type
     * @return the new datastream resource
     */
    public static Resource addDatastream(Resource parentResc, DatastreamType type) {
        PID parentPid = PIDs.get(parentResc.getURI());
        PID dsPid;
        Property dsProperty = getDatastreamProperty(type);
        switch (type) {
        case ORIGINAL_FILE:
            dsPid = DatastreamPids.getOriginalFilePid(parentPid);
            break;
        case TECHNICAL_METADATA:
            dsPid = DatastreamPids.getTechnicalMetadataPid(parentPid);
            break;
        case MD_DESCRIPTIVE_HISTORY:
            PID modsPid = DatastreamPids.getMdDescriptivePid(parentPid);
            dsPid = DatastreamPids.getDatastreamHistoryPid(modsPid);
            break;
        case TECHNICAL_METADATA_HISTORY:
            PID fitsPid = DatastreamPids.getTechnicalMetadataPid(parentPid);
            dsPid = DatastreamPids.getDatastreamHistoryPid(fitsPid);
            break;
        default:
            throw new RepositoryException("Cannot add datastream of type " + type.name());
        }
        Model model = parentResc.getModel();
        Resource dsResc = model.getResource(dsPid.getRepositoryPath());
        parentResc.addProperty(dsProperty, dsResc);
        return dsResc;
    }

    public static Resource getDatastream(Resource parentResc) {
        return parentResc.getPropertyResourceValue(getDatastreamProperty(DatastreamType.ORIGINAL_FILE));
    }

    public static Resource getDatastream(Resource parentResc, DatastreamType type) {
        return parentResc.getPropertyResourceValue(getDatastreamProperty(type));
    }

    public static Property getDatastreamProperty(DatastreamType type) {
        switch (type) {
        case ORIGINAL_FILE:
            return CdrDeposit.hasDatastreamOriginal;
        case TECHNICAL_METADATA:
            return CdrDeposit.hasDatastreamFits;
        case MD_DESCRIPTIVE_HISTORY:
            return CdrDeposit.hasDatastreamDescriptiveHistory;
        case TECHNICAL_METADATA_HISTORY:
            return CdrDeposit.hasDatastreamFitsHistory;
        default:
            throw new RepositoryException("Cannot add datastream of type " + type.name());
        }
    }

    /**
     * Adds a deposit model resource for a manifest datastream on the given deposit record
     * @param depositResc
     * @param name
     * @return the manifest resource
     */
    public static Resource addManifest(Resource depositResc, String name) {
        Model model = depositResc.getModel();
        PID parentPid = PIDs.get(depositResc.getURI());
        PID dsPid = DatastreamPids.getDepositManifestPid(parentPid, name);
        Resource dsResc = model.getResource(dsPid.getRepositoryPath());
        depositResc.addProperty(CdrDeposit.hasDatastreamManifest, dsResc);
        return dsResc;
    }
}
