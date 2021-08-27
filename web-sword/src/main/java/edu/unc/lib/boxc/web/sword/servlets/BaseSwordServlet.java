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
package edu.unc.lib.boxc.web.sword.servlets;

import javax.annotation.Resource;

import org.swordapp.server.SwordConfiguration;

/**
 * 
 * @author bbpennel
 *
 */
public abstract class BaseSwordServlet {
    @Resource
    protected SwordConfiguration config;

    public SwordConfiguration getConfig() {
        return config;
    }

    public void setConfig(SwordConfiguration config) {
        this.config = config;
    }
}
