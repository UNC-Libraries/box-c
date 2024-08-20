import modalMetadata from '@/components/modalMetadata.vue';
import thumbnail from '@/components/full_record/thumbnail.vue';
import permissionUtils from "./permissionUtils";
import { formatInTimeZone } from 'date-fns-tz';
import { mapState } from 'pinia';
import { useAccessStore } from '../stores/access';

export default {
    components: { modalMetadata, thumbnail },

    data() {
        return {
            showMetadata: false
        }
    },

    mixins: [permissionUtils],

    props: {
        recordData: Object
    },

    computed: {
        ...mapState(useAccessStore, [
            'isLoggedIn',
            'username'
        ]),

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
            if (!this.hasGroups(this.recordData) ||
                this.recordData.briefObject.groupRoleMap.everyone === undefined) {
                return false;
            }
            if (this.recordData.briefObject.groupRoleMap.everyone.includes('canViewOriginals')) {
                return false;
            }
            // For File objects, content is not restricted if the user can at least download low res files
            if (this.recordData.resourceType == 'File' && this.hasDownloadAccess(this.recordData)) {
                return false;
            }
            return true;
        },

        loginUrl() {
            const current_page = window.location;
            return `https://${current_page.host}/Shibboleth.sso/Login?target=${encodeURIComponent(current_page)}`;
        },

        downloadLink() {
            return `${this.recordData.dataFileUrl}?dl=true`;
        },

        parentUrl() {
            return `/record/${this.recordData.briefObject.parentCollectionId}`;
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

        downloadBulkUrl(id) {
            return `https://${window.location.host}/services/api/bulkDownload/${id}`;
        },

        hasMoreExhibits(current_exhibit, exhibits) {
            return current_exhibit < Object.keys(exhibits).length - 1;
        }
    }
}