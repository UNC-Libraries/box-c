import breadCrumbs from '@/components/full_record/breadCrumbs.vue';
import modalMetadata from '@/components/modalMetadata.vue';
import thumbnail from '@/components/full_record/thumbnail.vue';
import isEmpty from 'lodash.isempty';

export default {
    components: { breadCrumbs, modalMetadata, thumbnail },

    data() {
        return {
            showFullAbstract: false,
            showMetadata: false
        }
    },

    props: {
        username: String,
        recordData: Object
    },

    computed: {
        abstractLinkText() {
            return this.showFullAbstract ? this.$t('full_record.read_less'): this.$t('full_record.read_more');
        },

        truncateAbstract() {
            return this.recordData.briefObject.abstractText !== undefined &&
                this.recordData.briefObject.abstractText.length > 350;
        },

        truncatedAbstractText() {
            if (this.truncateAbstract && !this.showFullAbstract) {
                return this.recordData.briefObject.abstractText.substring(0, 350);
            }

            return this.recordData.briefObject.abstractText;
        },

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
            return !this.recordData.briefObject.groupRoleMap.everyone.includes('canViewOriginals');
        },

        loginUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/Shibboleth.sso/Login?target=${encodeURIComponent(current_page)}`;
        },

        isLoggedIn() {
            return this.username !== undefined && this.username !== ''
        }
    },

    methods: {
        toggleAbstractDisplay() {
            this.showFullAbstract = !this.showFullAbstract;
        },

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
            return value.split(/[A-Z]/)[0];
        },

        editDescriptionUrl(id) {
            return `https://${window.location.host}/admin/describe/${id}`;
        },

        hasGroupRole(permission, user_type= 'everyone') {
            const group_roles = this.recordData.briefObject.groupRoleMap;
            if (group_roles === undefined || isEmpty(group_roles)) {
                return false;
            }

            return Object.keys(group_roles).includes(user_type) &&
                group_roles[user_type].includes(permission);
        },

        hasPermission(permission) {
            if (this.recordData.briefObject.permissions === undefined) {
                return false;
            }
            return this.recordData.briefObject.permissions.includes(permission);
        }
    }
}