export default {
    props: {
        searchResults: Object,
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
            const admin_base = `https://${window.location.host}/admin/`;
            const container_id = this.searchResults.container_metadata.id;
            const search_key = this.recordData.briefObject.ancestorPathFacet.searchKey;
            const object_id = this.recordData.briefObject.id;
            if (this.searchResults.container_metadata !== null) {
                return `https://${admin_base}/list/${container_id}`;
            } else if (this.recordData.briefObject.resourceType === 'File') {
                return `https://${admin_base}/list/${search_key}`;
            } else if (this.recordData.briefObject !== null) {
                return `https://${admin_base}/list/${object_id}`;
            } else {
                return admin_base;
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