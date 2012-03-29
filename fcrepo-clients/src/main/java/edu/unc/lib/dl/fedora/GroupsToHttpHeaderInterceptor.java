package edu.unc.lib.dl.fedora;

import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.CommonsHttpConnection;

import edu.unc.lib.dl.httpclient.HttpClientUtil;

public class GroupsToHttpHeaderInterceptor implements ClientInterceptor {
	private static final Logger LOG = LoggerFactory.getLogger(GroupsToHttpHeaderInterceptor.class);

	@Override
	public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
		LOG.debug("In handleRequest()");
		String groups = GroupsThreadStore.getGroups();
		if (groups != null) {
			LOG.debug("GOT GROUPS FROM THREAD");
			TransportContext context = TransportContextHolder.getTransportContext();
			CommonsHttpConnection connection = (CommonsHttpConnection) context.getConnection();
			PostMethod postMethod = connection.getPostMethod();
			postMethod.addRequestHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, groups);
			LOG.debug("Added HTTP header to POST: " + HttpClientUtil.FORWARDED_GROUPS_HEADER + " : " + groups);
		} else {
			LOG.debug("NO GROUPS SET ON THREAD");
		}
		return true;
	}

	@Override
	public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
		return true;
	}

	@Override
	public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
		return true;
	}

}
