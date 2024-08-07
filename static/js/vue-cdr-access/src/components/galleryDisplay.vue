<!--
Renders search results in a gallery view display in full record pages.
-->
<template>
    <div class="browse-records-display">
        <div class="columns">
            <div class="column is-12" >
                <ul class="column is-12" v-for="records in chunkedRecords">
                    <li v-for="record in records" class="column" :class="column_size">
                        <thumbnail :thumbnail-data="record" :link-to-url="recordUrl(record, 'gallery-display')"></thumbnail>
                        <router-link class="record-title" :class="{deleted: markedForDeletion(record)}" :to="recordUrl(record, 'gallery-display')">{{ record.title }}</router-link>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</template>

<script>
    import thumbnail from '@/components/full_record/thumbnail.vue';
    import displayUtils from '../mixins/displayUtils';
    import permissionUtils from '../mixins/permissionUtils';
    import debounce from 'lodash.debounce';
    import chunk from 'lodash.chunk';

    export default {
        name: 'galleryDisplay',

        props: {
            recordList: {
                default: [],
                type: Array
            }
        },

        components: {thumbnail},

        mixins: [displayUtils, permissionUtils],

        data() {
            return {
                column_size: 'is-3',
            }
        },

        computed: {
            chunkedRecords() {
                if (this.column_size === 'is-4') {
                    return chunk(this.recordList, 3);
                } else if (this.column_size === 'is-6') {
                    return chunk(this.recordList, 2);
                } else {
                    return chunk(this.recordList, 4);
                }
            }
        },

        methods: {
            numberOfColumns() {
                let screen_size = window.innerWidth;

                if (screen_size > 1023) {
                    this.column_size = 'is-3';
                } else if (screen_size > 768) {
                    this.column_size = 'is-4';
                } else {
                    this.column_size = 'is-6';
                }
            }
        },

        mounted() {
            window.addEventListener('resize', debounce(this.numberOfColumns, 300));
        },

        beforeDestroy() {
            window.removeEventListener('resize', this.numberOfColumns);
        }
    };
</script>

<style scoped lang="scss">
    .browse-records-display {
        .columns {
            display: inline-flex;
            width: 100%;
        }

        ul {
            display: inline-flex;
            width: 100%;
        }

        li {
            text-indent: 0;
        }

        div {
            font-size: 1.2rem;
        }

        i {
            font-size: 9rem;
        }

        .record-title {
            display: block;
            float: left;
            -ms-hyphens: auto;
            -webkit-hyphens: auto;
            hyphens: auto;
            line-height: 1.2;
            margin-top: 5px;
            max-width: 250px;
            overflow-wrap: break-word;
            width: 95%;
            font-size: 1.2rem;
        }

        img.restricted {
            float: none;
        }
    }
</style>