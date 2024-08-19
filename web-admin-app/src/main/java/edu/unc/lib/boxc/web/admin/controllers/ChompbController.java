package edu.unc.lib.boxc.web.admin.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author lfarrell
 */
@Controller
public class ChompbController {
    @RequestMapping(value = "chompb", method = RequestMethod.GET)
    public String chompb() {
        return "report/chompb";
    }
}
