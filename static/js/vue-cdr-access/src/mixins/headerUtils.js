export default {
    props: {
        recordData: Object
    },

    computed: {
        adminUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/admin/`;
        },

        homeUrl() {
            let current_page = window.location;
            return `https://${current_page.host}/`;
        },

        jumpToAdminUrl() {
            //         <c:when test="${not empty resultResponse && not empty resultResponse.selectedContainer}">
            //             <s:eval var="jumpToAdmin" expression=
            //                 "T(edu.unc.lib.boxc.common.util.URIUtil).join(adminBaseUrl, 'list', resultResponse.selectedContainer.id)"/>
            //         </c:when>
            const current_page = window.location;
            const search_key = this.recordData.briefObject.ancestorPathFacet.searchKey;
            const id = this.recordData.briefObject.id;
            if (this.recordData.briefObject.resourceType === 'File') {
                return `https://${current_page.host}/list/${search_key}`;
            } else if (this.recordData.briefObject !== null) {
                return `https://${current_page.host}/list/${id}`;
            } else {
                return `https://${current_page.host}/admin/`;
            }
        },

        logoutUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/Shibboleth.sso/Logout?return=https://sso.unc.edu/idp/logout.jsp?return_url=${encodeURIComponent(current_page)}`;
        },

        loginUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/Shibboleth.sso/Login?target=${encodeURIComponent(current_page)}`;
        },

        isLoggedIn() {
            const username = document.getElementById("pagewrap").dataset.username;
            return username !== null;
        },

        adminAccess() {
            return document.getElementById("pagewrap").dataset.admin;
        }
    }
}