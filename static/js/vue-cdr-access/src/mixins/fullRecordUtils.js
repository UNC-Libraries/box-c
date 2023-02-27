import breadCrumbs from '@/components/full_record/breadCrumbs.vue';
import modalMetadata from '@/components/modalMetadata.vue';
import thumbnail from '@/components/full_record/thumbnail.vue';
import isEmpty from 'lodash.isempty';

export default {
    components: { breadCrumbs, modalMetadata, thumbnail },

    data() {
        return {
            displayMetadata: false,
            showFullAbstract: false
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
        toggleAbstractDisplay() {
            this.showFullAbstract = !this.showFullAbstract;
        },

        displayMetadata() {
            this.displayMetadata = true;
        },

        toggleMetadata(show) {
            this.displayMetadata = show;
        }
    }
}