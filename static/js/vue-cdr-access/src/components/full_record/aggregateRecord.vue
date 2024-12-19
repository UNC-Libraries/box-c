<template>
    <div class="full_record">
        <div class="columns is-6-desktop browse-top container is-mobile is-1-mobile">
            <div class="column is-narrow-desktop is-5-mobile" :class="isDeleted">
                <thumbnail :thumbnail-data="recordData"></thumbnail>
                <div class="download-jump mt-5 has-text-centered">
                    <a class="button action is-primary is-responsive" :href="filesLink">
                        <span class="icon"><i class="fa fa-download" aria-hidden="true"></i></span><span>Skip to Download</span>
                    </a>
                </div>
            </div>
            <div class="column content is-7-mobile">
                <h2 :class="isDeleted" class="title is-3 is-text-unc-blue">
                    {{ recordData.briefObject.title }}
                </h2>
                <dl class="property-grid">
                    <template v-if="fieldExists(recordData.briefObject.added)">
                        <dt>{{ $t('full_record.date_added') }}</dt>
                        <dd>{{ formatDate(recordData.briefObject.added) }}</dd>
                    </template>
                    <dt>{{ $t('display.collection') }}</dt>
                    <dd><router-link class="parent-collection" :to="parentUrl">{{ recordData.briefObject.parentCollectionName }}</router-link></dd>
                    <template v-if="fieldExists(recordData.findingAidUrl)">
                        <dt>{{ $t('full_record.finding_aid') }}</dt>
                        <dd><a class="finding-aid" :href="recordData.findingAidUrl">{{ recordData.findingAidUrl }}</a></dd>
                    </template>
                    <template v-if="fieldExists(recordData.exhibits)">
                        <dt>{{ $t('full_record.related_digital_exhibits') }}</dt>
                        <dd class="exhibits">
                            <span v-for="(exhibit_link, title, index) in recordData.exhibits">
                                <a :href="exhibit_link">{{ title }}</a>
                                <template v-if="hasMoreExhibits(index, recordData.exhibits)">; </template>
                            </span>
                        </dd>
                    </template>
                    <template v-if="fieldExists(recordData.briefObject.creator)">
                        <dt>{{ $t('full_record.creator') }}</dt>
                        <dd>{{ recordData.briefObject.creator.join('; ') }}</dd>
                    </template>
                    <dt>{{ $t('full_record.contains') }}</dt>
                    <dd>{{ displayChildCount }}</dd>
                    <template v-if="fieldExists(recordData.briefObject.created)">
                        <dt>{{ $t('full_record.date_created') }}</dt>
                        <dd>{{ formatDate(recordData.briefObject.created) }}</dd>
                    </template>
                    <template class="embargo" v-if="fieldExists(recordData.embargoDate)">
                        <dt>{{ $t('full_record.embargo_date') }}</dt>
                        <dd class="embargo">{{ recordData.embargoDate }}</dd>
                    </template>
                </dl>
                <abstract v-if="recordData.briefObject.abstractText" :brief-object="recordData.briefObject"/>
            </div>
            <object-actions :record-data="recordData"></object-actions>
        </div>
        <div class="full_record_bottom pb-6">
            <div class="container pb-5" v-if="recordData.viewerType">
                <player :record-data="recordData"></player>
            </div>
            <template v-if="childCount > 0">
                <div class="file-list-header columns is-vcentered container">
                    <h3 class="column subtitle">{{ $t('full_record.item_list') }} ({{ childCount }})</h3>
                    <bulk-download :has-bulk-download-access="recordData.canBulkDownload"
                                   :total-download-size="recordData.totalDownloadSize"
                                   :work-id="recordData.briefObject.id"
                                   :child-count="childCount">
                    </bulk-download>
                </div>
                <div class="container">
                    <file-list id="file-display"
                            :work-id="recordData.briefObject.id"
                            :download-access="hasDownloadAccess(recordData)"
                            :edit-access="hasPermission(recordData,'editDescription')">
                    </file-list>
                </div>
            </template>
            <div class="container">
                <metadata-display :uuid="recordData.briefObject.id"
                                :can-view-metadata="hasPermission(recordData, 'viewMetadata')">
                </metadata-display>
            </div>
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