<!--
Renders search results in a list view display format
-->
<template>
    <div id="list-records-display">
        <div class="columns">
            <div class="column">
                <div v-for="(record, index) in recordList" class="columns is-mobile browseitem py-5" :class="{'has-background-white-ter': index % 2 === 0}">
                    <div class="column is-narrow pt-3">
                        <thumbnail :thumbnail-data="record" size="medium" :link-to-url="recordUrl(record, linkBrowseType)"></thumbnail>
                    </div>
                    <div class="column metadata-fields">
                        <h3 class="subtitle is-4 mb-4">
                            <router-link :class="{deleted: markedForDeletion(record)}" :to="recordUrl(record, linkBrowseType)">{{ record.title }}</router-link>
                            <span v-if="record.type !== 'File'" class="tag is-primary ml-4">{{ countDisplay(record.counts.child) }}</span>
                        </h3>
                        <dl class="property-grid">
                            <dt>{{ $t('display.date_deposited') }}</dt>
                            <dd>{{ formatDate(record.added) }}</dd>
                            <template v-if="record.objectPath.length >= 3 && record.type !== 'Collection'">
                                <dt>{{ $t('display.collection') }}</dt>
                                <dd><router-link class="metadata-link" :to="recordUrl(record.objectPath[2].pid, linkBrowseType)">{{ collectionInfo(record.objectPath) }}</router-link></dd>
                            </template>
                            <template v-if="record.objectPath.length >= 3 && showCollection(record)">
                                <dt>{{ $t('display.collection_id') }}</dt>
                                <dd class="collection_id">{{ record.objectPath[2].collectionId }}</dd>
                            </template>
                            <template v-if="record.type === 'Work' || record.type === 'File'">
                                <dt>{{ $t('display.file_type') }}</dt>
                                <dd>{{ getFileType(record) }}</dd>
                            </template>
                        </dl>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import thumbnail from '@/components/full_record/thumbnail.vue';
    import displayUtils from '../mixins/displayUtils';
    import fileUtils from '../mixins/fileUtils';
    import permissionUtils from '../mixins/permissionUtils';
    import { format } from 'date-fns';

    export default {
        name: 'listDisplay',

        components: {thumbnail},

        mixins: [displayUtils, fileUtils, permissionUtils],

        props: {
            recordList: {
                default: () => [],
                type: Array
            },
            isRecordBrowse: {
                default: false,
                type: Boolean
            },
            excludeBrowseTypeFromRecordUrls: {
                default: false,
                type: Boolean
            }
        },

        computed: {
            linkBrowseType() {
                if (this.excludeBrowseTypeFromRecordUrls) {
                    return null;
                } else {
                    return 'list-display';
                }
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

            showCollection(record) {
                return record.type !== 'AdminUnit' && record.type !== 'Collection' &&
                    record.objectPath[2].collectionId !== null;
            }
        }
    }
</script>