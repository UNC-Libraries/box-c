package edu.unc.lib.boxc.web.common.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.util.Assert.notNull;

/**
 * Service which filters the contents of XML documents based on configured xpath expressions
 *
 * @author bbpennel
 */
public class XmlDocumentFilteringService {
    private static final Logger log = LoggerFactory.getLogger(XmlDocumentFilteringService.class);

    private String[] xPathStrings;
    private XPathFactory xPathFactory;
    private Path configPath;

    public void init() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        xPathStrings = mapper.readValue(configPath.toFile(), String[].class);
        xPathFactory = XPathFactory.instance();
    }

    /**
     * Modifies the provided document to exclude the configured xml fields
     * @param doc XML document to filter
     */
    public void filterExclusions(Document doc) {
        notNull(doc, "Must provide XML document");
        for (String xPathString: xPathStrings) {
            // Instantiating new xpath expressions since they are not thread safe, so cannot be safely reused
            XPathExpression<Element> xPath = xPathFactory.compile(xPathString, Filters.element(),
                    null, JDOMNamespaceUtil.MODS_V3_NS);
            for (Element el: xPath.evaluate(doc)) {
                log.debug("Excluding element {} from document based on expression {}", el.getName(), xPathString);
                // Remove the element itself and any parents of itself that would be left empty by the removal
                Element detachEl = el;
                Element parentEl = detachEl.getParentElement();
                // The parent is empty if it has no children other than the element being removed and no attributes
                while (parentEl != null && parentEl.getChildren().size() == 1 && parentEl.getAttributes().isEmpty()) {
                    detachEl = parentEl;
                    parentEl = detachEl.getParentElement();
                }
                detachEl.detach();

            }
        }
    }

    public void setConfigPathString(String configPath) throws URISyntaxException {
        this.configPath = Paths.get(getClass().getResource(configPath).toURI());
    }

    public void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }
}
