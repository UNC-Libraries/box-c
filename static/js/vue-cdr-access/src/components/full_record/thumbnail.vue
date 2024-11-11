<template>
    <router-link :to="linkToPath" :title="tooltip" :aria-label="ariaText" class="thumbnail" :class="imgClasses">
        <div v-if="src !== ''" :style="{ 'background-image': 'url(' + objectData.thumbnail_url + ')'}"
             :aria-label="altText(objectData.title)"
             role="img"
             class="thumbnail-viewer"
             :class="{restricted: markedForDeletion(objectData) || isRestricted(objectData)}"></div>
        <i v-else class="placeholder fa" :class="placeholderClass"></i>
        <div v-if="badgeIcon !== ''" class="thumbnail-badge">
            <div class="fa-stack">
                <i class="fas fa-circle fa-stack-2x background"></i>
                <i class="fas fa-stack-1x foreground" :class="badgeIcon"></i>
            </div>
        </div>
    </router-link>
</template>

<script>
import permissionUtils from '../../mixins/permissionUtils';

const types = ['AdminUnit', 'Collection', 'Folder', 'Work'];

export default {
    name: 'thumbnail',

    mixins: [permissionUtils],

    props: {
        thumbnailData: {
            type: Object,
            default: {}
        },
        // If provided, clicking the thumbnail will go to this url. Else it will go to the provided record's page.
        linkToUrl: {
            type: String,
            default: ''
        },
        size: {
            type: String,
            default: 'large'
        }
    },

    methods: {
        altText(title) {
            return `Thumbnail for ${title}`;
        }
    },

    computed: {
        objectData() {
            return this.permissionData(this.thumbnailData);
        },

        ariaText() {
            return `Visit ${this.objectData.title}`
        },

        badgeIcon() {
            if (this.thumbnailData.markedForDeletion) {
                return 'fa-trash';
            } else if (this.objectData.type !== 'AdminUnit' && !this.publicAccess(this.objectData)) {
                return 'fa-lock';
            } else {
                return '';
            }
        },

        imgClasses() {
            let class_list = [
                `thumbnail-size-${this.size}`
            ];
            if (this.thumbnailData.markedForDeletion) {
                class_list.push('deleted');
            }
            if (this.tooltip !== '') {
                class_list.push('has_tooltip');
            }

            return class_list.join(' ');
        },

        placeholderClass() {
            let type = this.objectData.type;
            if (type === 'AdminUnit') {
                return 'fa-university';
            } else if (type === 'Collection') {
                return 'fa-archive';
            } else if (type === 'Folder') {
                return 'fa-folder';
            } else {
                return 'fa-file';
            }
        },

        linkToPath() {
            if (this.linkToUrl !== '') {
                return this.linkToUrl;
            }
            return `/record/${this.objectData.id}`;
        },

        canView() {
            return (this.objectData.type === 'AdminUnit' || this.hasPermission(this.objectData, 'viewAccessCopies'));
        },

        src() {
            if (this.objectData.thumbnail_url !== undefined && this.canView()) {
                return this.objectData.thumbnail_url;
            }

            return '';
        },

        tooltip() {
            const record_type = this.objectData.type;
            if (types.includes(record_type)) {
                return this.$t('full_record.view_details', { title: this.objectData.title });
            }
            if (record_type === 'File') {
                return this.$t('full_record.view_title', { title: this.objectData.title });
            }
            if (record_type === 'List') {
                return this.$t('full_record.view_contents', { title: this.objectData.title });
            }

            return '';
        }
    }
}
</script>

<style scoped lang="scss">
    @media screen and (max-width: 600px) {
        a {
            margin-right: 15px;
        }
    }
</style>