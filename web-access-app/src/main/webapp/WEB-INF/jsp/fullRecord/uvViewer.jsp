<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<link rel="stylesheet" href="/static/plugins/uv/uv.css">
<div class="clear_space"></div>
<div id="jp2_viewer" class="jp2_imageviewer_window"></div>
<script>
    (function() {
        let viewer = document.getElementById('jp2_viewer');
        viewer.setAttribute('data-url', window.location.pathname.split('/')[2]);
    })();
</script>
<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/public/fullRecord"></script>
