package edu.unc.lib.dl.sword.server.servlets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.swordapp.server.servlets.SwordServlet;

@SuppressWarnings("serial")
public class ContextAwareSwordServlet extends SwordServlet implements ApplicationContextAware {
	protected static ApplicationContext context;
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(ContextAwareSwordServlet.class);

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		context = arg0;
	}
}
