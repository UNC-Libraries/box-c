export default {
    computed: {
        adminUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/admin/`;
        },

        jumpToAdminUrl() {
            const current_page = window.location.href;
            const admin_base = `https://${window.location.host}/admin/`;
            if (current_page.includes('record')) {
                let split_url = current_page.split('/');
                let id = split_url[4];
                return `${admin_base}list/${id}`;
            }
            return admin_base;
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
            return username !== null && username !== '';
        },

        adminAccess() {
            return document.getElementById("pagewrap").dataset.admin;
        }
    }
}