import breadCrumbs from '@/components/full_record/breadCrumbs.vue';
import modalMetadata from '@/components/modalMetadata.vue';
import thumbnail from '@/components/full_record/thumbnail.vue';
import isEmpty from 'lodash.isempty';
import {format} from 'date-fns';

export default {
    components: { breadCrumbs, modalMetadata, thumbnail },

    data() {
        return {
            showFullAbstract: false,
            showMetadata: false
        }
    },

    props: {
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
            if (this.recordData.briefObject.countMap === undefined) {
                return 0;
            }
            return this.recordData.briefObject.countMap.child;
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
        },

        hasEditAccess() {
            const group_roles = this.recordData.briefObject.groupRoleMap;
            if (group_roles === undefined || isEmpty(group_roles)) {
                return false;
            }

            return Object.keys(group_roles).includes('authenticated') &&
                group_roles.authenticated.includes('canDescribe');
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
            return format(value, 'yyyy-MM-dd');
        },

        editDescriptionUrl(id) {
            return `${window.location.host}/describe/${id}`;
        },

        hasAccess(user_type, permission) {
            const group_roles = this.recordData.briefObject.groupRoleMap;
            if (group_roles === undefined || isEmpty(group_roles)) {
                return false;
            }

            return Object.keys(group_roles).includes(user_type) &&
                group_roles[user_type].includes(permission);
        }
    }
}