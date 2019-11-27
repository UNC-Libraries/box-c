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
package edu.unc.lib.dl.persist.services.edit;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.validation.MODSValidator;
import io.dropwizard.metrics5.Timer;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderSAX2Factory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;

/**
 * Service that manages editing of the mods:title property on an object
 *
 * @author smithjp
 *
 */
public class EditTitleService {

    private AccessControlService aclService;
    private MODSValidator modsValidator;
    private OperationsMessageSender operationsMessageSender;
    private RepositoryObjectLoader repoObjLoader;

    private static final Timer timer = TimerFactory.createTimerForClass(EditTitleService.class);

    public EditTitleService() {
    }

    /**
     * Changes an object's mods:title
     *
     * @param agent security principals of the agent making request
     * @param pid the pid of the object whose label is to be changed
     * @param title the new label (dc:title) of the given object
     */
    public void editTitle(AgentPrincipals agent, PID pid, String title) throws Exception {
        try (Timer.Context context = timer.time()) {

            aclService.assertHasAccess(
                    "User does not have permissions to edit titles",
                    pid, agent.getPrincipals(), Permission.editDescription);

            ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);
            BinaryObject mods = obj.getDescription();

            String username = agent.getUsername();

            String newMods;
            InputStream modsStream;
            try {
                modsStream = mods.getBinaryStream();
            } catch (NullPointerException e) {
                modsStream = null;
            }

            if (modsStream != null) {
                String modsString = IOUtils.toString(modsStream, StandardCharsets.UTF_8);
                ByteArrayInputStream modsByteArray = new ByteArrayInputStream(modsString.getBytes());
                SAXBuilder sb = new SAXBuilder(new XMLReaderSAX2Factory(false));
                Document document = sb.build(modsByteArray);
                Element rootEl = document.getRootElement();

                String oldTitle = getOldTitle(rootEl);
                if (oldTitle != "no mods:title") {
                    newMods = updateTitle(document, title);
                } else {
                    newMods = addTitleToMODS(document, title);
                }
            } else {
                Document document = new Document();
                document.addContent(new Element("mods", MODS_V3_NS));
                newMods = addTitleToMODS(document, title);
            }

            InputStream newModsStream = new ByteArrayInputStream(newMods.getBytes());
            modsValidator.validate(newModsStream);
            obj.setDescription(newModsStream);
        }

        operationsMessageSender.sendUpdateDescriptionOperation(agent.getUsername(), Arrays.asList(pid));
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    /**
     *
     * @return
     */
    public RepositoryObjectLoader getRepoObjLoader() {
        return repoObjLoader;
    }

    /**
     *
     * @param repoObjLoader
     */
    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    /**
     *
     * @param modsValidator
     */
    public void setModsValidator(MODSValidator modsValidator) {
        this.modsValidator = modsValidator;
    }

    /**
     *
     * @param operationsMessageSender
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    /**
     *
     * @param mods the mods record to be edited
     * @return current title value as a string
     */
    private String getOldTitle(Element mods) {
        String oldTitle;
        try {
            Element title = mods.getChild("titleInfo", MODS_V3_NS).getChild("title",
                 MODS_V3_NS);
            oldTitle = title.getValue();
        } catch (NullPointerException e) {
            oldTitle = "no mods:title";
        }

        return oldTitle;
    }

    /**
     *
     * @param doc the mods document to be edited
     * @param title the new title to be added to the mods document
     * @return updated mods document as a string
     */
    private String addTitleToMODS(Document doc, String title) {
        doc.getRootElement()
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS)
                                .setText(title)));

        return doc.toString();
    }

    /**
     *
     * @param doc
     * @param title
     * @return updated mods document as a string
     */
    private String updateTitle(Document doc, String title) {
        Element oldTitle = doc.getRootElement().getChild("titleInfo", MODS_V3_NS).getChild("title",
                MODS_V3_NS);
        oldTitle.setText(title);

        return doc.toString();
    }
}
