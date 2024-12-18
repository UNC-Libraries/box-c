<!--
Renders search results in a gallery view display in full record pages.
-->
<template>
    <div class="browse-records-display">
        <div class="grid is-col-min-9 is-column-gap-3 is-row-gap-6">
            <div v-for="record in recordList" class="cell">
                <thumbnail :thumbnail-data="record" :link-to-url="recordUrl(record, 'gallery-display')"></thumbnail>
                <router-link class="record-title mt-4" :class="{deleted: markedForDeletion(record)}" :to="recordUrl(record, 'gallery-display')">{{ record.title }}</router-link>
            </div>
        </div>
    </div>
</template>

<script>
    import thumbnail from '@/components/full_record/thumbnail.vue';
    import displayUtils from '../mixins/displayUtils';
    import permissionUtils from '../mixins/permissionUtils';

    export default {
        name: 'galleryDisplay',

        props: {
            recordList: {
                default: [],
                type: Array
            }
        },

        components: {thumbnail},

        mixins: [displayUtils, permissionUtils]
    };
</script>

<style scoped lang="scss">
    .browse-records-display {
        .record-title {
            display: block;
            -ms-hyphens: auto;
            -webkit-hyphens: auto;
            hyphens: auto;
            line-height: 1.2;
            max-width: 250px;
            overflow-wrap: break-word;
            font-size: 1.2rem;
            text-align: center;
        }

        img.restricted {
            float: none;
        }

        .thumbnail.thumbnail-size-large {
            min-height: 128px;
        }
    }
</style>