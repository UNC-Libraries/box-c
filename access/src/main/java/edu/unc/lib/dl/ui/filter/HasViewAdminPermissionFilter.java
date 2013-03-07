package edu.unc.lib.dl.ui.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

public class HasViewAdminPermissionFilter extends OncePerRequestFilter {
	private SolrQueryLayerService queryLayer;
	
	@Override
	public void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException,
			ServletException {
		
		this.checkForAdminPermissions(req);
		chain.doFilter(req, res);
	}
	
	private void checkForAdminPermissions(HttpServletRequest request) {
		// Only check if the user is authenticated
		if (GroupsThreadStore.getUsername() == null || GroupsThreadStore.getUsername().length() == 0) 
			return;
		
		request.setAttribute("hasAdminViewPermission", queryLayer.hasAdminViewPermission(GroupsThreadStore.getGroups()));
	}

	public void setQueryLayer(SolrQueryLayerService queryLayer) {
		this.queryLayer = queryLayer;
	}
}
