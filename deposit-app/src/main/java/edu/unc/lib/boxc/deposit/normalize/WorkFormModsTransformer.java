package edu.unc.lib.boxc.deposit.normalize;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

/**
 * @author bbpennel
 */
public class WorkFormModsTransformer {
    private Template template;

    public WorkFormModsTransformer() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(this.getClass(), "/freemark_templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setWhitespaceStripping(true);  // Strip excessive whitespace
        template = cfg.getTemplate("work_form_mods.ftl");
    }

    /**
     * Transforms the work form XML to MODS
     * @param data
     * @return MODS XML
     */
    public Document transform(WorkFormData data) throws TemplateException, IOException, JDOMException {
        var model = new HashMap<String, Object>();
        model.put("data", data);

        StringWriter writer = new StringWriter();
        template.process(model, writer);

        SAXBuilder builder = new SAXBuilder();
        return builder.build(new StringReader(writer.toString()));
    }
}
