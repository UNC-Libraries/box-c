<template>
    <a :href="currentPage" :title="tooltip" :aria-label="ariaText" class="thumbnail" :class="imgClasses">
        <div class="thumbnail-placeholder">
            <span v-if="src === ''" class="thumbnail-content-type">{{ contentType }}</span>
        </div>

        <div v-if="src !== ''" class="thumbnail-preview">
            <img :src="src" :alt="thumbnailData.briefObject.title"/>
        </div>

        <div v-if="badgeIcon !== ''" class="thumbnail-badge">
            <div class="fa-stack">
                <i class="fas fa-circle fa-stack-2x background"></i>
                <i class="fas fa-stack-1x foreground" :class="badgeIcon"></i>
            </div>
        </div>
    </a>
</template>

<script>

const types = ['AdminUnit', 'Collection', 'Folder', 'Work'];

export default {
    name: 'thumbnail',

    props: {
        allowsFullAccess: {
            type: Boolean,
            default: true
        },
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
                && !this.allowsFullAccess) {
                return 'fa-lock';
            } else {
                return '';
            }
        },

        imgClasses() {
            let class_list = [
                `thumbnail-size-${this.size}`
            ];
            if (this.src === '') {
                class_list.push('placeholder');
                class_list.push(`thumbnail-resource-type-${this.placeholder}`);
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
            const file_type = this.thumbnailData.briefObject.fileFormatCategory;
            if (file_type === undefined || file_type.length === 0 || file_type[0] === 'unknown') {
                return ''
            }
            return file_type[0];
        },

        placeholder() {
            const type = this.thumbnailData.resourceType.toLowerCase();
            if (type === 'adminunit' || type === 'work') {
                return 'document';
            }
            return type;
        },

        currentPage() {
            return window.location;
        },

        src() {
            if (this.thumbnailData.briefObject.thumbnailId !== undefined) {
                return `https://${this.currentPage.host}/services/api/thumb/${this.thumbnailData.briefObject.thumbnailId}/${this.size}`;
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
        }
    }
}
</script>

<style scoped lang="scss">

</style>