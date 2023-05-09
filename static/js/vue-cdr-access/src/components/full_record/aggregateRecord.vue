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
                                </div>
                                <div class="column">
                                    <ul class="record-metadata">
                                        <li v-if="fieldExists(recordData.briefObject.added)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.date_added') }}: </span>
                                            {{ formatDate(recordData.briefObject.added) }}
                                        </li>
                                        <li v-if="fieldExists(recordData.briefObject.parentCollectionName)">
                                            <span class="has-text-weight-bold">{{ $t('display.collection') }}: </span>
                                            <router-link class="parent-collection" :to="parentUrl">{{ recordData.briefObject.parentCollectionName }}</router-link>
                                        </li>
                                        <li v-if="fieldExists(recordData.briefObject.collectionId)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.collection_id') }}: </span>
                                            {{ recordData.briefObject.collectionId }}
                                        </li>
                                        <li><span class="has-text-weight-bold">{{ $t('full_record.finding_aid') }}: </span>
                                            <template v-if="fieldExists(recordData.findingAidUrl)">
                                                <a class="finding-aid" :href="recordData.findingAidUrl">{{ recordData.findingAidUrl }}</a>
                                            </template>
                                            <template v-else>{{ $t('full_record.no_finding_aid') }}</template>
                                        </li>
                                        <li v-if="fieldExists(recordData.briefObject.creator)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.creator') }}: </span>
                                            {{ recordData.briefObject.creator.join('; ') }}
                                        </li>
                                        <template v-if="fieldExists(recordData.briefObject.fileDesc)">
                                            <li>
                                                <span class="has-text-weight-bold">{{ $t('full_record.file_type') }}: </span>
                                                {{ getFileType(recordData.briefObject) }}
                                            </li>
                                            <li v-if="fieldExists(recordData.briefObject.filesizeTotal)">
                                                <span class="has-text-weight-bold">{{ $t('full_record.filesize') }}: </span>
                                                {{ formatFilesize(recordData.briefObject.filesizeTotal) }}
                                            </li>
                                        </template>
                                        <li v-else>
                                            <span class="has-text-weight-bold">{{ $t('full_record.contains') }}: </span>
                                            {{ displayChildCount }}
                                        </li>
                                        <li v-if="fieldExists(recordData.briefObject.created)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.date_created') }}: </span>
                                            {{ formatDate(recordData.briefObject.created) }}
                                        </li>
                                        <li v-if="fieldExists(recordData.briefObject.embargoDate)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.embargo_date') }}: </span>
                                            {{ formatDate(recordData.briefObject.embargoDate) }}
                                        </li>
                                        <abstract v-if="recordData.briefObject.abstractText" :brief-object="recordData.briefObject"/>
                                        <li v-if="fieldExists(recordData.exhibits)">
                                            <span class="has-text-weight-bold">{{ $t('full_record.related_digital_exhibits') }}: </span>
                                            <template v-for="(exhibit, index) in recordData.exhibits">
                                                <a :href="exhibit.value">{{ exhibit.key }}</a><template v-if="index < recordData.exhibits.length - 1">;</template>
                                            </template>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                    <restricted-content :record-data="recordData"></restricted-content>
                </div>
            </div>
        </div>
        <div class="full_record_bottom">
            <player :record-data="recordData"></player>
            <file-list v-if="childCount > 0"
                       :child-count="childCount"
                       :work-id="recordData.briefObject.id"
                       :edit-access="hasPermission(recordData,'editDescription')">
            </file-list>
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
import player from '@/components/full_record/player.vue';
import restrictedContent from '@/components/full_record/restrictedContent.vue';

export default {
    name: 'aggregateRecord',

    components: {abstract, fileList, neighborList, metadataDisplay, player, restrictedContent},

    mixins: [fileUtils, fullRecordUtils],
}
</script>
