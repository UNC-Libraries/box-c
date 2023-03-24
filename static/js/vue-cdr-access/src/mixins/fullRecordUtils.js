import breadCrumbs from '@/components/full_record/breadCrumbs.vue';
import modalMetadata from '@/components/modalMetadata.vue';
import thumbnail from '@/components/full_record/thumbnail.vue';
import isEmpty from 'lodash.isempty';

export default {
    components: { breadCrumbs, modalMetadata, thumbnail },

    data() {
        return {
            showMetadata: false
        }
    },

    props: {
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
            if (this.recordData.briefObject.countMap === undefined) {
                return `0 ${this.$t('full_record.items')}`;
            }
            const childCount = this.recordData.briefObject.countMap.child;
            const pluralizeItems = childCount === 1 ? this.$t('full_record.item') : this.$t('full_record.items');
            return `${childCount} ${pluralizeItems}`;
        },

        restrictedContent() {
            const brief_object = this.recordData.briefObject;
            if (brief_object === undefined || brief_object.roleGroup === undefined) {
                return false;
            }

            return !brief_object.roleGroup.includes('canViewOriginals|everyone');
        },

        loginUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/Shibboleth.sso/Login?target=${encodeURIComponent(current_page)}`;
        },

        allowsFullAuthenticatedAccess() {
            const group_roles = this.recordData.briefObject.groupRoleMap;
            if (group_roles === undefined || isEmpty(group_roles)) {
                return false;
            }

            return Object.keys(group_roles).includes('authenticated') &&
                group_roles.authenticated.includes('canViewOriginals');
        }
    },

    methods: {
        displayMetadata() {
            this.showMetadata = true;
        },

        toggleMetadata(show) {
            this.showMetadata = show;
        }
    }
}