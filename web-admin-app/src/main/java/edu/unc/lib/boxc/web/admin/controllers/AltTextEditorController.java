package edu.unc.lib.boxc.web.admin.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author lfarrell
 * Returns the alt text editor page, which is used to view/edit the machine generated
 * alt text for images in the DCR.
 */
@Controller
public class AltTextEditorController {
    @RequestMapping(value = "altTextEditor/{pid}", method = RequestMethod.GET)
    public String altTextEditor() {
        return "report/altTextEditor";
    }
}
