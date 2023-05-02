<template>
    <div class="onecol gray shadowtop">
        <div class="contentarea">
            <h2 class="link-list">{{ $t('full_record.neighbor_list') }}</h2>
            <template v-for="neighbor in neighbors">
                <div class="relateditem" :class="{current_item: currentRecordId === neighbor.id}">
                    <div class="relatedthumb" :class="neighborIsDeleted(neighbor.status)">
                        <thumbnail :thumbnail-data="neighbor"
                                   :allows-full-access="allowsPublicAccess(neighbor)"
                                   size="small"></thumbnail>
                    </div>
                    <p><router-link :to="fullRecordUrl(neighbor.id)">{{ truncateText(neighbor.title) }}</router-link></p>
                </div>
            </template>
        </div>
    </div>
</template>

<script>
import thumbnail from '@/components/full_record/thumbnail.vue';
import isEmpty from "lodash.isempty";

export default {
    name: 'neighborList',

    components: {thumbnail},

    props: {
        currentRecordId: String,
        neighbors: {
            type: Array,
            default: []
        }
    },

    methods: {
        fullRecordUrl(neighbor_id) {
            return `/record/${neighbor_id}`;
        },

        neighborIsDeleted(status) {
            return status.includes('Marked For Deletion') ? 'deleted' : '';
        },

        truncateText(title) {
            return title.substring(0, 50);
        },

        allowsPublicAccess(neighbor) {
            const group_roles = neighbor.groupRoleMap;
            if (group_roles === undefined || isEmpty(group_roles)) {
                return false;
            }

            return Object.keys(group_roles).includes('everyone') &&
                group_roles.everyone.includes('canViewOriginals');
        }
    }
}
</script>