<template>
    <a :href="currentPage" :title="tooltip" :aria-label="ariaText" class="thumbnail" :class="imgClasses">
        <div class="thumbnail-placeholder">
            <span v-if="src === ''" class="thumbnail-content-type">{{ contentType }}</span>
        </div>

        <div v-if="src !== ''" class="thumbnail-preview">
            <img :src="src" :alt="objectData.title"/>
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
        objectData() {
            if (this.thumbnailData.briefObject !== undefined) {
                return this.thumbnailData.briefObject
            }
            return this.thumbnailData;
        },

        ariaText() {
            return `Visit ${this.objectData.title}`
        },

        badgeIcon() {
            if (this.thumbnailData.markedForDeletion) {
                return 'fa-trash';
            } else if (this.objectData.type !== 'AdminUnit'
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
            const file_type = this.objectData.fileFormatCategory;
            if (file_type === undefined || file_type.length === 0 || file_type[0] === 'unknown') {
                return ''
            }
            return file_type[0];
        },

        placeholder() {
            const type = this.objectData.type.toLowerCase();
            if (type === 'adminunit' || type === 'work') {
                return 'document';
            }
            return type;
        },

        currentPage() {
            return window.location;
        },

        src() {
            if (this.objectData.thumbnail_url !== undefined) {
                return this.objectData.thumbnail_url;
            }

            return '';
        },

        tooltip() {
            const record_type = this.objectData.type;
            if (types.includes(record_type)) {
                return `View details for ${this.objectData.title}`;
            }

            if (record_type === 'File') {
                return `View ${this.objectData.title}`;
            }

            if (record_type === 'List') {
                return `View the contents of ${this.objectData.title}`;
            }

            return '';
        }
    }
}
</script>