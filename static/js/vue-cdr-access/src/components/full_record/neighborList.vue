<template>
    <div class="onecol gray shadowtop">
        <div class="contentarea relateditems">
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
<style scoped lang="scss">
    .relateditems {
        display: inline-flex;
        flex-wrap: wrap;
        width: 100%;

        h2 {
            text-align: center;
            width: 100%;
        }
    }

    .relateditem {
        margin: 0 0 10px 0;
        padding: 10px 0;
        text-align: center;
        overflow: hidden;
        border: 1px solid transparent;
        max-width: 160px;
    }

    .relateditem.current_item,.relateditem:hover {
        border-color: #ddd;
        background-color: #eee;
    }

    .relatedthumb {
        height: auto;
        width: auto;
        display: block;
        text-align: center;
    }

    .relateditem p {
        font-size: 11px;
        line-height: 15px;
        clear: both;
        margin: 0 5px;
    }

    @media screen and (max-width: 600px) {
        .relateditem {
            max-width: 100%;
        }
    }
</style>