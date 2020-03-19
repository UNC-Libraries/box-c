<template>
    <div id="list-records-display">
        <div class="columns">
            <div class="column is-12">
                <ul :class="{'margin-offset': isRecordBrowse}">
                    <li v-for="(record, index) in recordList" class="columns browseitem" :class="{stripe: index % 2 === 0}">
                        <div class="column is-2">
                            <a :href="recordUrl(record.id, linkBrowseType)" :class="{deleted: markedForDeletion(record)}">
                                <img v-if="thumbnailPresent(record.thumbnail_url)" :src="record.thumbnail_url"
                                     :alt="altText(record.title)" class="thumbnail thumbnail-size-large">
                                <i v-else class="fa" :class="recordType(record.type)"></i>
                                <div v-if="markedForDeletion(record)" class="thumbnail-badge-trash"
                                     :class="{'deleted-image-icon': thumbnailPresent(record.thumbnail_url),
                                     'thumbnail-badge-trash-search ': !isRecordBrowse}">
                                    <div class="fa-stack">
                                        <i class="fa fa-circle fa-stack-2x background"></i>
                                        <i class="fa fa-trash fa-stack-1x foreground"></i>
                                    </div>
                                </div>
                                <div v-else-if="isRestricted(record)" class="thumbnail-badge-lock"
                                     :class="{'deleted-image-icon': thumbnailPresent(record.thumbnail_url),
                                     'thumbnail-badge-lock-search ': !isRecordBrowse}">
                                    <div class="fa-stack">
                                        <i class="fa fa-circle fa-stack-2x background"></i>
                                        <i class="fa fa-lock fa-stack-1x foreground"></i>
                                    </div>
                                </div>
                            </a>
                        </div>
                        <div class="column is-10">
                            <div class="result-title">
                                <a :class="{deleted: markedForDeletion(record)}" :href="recordUrl(record.id, linkBrowseType)">{{ record.title }}</a>
                                <span v-if="record.type !== 'File'" class="searchitem_container_count">{{ countDisplay(record.counts.child) }}</span>
                            </div>
                            <div><span class="has-text-weight-bold">Date Deposited:</span> {{ formatDate(record.added) }}</div>
                            <div v-if="record.objectPath.length >= 3 && record.type !== 'Collection'">
                                <span class="has-text-weight-bold">Collection:</span> <a class="metadata-link" :href="recordUrl(record.objectPath[2].pid, linkBrowseType)">{{ collectionInfo(record.objectPath) }}</a>
                            </div>
                            <div v-if="record.objectPath.length >= 3 && showCollection(record)">
                                <p class="collection_id"><span class="has-text-weight-bold">Collection Number:</span> {{ record.objectPath[2].collectionId }}</p>
                            </div>
                            <div v-if="record.type === 'Work' || record.type === 'File'"><span class="has-text-weight-bold">File Type:</span> {{ getFileType(record.datastream) }}</div>
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
            },
            isRecordBrowse: {
                default: false,
                type: Boolean
            },
            useSavedBrowseType: {
                default: false,
                type: Boolean
            }
        },

        computed: {
            linkBrowseType() {
                if (this.useSavedBrowseType) {
                    let saved_browse_type = sessionStorage.getItem('browse-type');
                    return saved_browse_type !== null ? saved_browse_type : 'gallery-display';
                }

                return 'list-display';
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
            },

            showCollection(record) {
                return record.type !== 'AdminUnit' && record.type !== 'Collection' &&
                    record.objectPath[2].collectionId !== null;
            }
        }
    }
</script>

<style scoped lang="scss">
    #list-records-display {
        ul.margin-offset {
            width: inherit;
            margin-left: 15px;
            margin-right: -15px;
        }

        li {
            align-items: center;
            display: inline-flex;
            padding-bottom: 20px;
            padding-top: 20px;
        }

        .is-2 {
            position: relative;
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
            font-size: 7rem;
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

        .thumbnail-badge-trash,
        .thumbnail-badge-lock {
            margin-top: -55px;
            padding-bottom: 15px;
            padding-left: 75px;

            .fa-circle {
                font-size: 4rem;
            }

            .fa-trash,
            .fa-lock {
                font-size: 2rem;
                margin: 8px 0;
            }
        }


        .deleted-image-icon {
            top: 100px;
        }

        @media screen and (max-width: 1024px) {
            .is-2 {
                margin-right: 25px;
            }

            .thumbnail-badge-trash,
            .thumbnail-badge-lock {
                padding-left: 55px;
            }
        }

        @media screen and (max-width: 768px) {
            margin-top: 25px;

            li {
                display: inherit;
            }

            .browseitem {
                float: none;
                padding-left: 15px;
                text-align: center;
            }
        }
    }
</style>