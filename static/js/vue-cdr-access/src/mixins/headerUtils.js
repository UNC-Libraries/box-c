import { mapState } from 'pinia';
import { useAccessStore } from '../stores/access';

export default {
    data() {
        return {
            mobileMenuOpen: false
        }
    },

    computed: {
        ...mapState(useAccessStore, [
            'isLoggedIn',
            'username',
            'viewAdmin'
        ]),

        adminUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/admin/`;
        },

        jumpToAdminUrl() {
            const admin_base = `https://${window.location.host}/admin/`;
            if (this.$route?.name === 'displayRecords' && this.$route?.params?.id) {
                return `${admin_base}list/${this.$route.params.id}`;
            }
            return admin_base;
        },

        adminAccess() {
            return this.viewAdmin;
        }
    },

    methods: {
        toggleMobileMenu() {
            this.mobileMenuOpen = !this.mobileMenuOpen;
        }
    }
}