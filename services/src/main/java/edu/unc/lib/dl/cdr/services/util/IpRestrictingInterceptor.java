package edu.unc.lib.dl.cdr.services.util;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class IpRestrictingInterceptor  extends HandlerInterceptorAdapter {
	private Pattern allowRegEx = null;
	
	public String getAllowRegEx() {
        if (allowRegEx == null) {
            return null;
        }
        return allowRegEx.toString();
	}

	public void setAllowRegEx(String allowRegEx) {
        if (allowRegEx == null || allowRegEx.length() == 0) {
            this.allowRegEx = null;
        } else {
            this.allowRegEx = Pattern.compile(allowRegEx);
        }
	}

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		if (allowRegEx != null && allowRegEx.matcher(request.getRemoteAddr()).matches()) {
			return super.preHandle(request, response, handler);
        } else {
        	response.sendError(403);
        	return false;
        }
	}
	
}
