package edu.unc.lib.dl.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/aboutRepository")
public class AboutRepositoryController {
    @RequestMapping(method = RequestMethod.GET)
    public String handleRequest() {
        return "aboutRepository";
    }
}