import fullRecordUtils from "./fullRecordUtils";

export default {
    mixins: [fullRecordUtils],

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
            const current_page = window.location;
            // <c:when test="${not empty resultResponse && not empty resultResponse.selectedContainer}">
            //     <s:eval var="jumpToAdmin" expression=
            //         "T(edu.unc.lib.boxc.common.util.URIUtil).join(adminBaseUrl, 'list', resultResponse.selectedContainer.id)" />
            // </c:when>
            if (this.recordData.briefObject != null && this.recordData.briefObject.resourceType == 'File') {
                return `https://${current_page.host}/admin/list/${this.recordData.briefObject.ancestorPathFacet.searchKey}`
            } else if (this.recordData.briefObject != null) {
                return `https://${current_page.host}/admin/list/${this.recordData.briefObject.id}`
            } else {
                return `https://${current_page.host}/admin/`
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

        getUsername() {
            let address = window.location.search;
            let parameterList = new URLSearchParams(address);
            if (parameterList.get("username") !== undefined && parameterList.get("username") !== '') {
                return true;
            }
        },

        adminAccess() {
            let address = window.location.search;
            let parameterList = new URLSearchParams(address);
            if (parameterList.get("accessLevel") === "admin") {
                return true;
            }
        }
    }
}