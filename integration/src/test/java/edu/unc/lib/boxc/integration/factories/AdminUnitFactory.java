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
package edu.unc.lib.boxc.integration.factories;

import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;

import java.util.Map;

/**
 * Factory for making AdminUnit objects
 * @author snluong
 */
public class AdminUnitFactory extends ContentObjectFactory {
    public AdminUnit createAdminUnit(Map<String, String> options) throws Exception {
        var adminUnit = repositoryObjectFactory.createAdminUnit(null);
        ContentRootObject contentRoot = repositoryObjectLoader.getContentRootObject(
                RepositoryPaths.getContentRootPid());
        contentRoot.addMember(adminUnit);
        // if options has "hasThumbnail = true" then add a thumbnail
        if ("true".equals(options.get("addThumbnail"))) {
            addThumbnail(adminUnit);
        }
        prepareObject(adminUnit, options);

        return adminUnit;
    }

    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }
}
