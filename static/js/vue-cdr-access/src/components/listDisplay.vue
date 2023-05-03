<!--
Renders search results in a list view display format
-->
<template>
    <div id="list-records-display">
        <div class="columns">
            <div class="column">
                <ul :class="{'margin-offset': isRecordBrowse}">
                    <li v-for="(record, index) in recordList" class="columns browseitem" :class="{stripe: index % 2 === 0}">
                        <div class="column is-narrow">
                            <thumbnail :thumbnail-data="record" size="medium"></thumbnail>
                        </div>
                        <div class="column metadata-fields">
                            <div class="result-title">
                                <router-link :class="{deleted: markedForDeletion(record)}" :to="recordUrl(record.id, linkBrowseType)">{{ record.title }}</router-link>
                                <span v-if="record.type !== 'File'" class="item_container_count">{{ countDisplay(record.counts.child) }}</span>
                            </div>
                            <div><span class="has-text-weight-bold">{{ $t('display.date_deposited') }}:</span> {{ formatDate(record.added) }}</div>
                            <div v-if="record.objectPath.length >= 3 && record.type !== 'Collection'">
                                <span class="has-text-weight-bold">{{ $t('display.collection') }}:</span> <router-link class="metadata-link" :to="recordUrl(record.objectPath[2].pid, linkBrowseType)">{{ collectionInfo(record.objectPath) }}</router-link>
                            </div>
                            <div v-if="record.objectPath.length >= 3 && showCollection(record)">
                                <p class="collection_id"><span class="has-text-weight-bold">{{ $t('display.collection_number') }}:</span> {{ record.objectPath[2].collectionId }}</p>
                            </div>
                            <div v-if="record.type === 'Work' || record.type === 'File'"><span class="has-text-weight-bold">{{ $t('display.file_type') }}:</span> {{ getFileType(record) }}</div>
                        </div>
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
    import { format } from 'date-fns';

    export default {
        name: 'listDisplay',

        components: {thumbnail},

        mixins: [displayUtils, permissionUtils],

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

            /**
             * @param record
             * @returns {string} File Type value for a work or file object. Will return the descriptive
             *      form of the file type if available, and fall back to the mimetype if not
             */
            getFileType(record) {
                let fileType = this.determineFileType(record.fileDesc);
                if (!fileType) {
                    fileType = this.determineFileType(record.fileType);
                }
                return fileType || '';
            },

            /**
             * Determines which filetype should be shown
             * For multiple filetypes it de-dupes the array and if only one value show that, otherwise show 'Various'
             * @param fileTypes
             * @returns {string|*|undefined}
             */
            determineFileType(fileTypes) {
                if (fileTypes && fileTypes.length === 1) {
                    return fileTypes[0];
                } else if (fileTypes && fileTypes.length > 1) {
                    return ([...new Set(fileTypes)].length === 1) ? fileTypes[0] : 'Various';
                } else {
                    return undefined;
                }
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
            -ms-hyphens: auto;
            -webkit-hyphens: auto;
            hyphens: auto;
            line-height: 1.2;
            margin-bottom: 15px;
            overflow-wrap: break-word;
            padding-right: 25px;
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

        .has-image-icon {
            top: 100px;
        }

        @media screen and (max-width: 1023px){
            .metadata-fields {
                margin-left: 10px;
            }
        }

        @media screen and (max-width: 768px) {
            margin-top: 25px;

            li {
                display: inherit;
            }

            .browseitem {
                float: none;
            }

            // images to their left are positioned absolutely with a width of 128px
            .metadata-fields {
                margin-left: 150px;
            }
        }

        @media screen and (max-width: 576px) {
            .metadata-fields {
                margin-left: 0;
            }
        }
    }
</style>