<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="en">
<head>
    <c:set var="url">${accessBaseUrl}</c:set>
    <base href="${url}" />
    <%@ include file="../../html/headElements.html"%>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/universalviewer@4.0.22/dist/uv.css"/>
    <script src="https://cdn.jsdelivr.net/npm/universalviewer@4.0.22/dist/umd/UV.js"></script>
</head>
<body class="white">
    <div class="clear_space"></div>
    <div id="jp2_viewer" class="jp2_imageviewer_window uv"></div>
    <script src="/static/plugins/uv/uv_init.js"></script>
</body>
</html>