<template>
    <router-link :to="currentPage" :title="tooltip" :aria-label="ariaText" class="thumbnail" :class="imgClasses">
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
        size: {
            type: String,
            default: 'large'
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
            } else if (this.objectData.type !== 'AdminUnit' && !this.publicAccess) {
                return 'fa-lock';
            } else {
                return '';
            }
        },

        publicAccess() {
            return this.objectData.status !== undefined && this.objectData.status.includes('Public Access');
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
            const file_type = this.objectData.format;
            if (file_type === undefined || file_type.length === 0 || file_type[0] === 'unknown') {
                return ''
            }
            return file_type[0];
        },

        placeholder() {
            const type = this.objectData.type.toLowerCase();
            if (type === 'adminunit' || type === 'work' || type === 'file') {
                return 'document';
            }
            return type;
        },

        currentPage() {
            return `/record/${this.objectData.id}`;
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