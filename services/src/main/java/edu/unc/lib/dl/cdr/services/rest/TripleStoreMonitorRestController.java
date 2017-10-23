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
package edu.unc.lib.dl.cdr.services.rest;

import java.util.Collections;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.ServletContextAware;

/**
 * @author Gregory Jansen
 *
 */
@Controller
@RequestMapping(value = { "/triplestoremonitor" })
public class TripleStoreMonitorRestController implements ServletContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(TripleStoreMonitorRestController.class);

    private ServletContext servletContext = null;

    private Map<String, String> mulgaraUpResponse = Collections.singletonMap("status", "ok");

//    @Resource
//    private TripleStoreQueryService tripleStoreQueryService;

    @PostConstruct
    public void init() {
        LOG.debug("init");
    }

    @RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
    public @ResponseBody
    Map<String, ? extends Object> getInfo() throws MulgaraDown {
//        LOG.debug("getInfo()");
//        String error = null;
//        try {
//            PID fedoraObjPID = this.getTripleStoreQueryService().verify(
//                    ContentModelHelper.Fedora_PID.FEDORA_OBJECT.getPID());
//            if (fedoraObjPID == null) {
//                error = "Cannot find " + ContentModelHelper.Fedora_PID.FEDORA_OBJECT.getPID();
//            }
//        } catch (RuntimeException e) {
//            error = e.getLocalizedMessage();
//        }
//        if (error != null) {
//            throw new MulgaraDown(error);
//        } else {
//            return this.mulgaraUpResponse;
//        }
        return null;
    }

    static class MulgaraDown extends Exception {
        private static final long serialVersionUID = 7354966480980839751L;

        MulgaraDown(String msg) {
            super(msg);
        }
    }

    @ExceptionHandler()
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR,reason = "Mulgara is down")
    public void mulgaraDown() { }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.web.context.ServletContextAware#setServletContext(javax.servlet.ServletContext)
     */
    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

}
