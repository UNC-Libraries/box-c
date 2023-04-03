import breadCrumbs from '@/components/full_record/breadCrumbs.vue';
import modalMetadata from '@/components/modalMetadata.vue';
import thumbnail from '@/components/full_record/thumbnail.vue';
import isEmpty from 'lodash.isempty';
import { formatInTimeZone } from 'date-fns-tz';

export default {
    components: { breadCrumbs, modalMetadata, thumbnail },

    data() {
        return {
            showMetadata: false
        }
    },

    props: {
        username: String,
        recordData: Object
    },

    computed: {
        isDeleted() {
            if (this.recordData.markedForDeletion) {
                return 'deleted';
            }
            return '';
        },

        displayChildCount() {
            const pluralizeItems = this.childCount === 1 ? this.$t('full_record.item') : this.$t('full_record.items');
            return `${this.childCount} ${pluralizeItems}`;
        },

        childCount() {
            if (this.recordData.briefObject.counts === undefined) {
                return 0;
            }
            return this.recordData.briefObject.counts.child;
        },

        restrictedContent() {
            if (!this.hasGroups) {
                return false;
            }
            return !this.recordData.briefObject.groupRoleMap.everyone.includes('canViewOriginals');
        },

        loginUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/Shibboleth.sso/Login?target=${encodeURIComponent(current_page)}`;
        },

        isLoggedIn() {
            return this.username !== undefined && this.username !== '';
        },

        hasGroups() {
            const group_roles = this.recordData.briefObject.groupRoleMap;
            return !(group_roles === undefined || isEmpty(group_roles));
        },

        downloadLink() {
            return `${this.recordData.dataFileUrl}?dl=true`;
        },

        parentUrl() {
            return `record/${this.recordData.briefObject.parentCollectionId}`;
        }
    },

    methods: {
        displayMetadata() {
            this.showMetadata = true;
        },

        toggleMetadata(show) {
            this.showMetadata = show;
        },

        fieldExists(value) {
            return value !== undefined;
        },

        formatDate(value) {
            return formatInTimeZone(value, 'America/New_York', 'yyyy-MM-dd');
        },

        editDescriptionUrl(id) {
            return `https://${window.location.host}/admin/describe/${id}`;
        },

        hasGroupRole(role, user_type= 'everyone') {
            if (!this.hasGroups) {
                return false;
            }

            const group_roles = this.recordData.briefObject.groupRoleMap;
            return Object.keys(group_roles).includes(user_type) &&
                group_roles[user_type].includes(role);
        },

        hasPermission(permission) {
            if (this.recordData.briefObject.permissions === undefined) {
                return false;
            }
            return this.recordData.briefObject.permissions.includes(permission);
        }
    }
}