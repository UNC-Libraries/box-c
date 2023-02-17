<template>
    <a :href="thumbUrl" :title="tooltip" :aria-label="ariaText" class="thumbnail" :class="imgClasses">
        <div class="thumbnail-placeholder">
            <span v-if="contentType !== ''" class="thumbnail-content-type">{{ contentType }}</span>
        </div>

        <div v-if="src !== ''" class="thumbnail-preview">
            <img :src="src" :alt="thumbnailData.briefObject.title"/>
        </div>

        <div v-if="badgeIcon !== ''" class="thumbnail-badge thumbnail-badge-${badgeIcon}" :class="badgeIcon">
            <div class="fa-stack">
                <i class="fas fa-circle fa-stack-2x background"></i>
                <i class="fas fa-stack-1x foreground" :class="badgeIcon"></i>
            </div>
        </div>
    </a>
</template>

<script>
import isEmpty from "lodash.isempty";

const types = ['AdminUnit', 'Collection', 'Folder', 'Work'];

export default {
    name: 'thumbnail',

    props: {
        thumbnailData: {
            type: Object,
            default: {}
        },
        size: {
            type: String,
            default: 'large'
        }
    },

    computed: {
        ariaText() {
            return `Visit ${this.thumbnailData.briefObject.title}`
        },

        hasThumbnail() {
            return Object.keys(this.thumbnailData).length > 0;
        },

        badgeIcon() {
            if (this.hasThumbnail && this.thumbnailData.markedForDeletion) {
                return 'fa-trash';
            } else if (this.hasThumbnail && this.thumbnailData.resourceType !== 'AdminUnit'
                && !this.allowsFullAuthenticatedAccess) {
                return 'fa-lock';
            } else {
                return '';
            }
        },

        imgClasses() {
            let class_list = [
                `thumbnail-resource-type-${this.thumbnailData.resourceType.toLowerCase()}`,
                `thumbnail-size-${this.size}`
            ];
            if (this.src === '') {
                class_list.push('placeholder')
            }
            if (this.thumbnailData.markedForDeletion) {
                class_list.push('deleted');
            }
            if (this.tooltip !== '') {
                class_list.push('has_tooltip');
            }

            return class_list.join(' ');
        },

        contentType() {
            return '';
        },

        src() {
            if (this.thumbnailData.briefObject.thumbnailId !== undefined) {
                return `https://${window.location.host}/services/api/thumb/${this.thumbnailData.briefObject.thumbnailId}/${this.size}`;
            }

            return '';
        },

        tooltip() {
            const record_type = this.thumbnailData.resourceType;
            if (types.includes(record_type)) {
                return `View details for ${this.thumbnailData.briefObject.title}`;
            }

            if (record_type === 'File') {
                return `View ${this.thumbnailData.briefObject.title}`;
            }

            if (record_type === 'List') {
                return `View the contents of ${this.thumbnailData.briefObject.title}`;
            }

            return '';
        },

        allowsFullAuthenticatedAccess() {
            const group_roles = this.thumbnailData.briefObject.groupRoleMap;
            if (group_roles === undefined || isEmpty(group_roles)) {
                return false;
            }

            return Object.keys(group_roles).includes('authenticated') &&
                group_roles.authenticated.includes('canViewOriginals');
        }
    },

    methods: {

    }
}
</script>

<style scoped lang="scss">

</style>