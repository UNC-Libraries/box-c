<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="crosswalk.*" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><c:out value="${form.title}"/></title>
</head>
<body>
<h1><c:out value="${form.title}"/></h1>
<c:out value="${formId}"/>
<form:form modelAttribute="form" enctype="multipart/form-data">
	<c:forEach items="${form.elements}" varStatus="elementRow">
		<spring:bind path="form.elements[${elementRow.index}]" ignoreNestedPath="true">
			<% if(Paragraph.class.isInstance(status.getValue())) { 
					Paragraph p = (Paragraph)status.getValue(); 
					if(p.getHeading() != null) { %>
					<h3><%= p.getHeading() %></h3>
					<% }
					if(p.getText() != null) { %>
					<p><%= ((Paragraph)status.getValue()).getText() %></p>
					<% } %>
			<% } else if(MetadataBlock.class.isInstance(status.getValue())) { 
					MetadataBlock mb = (MetadataBlock)status.getValue(); %>
					<div style="outline: gray solid 4px;">
					<h3><%= ((MetadataBlock)status.getValue()).getName() %></h3>
					<% if(mb.getDescription() != null) { %>
					<p><%= ((MetadataBlock)status.getValue()).getDescription() %></p>
					<% } %>
					<c:forEach items="${form.elements[elementRow.index].ports}" var="port" varStatus="portRow">
						<spring:bind path="form.elements[${elementRow.index}].ports[${portRow.index}]" ignoreNestedPath="true">
							<c:out value="${port.label}"/>
							<form:input path="elements[${elementRow.index}].ports[${portRow.index}].enteredValue" />
							<c:out value="${port.usage}"/>
							<form:errors cssStyle="color:red;" path="elements[${elementRow.index}].ports[${portRow.index}].enteredValue" /><br />
						</spring:bind>
					</c:forEach>
					</div>
			<% } %>
		</spring:bind>
	</c:forEach>
	File for deposit <input name="file" type="file" /><br />
	<input type="submit" value="submit" />
</form:form>
</body>
</html>