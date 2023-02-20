import isEmpty from 'lodash.isempty';

export default {
    data() {
        return {
            displayMetadate: false,
            showFullAbstract: false
        }
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

        childCount() {
            if (this.recordData.briefObject.countMap === undefined) {
                return 0;
            }
            return this.recordData.briefObject.countMap.child;
        },

        pluralizeItems() {
            return this.childCount === 1 ? 'item' : 'items';
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
        abstractDisplay() {
            this.showFullAbstract = !this.showFullAbstract;
        },

        metadataDisplay() {
            this.displayMetadate = true;
        },

        hideMetadata(show) {
            this.displayMetadate = show;
        }
    }
}