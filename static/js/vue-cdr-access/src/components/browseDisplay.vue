<template>
    <div class="browse-records-display">
        <div class="columns">
            <div class="column is-12" >
                <ul class="column is-12" v-for="records in chunkedRecords">
                    <li v-for="record in records" class="column" :class="column_size">
                        <a :href="recordUrl(record.id, 'gallery-display')">
                            <img v-if="thumbnailPresent(record.thumbnail_url)" :src="record.thumbnail_url" :alt="altText(record.title)" class="thumbnail thumbnail-size-large">
                            <i v-else class="fa" :class="recordType(record.type)"></i>
                            <div class="record-title">{{ record.title }}</div>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</template>

<script>
    import displayUtils from '../mixins/displayUtils';
    import debounce from 'lodash.debounce';
    import chunk from 'lodash.chunk';

    export default {
        name: 'browseDisplay',

        props: {
            recordList: {
                default: [],
                type: Array
            }
        },

        mixins: [displayUtils],

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
            line-height: 1.2;
            margin-top: 25px;
            word-break: break-all;
        }

        .thumbnail + .record-title {
            margin-top: 165px;
        }
    }
</style>