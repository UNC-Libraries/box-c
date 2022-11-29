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
