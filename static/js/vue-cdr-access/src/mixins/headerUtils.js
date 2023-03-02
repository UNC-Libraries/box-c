export default {
    computed: {
        adminUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/admin/`;
        },

        homeUrl() {
            let url = window.location.href;
            return url.slice(0, url.lastIndexOf('/'));
        },

        jumptoAdminUrl() {
            //TODO: jumptoAdmin urls
            // <c:if test="${sessionScope.accessLevel != null && sessionScope.accessLevel.viewAdmin}">
            //     <c:choose>
            //         <c:when test="${not empty resultResponse && not empty resultResponse.selectedContainer}">
            //             <s:eval var="jumpToAdmin" expression=
            //                 "T(edu.unc.lib.boxc.common.util.URIUtil).join(adminBaseUrl, 'list', resultResponse.selectedContainer.id)"/>
            //         </c:when>
            //         <c:when test="${not empty briefObject && briefObject.resourceType == 'File'}">
            //             <s:eval var="jumpToAdmin" expression=
            //                 "T(edu.unc.lib.boxc.common.util.URIUtil).join(adminBaseUrl, 'list', briefObject.ancestorPathFacet.searchKey)"/>
            //         </c:when>
            //         <c:when test="${not empty briefObject}">
            //             <s:eval var="jumpToAdmin" expression=
            //                 "T(edu.unc.lib.boxc.common.util.URIUtil).join(adminBaseUrl, 'list', briefObject.id)"/>
            //         </c:when>
            //         <c:otherwise>
            //             <c:set var="jumpToAdmin" value="${adminBaseUrl}"/>
            //         </c:otherwise>
            //     </c:choose>
            //     <a href="${jumpToAdmin}" className="navbar-item" target="_blank">Admin</a>
            // </c:if>
            const current_page = window.location;
            return `https://${current_page.host}/admin/`;
        },

        logoutUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/Shibboleth.sso/Logout?return=https://sso.unc.edu/idp/logout.jsp?return_url=${encodeURIComponent(current_page)}`;
        },

        loginUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/Shibboleth.sso/Login?target=${encodeURIComponent(current_page)}`;
        },

        getUsername() {
            const username = document.getElementById("pagewrap").dataset.username;
            if (username !== null) {
                return true;
            }
            return false;
        },

        adminAccess() {
            return document.getElementById("pagewrap").dataset.admin;
        }
    }
}