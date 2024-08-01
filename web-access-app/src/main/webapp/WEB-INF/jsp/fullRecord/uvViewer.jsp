<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="en">
<head>
    <c:set var="url">${accessBaseUrl}</c:set>
    <base href="${url}" />
    <%@ include file="../../html/headElements.html"%>

    <script src="https://www.unpkg.com/@samvera/clover-iiif@latest/dist/web-components/index.umd.js"></script>
</head>
<body class="white">
<div class="clear_space"></div>
<div id="jp2_viewer" class="jp2_imageviewer_window uv">
    <clover-viewer
            id="${url}services/api/iiif/v3/${viewerPid}/manifest"
    />
</div>
</body>
</html>