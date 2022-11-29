package edu.unc.lib.boxc.web.admin.controllers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(value = {"/", ""})
public class DashboardController {
    @GetMapping
    public ModelAndView handleRequest(Model model, HttpServletRequest request) {
        return new ModelAndView("redirect:/list");
    }
}
