<template>
    <div class="gray shadowtop">
        <div class="contentarea">
            <h2 class="link-list">{{ $t('full_record.neighbor_list') }}</h2>
            <template v-for="neighbor in neighborList">
                <div class="relateditem" :class="currentItemClass">
                    <div class="relatedthumb" :class="neighborIsDeleted(neighbor.status)">
                        <thumbnail :thumbnail-data="neighbor"
                                   allows-full-access="true"
                                   size="small"></thumbnail>
                    </div>
                    <p><a :href="fullRecordUrl(neighbor.id)">{{ truncateText(neighbor.title) }}</a></p>
                </div>
            </template>
        </div>
    </div>
</template>

<script>
import thumbnail from '@/components/full_record/thumbnail.vue';

export default {
    name: 'neighborList',

    components: {thumbnail},

    props: {
        currentRecordId: Number,
        neighborList: {
            type: Array,
            default: []
        }
    },

    methods: {
        currentItemClass(neighbor_id) {
            return this.currentRecordId === neighbor_id;
        },

        fullRecordUrl(neighbor_id) {
            return `record/${neighbor_id}`;
        },

        neighborIsDeleted(status) {
            return status.includes('Marked For Deletion') ? 'deleted' : '';
        },

        truncateText(title) {
            return title.substring(0, 50);
        }
    }
}
</script>

<style scoped lang="scss">
    .relateditem {
        float: none;
    }
</style>