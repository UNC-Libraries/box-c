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