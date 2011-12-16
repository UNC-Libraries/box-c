package edu.unc.lib.dl.cdr.services.sword.server.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.swordapp.server.CollectionAPI;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.CollectionListManager;

import edu.unc.lib.dl.sword.server.SwordConfigurationImpl;

@SuppressWarnings("serial")
public class CollectionServlet extends ContextAwareSwordServlet {
	private static Logger log = Logger.getLogger(CollectionServlet.class);

	protected CollectionListManager clm = null;
	protected CollectionDepositManager cdm;
	protected CollectionAPI api;

	public void init() throws ServletException {
		super.init();
		((SwordConfigurationImpl) this.config).init(getServletContext());

		// load the collection list manager implementation
		Object possibleClm = this.loadImplClass("collection-list-impl", true); // allow null
		this.clm = possibleClm == null ? null : (CollectionListManager) possibleClm;

		// load the deposit manager implementation
		this.cdm = (CollectionDepositManager) this.loadImplClass("collection-deposit-impl", false);

		// load the API
		this.api = new CollectionAPI(this.clm, this.cdm, this.config);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.api.get(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.api.post(req, resp);
	}
}
