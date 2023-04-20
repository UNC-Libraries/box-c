package edu.unc.lib.boxc.web.common.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.springframework.http.HttpStatus.OK;

/**
 * Returns headers with user information, such as username and admin access
 * @author lfarrell
 */
@Controller
public class UserInformationController {
    @RequestMapping(value = "/userInformation", method = RequestMethod.HEAD)
    public ResponseEntity<Object> getUserInformation() {
        return ResponseEntity.status(OK).contentLength(0).build();
    }
}
