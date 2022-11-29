<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>

    <div class="restricted-access">
        <h2>This ${fn:toLowerCase(briefObject.resourceType)} has restricted content</h2>
        <c:if test="${permsHelper.allowsFullAuthenticatedAccess(briefObject)}">
            <div class="actionlink"><a class="button" href="${loginUrl}"><i class="fa fa-id-card"></i> Log in for access (UNC Onyen)</a></div>
        </c:if>
        <div class="actionlink"><a class="button" href="${contactUrl}"><i class="fa fa-envelope"></i> Contact Wilson Library for access</a></div>
    </div>
