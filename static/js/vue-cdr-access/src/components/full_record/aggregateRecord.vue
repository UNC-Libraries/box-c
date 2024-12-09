<template>
    <div class="content-wrap full_record">
        <div class="full_record_top">
            <div class="aggregate-record">
                <div class="columns browse-top">
                    <div class="column">
                        <h2>{{ recordData.briefObject.title }}</h2>
                        <div class="column">
                            <div class="columns is-tablet">
                                <div class="column is-narrow" :class="isDeleted">
                                    <thumbnail :thumbnail-data="recordData"></thumbnail>
                                    <div class="actionlink download-jump">
                                        <a class="button action" :href="filesLink">
                                            <i class="fa fa-download" aria-hidden="true"></i> Skip to Download
                                        </a>
                                    </div>
                                </div>
                                <div class="column">
                                    <ul class="record-metadata">
                                        <li v-if="fieldExists(recordData.briefObject.added)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.date_added') }}: </span>
                                            {{ formatDate(recordData.briefObject.added) }}
                                        </li>
                                        <li>
                                            <span class="has-text-weight-bold">{{ $t('display.collection') }}: </span>
                                            <router-link class="parent-collection" :to="parentUrl">{{ recordData.briefObject.parentCollectionName }}</router-link>
                                        </li>
                                        <li v-if="fieldExists(recordData.findingAidUrl)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.finding_aid') }}: </span>
                                            <a class="finding-aid" :href="recordData.findingAidUrl">{{ recordData.findingAidUrl }}</a>
                                        </li>
                                        <li class="exhibits" v-if="fieldExists(recordData.exhibits)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.related_digital_exhibits') }}: </span>
                                            <template v-for="(exhibit_link, title, index) in recordData.exhibits">
                                                <a :href="exhibit_link">{{ title }}</a>
                                                <template v-if="hasMoreExhibits(index, recordData.exhibits)">; </template>
                                            </template>
                                        </li>
                                        <li v-if="fieldExists(recordData.briefObject.creator)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.creator') }}: </span>
                                            {{ recordData.briefObject.creator.join('; ') }}
                                        </li>
                                        <li>
                                            <span class="has-text-weight-bold">{{ $t('full_record.contains') }}: </span>
                                            {{ displayChildCount }}
                                        </li>
                                        <li v-if="fieldExists(recordData.briefObject.created)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.date_created') }}: </span>
                                            {{ formatDate(recordData.briefObject.created) }}
                                        </li>
                                        <li class="embargo" v-if="fieldExists(recordData.embargoDate)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.embargo_date') }}: </span>
                                            {{ recordData.embargoDate }}
                                        </li>
                                        <abstract v-if="recordData.briefObject.abstractText" :brief-object="recordData.briefObject"/>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                    <object-actions :record-data="recordData"></object-actions>
                </div>
            </div>
        </div>
        <div class="full_record_bottom container is-fluid">
            <player :record-data="recordData"></player>
            <template v-if="childCount > 0">
                <div class="file-list-header columns is-vcentered">
                    <h3 class="column">{{ $t('full_record.item_list') }} ({{ childCount }})</h3>
                    <bulk-download :has-bulk-download-access="recordData.canBulkDownload"
                                   :total-download-size="recordData.totalDownloadSize"
                                   :work-id="recordData.briefObject.id"
                                   :child-count="childCount">
                    </bulk-download>
                </div>
                <file-list id="file-display"
                           :work-id="recordData.briefObject.id"
                           :download-access="hasDownloadAccess(recordData)"
                           :edit-access="hasPermission(recordData,'editDescription')">
                </file-list>
            </template>
            <metadata-display :uuid="recordData.briefObject.id"
                              :can-view-metadata="hasPermission(recordData, 'viewMetadata')">
            </metadata-display>
        </div>
        <neighbor-list :current-record-id="recordData.briefObject.id"
                       :neighbors="recordData.neighborList">
        </neighbor-list>
    </div>
</template>

<script>
import fileUtils from '../../mixins/fileUtils';
import fullRecordUtils from '../../mixins/fullRecordUtils';
import abstract from '@/components/full_record/abstract.vue';
import fileList from '@/components/full_record/fileList.vue';
import metadataDisplay from '@/components/full_record/metadataDisplay.vue';
import neighborList from '@/components/full_record/neighborList.vue';
import objectActions from '@/components/full_record/objectActions.vue';
import player from '@/components/full_record/player.vue';
import bulkDownload from "@/components/full_record/bulkDownload.vue";

export default {
    name: 'aggregateRecord',

    components: {bulkDownload, abstract, fileList, neighborList, metadataDisplay, objectActions, player},

    mixins: [fileUtils, fullRecordUtils],

    computed: {
        filesLink() {
            const current_page = location.href;
            return /file-display$/.test(current_page) ? current_page : `${current_page}#file-display`;
        }
    }
}
</script>

<style scoped lang="scss">
    .actionlink.download-jump {
        margin: 0 5px 0 0;
    }

    .actionlink a.action {
        font-size: 1rem;
        padding: 10px;
    }
</style>
