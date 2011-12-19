package edu.unc.lib.dl.cdr.services.sword.server.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.swordapp.server.ServiceDocumentAPI;
import org.swordapp.server.ServiceDocumentManager;
import edu.unc.lib.dl.cdr.services.sword.server.SwordConfigurationImpl;

@SuppressWarnings("serial")
public class ServiceDocumentServlet extends ContextAwareSwordServlet {
	private static Logger log = Logger.getLogger(ServiceDocumentServlet.class);
	protected ServiceDocumentManager sdm;
	protected ServiceDocumentAPI api;

	public void init() throws ServletException {
		super.init();
		((SwordConfigurationImpl) this.config).init(getServletContext());

		// load the service document implementation
		this.sdm = (ServiceDocumentManager) this.loadImplClass("service-document-impl", false);

		// load the api
		this.api = new ServiceDocumentAPI(this.sdm, this.config);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.api.get(req, resp);
	}
}