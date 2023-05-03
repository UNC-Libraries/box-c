<template>
    <router-link :to="currentPage" :title="tooltip" :aria-label="ariaText" class="thumbnail" :class="imgClasses">
        <img v-if="src !== ''" :src="objectData.thumbnail_url"
             :alt="altText(objectData.title)"
             :class="{restricted: markedForDeletion(objectData) || isRestricted(objectData)}">
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

    .thumbnail.thumbnail-size-large {
        width: 160px;
        .placeholder {
            font-size: 9em;
        }
        .thumbnail-badge {
            font-size: 200%;
            bottom: 0;
            right: 8px;
        }
    }

    .thumbnail.thumbnail-size-medium {
        width: 140px;
        .placeholder {
            font-size: 7em;
        }
        .thumbnail-badge {
            font-size: 150%;
            bottom: -50px;
        }
    }

    .thumbnail.thumbnail-size-small {
        width: 64px;
        overflow-y: hidden;
        .placeholder {
            font-size: 5em;
        }
        .thumbnail-badge {
            bottom: 1px;
            right: 0;
        }
    }
</style>