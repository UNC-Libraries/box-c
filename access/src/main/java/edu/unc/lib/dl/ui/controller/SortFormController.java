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
package edu.unc.lib.dl.ui.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 
 * @author bbpennel
 *
 */
@Controller
@RequestMapping("/sort")
public class SortFormController {
    private static final Logger log = LoggerFactory.getLogger(SortFormController.class);

    @RequestMapping(method = RequestMethod.GET)
    public String sortForm(@RequestParam(value = "sort") String sort,
            @RequestParam(value = "container", required = false) String container,
            @RequestParam(value = "within", required = false) String searchWithin,
            @RequestParam(value = "queryPath", required = false) String queryPath,
            Model model,
            HttpServletRequest request) {

        StringBuilder destination = new StringBuilder();

        if (queryPath == null) {
            destination.append("redirect:/search");
        } else {
            destination.append("redirect:/" + queryPath);
        }

        if (container != null && container.length() > 0) {
            destination.append('/').append(container);
        }

        try {
            destination.append("?sort=").append(URLEncoder.encode(sort, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.warn("Unable to encode sort parameter", e);
        }

        if (searchWithin != null) {
            try {
                searchWithin = URLDecoder.decode(searchWithin, "UTF-8");
                destination.append('&').append(searchWithin);
            } catch (UnsupportedEncodingException e) {
                log.warn("Unable to decode query parameters", e);
            }
        }

        return destination.toString();
    }
}
