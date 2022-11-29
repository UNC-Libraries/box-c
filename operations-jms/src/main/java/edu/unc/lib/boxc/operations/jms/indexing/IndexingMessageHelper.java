package edu.unc.lib.boxc.operations.jms.indexing;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Helper methods for generating indexing messages
 *
 * @author harring
 * @author bbpennel
 *
 */
public class IndexingMessageHelper {
    private static final Logger log = LoggerFactory.getLogger(IndexingMessageHelper.class);

    private IndexingMessageHelper() {
    }

    public static Document makeIndexingOperationBody(String userid, PID targetPid, Collection<PID> children,
            IndexingActionType actionType) {
        return makeIndexingOperationBody(userid, targetPid, children, actionType, null, null);
    }

    public static Document makeIndexingOperationBody(String userid, PID targetPid, Collection<PID> children,
            IndexingActionType actionType, Map<String, String> params, IndexingPriority priority) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        msg.addContent(entry);
        entry.addContent(new Element("author", ATOM_NS).addContent(new Element("name", ATOM_NS).setText(userid)));
        entry.addContent(new Element("pid", ATOM_NS).setText(targetPid.getQualifiedId()));
        if (children != null && children.size() > 0) {
            Element childEl = new Element("children", CDR_MESSAGE_NS);
            entry.addContent(childEl);
            for (PID child : children) {
                childEl.addContent(new Element("pid", CDR_MESSAGE_NS).setText(child.getRepositoryPath()));
            }
        }
        entry.addContent(new Element("actionType", ATOM_NS)
                .setText(actionType.getURI().toString()));

        if (params != null && params.size() > 0) {
            Element paramsEl = new Element("params", CDR_MESSAGE_NS);
            entry.addContent(paramsEl);
            for (Entry<String, String> param : params.entrySet()) {
                Element paramEl = new Element("param", CDR_MESSAGE_NS);
                paramEl.setAttribute("name", param.getKey());
                paramEl.setText(param.getValue());
                paramsEl.addContent(paramEl);
            }
        }

        if (priority != null) {
            entry.addContent(new Element("category", ATOM_NS).setText(priority.name()));
        }

        return msg;
    }
}
