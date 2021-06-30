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
package edu.unc.lib.boxc.model.api.objects;

import org.apache.jena.rdf.model.Model;

/**
 * @author bbpennel
 */
public interface FolderObject extends ContentContainerObject {

    /**
     * Creates and adds a new folder to this folder.
     *
     * @return the newly created folder object
     */
    FolderObject addFolder();

    /**
     * Creates and adds a new folder with the provided pid and properties to this
     * folder.
     *
     * @param model
     *            properties for the new folder
     * @return the newly created folder object
     */
    FolderObject addFolder(Model model);

    /**
     * Creates and adds a new work to this folder.
     *
     * @return the newly created work object
     */
    WorkObject addWork();

    /**
     * Creates and adds a new work with the provided properties to this folder.
     *
     * @param model
     *            optional additional properties for the work
     * @return the newly created work object
     */
    WorkObject addWork(Model model);

}