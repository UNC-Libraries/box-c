package edu.unc.lib.boxc.operations.impl.edit;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import io.dropwizard.metrics5.Timer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;

/**
 * Service that manages editing of the mods:title property on an object
 *
 * @author smithjp
 *
 */
public class EditTitleService {

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private UpdateDescriptionService updateDescriptionService;
    private OperationsMessageSender operationsMessageSender;

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
    public void editTitle(AgentPrincipals agent, PID pid, String title) {
        try (Timer.Context context = timer.time()) {

            aclService.assertHasAccess(
                    "User does not have permissions to edit titles",
                    pid, agent.getPrincipals(), Permission.editDescription);

            ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);
            BinaryObject mods = obj.getDescription();

            Document newMods;

            if (mods != null) {
                try (InputStream modsStream = mods.getBinaryStream()) {
                    Document document = createSAXBuilder().build(modsStream);
                    Element rootEl = document.getRootElement();

                    if (hasExistingTitle(rootEl)) {
                        newMods = updateTitle(document, title);
                    } else {
                        newMods = addTitleToMODS(document, title);
                    }
                }
            } else {
                Document document = new Document();
                document.addContent(new Element("mods", MODS_V3_NS));
                newMods = addTitleToMODS(document, title);
            }

            updateDescriptionService.updateDescription(new UpdateDescriptionRequest(agent, pid, newMods));
        } catch (JDOMException e) {
            throw new ServiceException("Unable to build mods document for " + pid, e);
        } catch (IOException e) {
            throw new ServiceException("Unable to build new mods stream for " + pid, e);
        }

        // Send message that the action completed
        operationsMessageSender.sendUpdateDescriptionOperation(
                agent.getUsername(), Arrays.asList(pid));
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

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
     * @param mods the mods record to be edited
     * @return true if mods has title
     */
    private boolean hasExistingTitle(Element mods) {
        try {
            mods.getChild("titleInfo", MODS_V3_NS).getChild("title", MODS_V3_NS);
            return true;
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     *
     * @param doc the mods document to be edited
     * @param title the new title to be added to the mods document
     * @return updated mods document
     */
    private Document addTitleToMODS(Document doc, String title) {
        doc.getRootElement()
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS)
                                .setText(title)));

        return doc;
    }

    /**
     *
     * @param doc
     * @param title
     * @return updated mods document
     */
    private Document updateTitle(Document doc, String title) {
        Element oldTitle = doc.getRootElement().getChild("titleInfo", MODS_V3_NS).getChild("title",
                MODS_V3_NS);
        oldTitle.setText(title);

        return doc;
    }

    /**
     * @param updateDescriptionService the updateDescriptionService to set
     */
    public void setUpdateDescriptionService(UpdateDescriptionService updateDescriptionService) {
        this.updateDescriptionService = updateDescriptionService;
    }

    /**
     * @param operationsMessageSender
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }
}
