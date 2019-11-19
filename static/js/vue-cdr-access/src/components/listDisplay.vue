<template>
    <div class="list-records-display">
        <div class="columns">
            <div class="column is-12" >
                <ul>
                    <li v-for="(record, index) in recordList" class="columns" :class="{stripe: index % 2 === 0}">
                        <div class="column is-2">
                            <a :href="recordUrl(record.id)">
                                <img v-if="thumbnailPresent(record.thumbnail_url)" :src="record.thumbnail_url" :alt="altText(record.title)" class="thumbnail thumbnail-size-small">
                                <i v-else class="fa" :class="recordType(record.type)"></i>
                            </a>
                        </div>
                        <div class="column is-10">
                            <div class="result-title">
                                <a :href="recordUrl(record.id, 'list-display')">{{ record.title }}</a>
                                <span v-if="record.type !== 'Work'" class="searchitem_container_count">{{ countDisplay(record.counts.child) }}</span>
                            </div>
                            <div><span class="has-text-weight-bold">Date Deposited:</span> {{ formatDate(record.added) }}</div>
                            <div v-if="record.objectPath.length >= 3">
                                <span class="has-text-weight-bold">Collection:</span> <a class="metadata-link" :href="recordUrl(record.objectPath[2].pid, 'list-display')">{{ collectionInfo(record.objectPath) }}</a>
                            </div>
                            <div v-if="record.type === 'Work'"><span class="has-text-weight-bold">File Type:</span> {{ getFileType(record.datastream) }}</div>
                        </div>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</template>

<script>
    import displayUtils from '../mixins/displayUtils';
    import { format } from 'date-fns';

    export default {
        name: 'listDisplay',

        mixins: [displayUtils],

        props: {
            recordList: {
                default: () => [],
                type: Array
            }
        },

        methods: {
            countDisplay(count) {
                let wording = 'items';

                if (count === 1) {
                    wording = 'item';
                }

                return `${count} ${wording}`;
            },

            formatDate(date_string) {
                return format(new Date(date_string), 'MMMM do, yyyy');
            },

            collectionInfo(object_path) {
                if (object_path.length >= 3) {
                    return object_path[2].name;
                }

                return '';
            },

            getFileType(datastream_info) {
                for (let i in datastream_info) {
                    let ds_parts = datastream_info[i].split('\|');

                    if (ds_parts.length < 5 || ds_parts[0] !== 'original_file') {
                        continue;
                    }

                    return ds_parts[3];
                }

                return '';
            }
        }

    }
</script>

<style scoped lang="scss">
    li {
        align-items: center;
        display: inline-flex;
        padding-bottom: 20px;
        padding-top: 20px;
        width: 100%;
    }

    .is-2 {
        text-align: center;
    }

    a {
        font-size: 1.5rem;
    }

    a.metadata-link {
        font-size: 1rem;
        margin-left: 5px;
    }

    div {
        margin-bottom: 5px;
    }

    .result-title {
        margin-bottom: 15px;
    }

    i {
        color: #007FAE;
        font-size: 5rem;
    }

    img.thumbnail {
        float: none;
        margin: auto;
    }

    span {
        margin-left: 10px;
    }

    .has-text-weight-bold {
        margin-left: 0;
    }

    .stripe {
        background-color: #f7f7f7;
    }
</style>