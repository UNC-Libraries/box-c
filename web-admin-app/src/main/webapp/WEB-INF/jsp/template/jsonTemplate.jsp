<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>
<c:if test="${not empty contentPage}">
	<c:import var="contentText" url="${contentPage}" />
{
	"content" :  ${cdr:objectToJSON(contentText)}
	<c:if test="${not empty additionalData}">
	, "data" : {
		<c:forEach items="${additionalData}" var="dataEntry">
			"${dataEntry.key}" : ${cdr:objectToJSON(dataEntry.value)}
		</c:forEach>
	}
	</c:if>
} 
</c:if>
