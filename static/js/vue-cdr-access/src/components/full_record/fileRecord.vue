<template>
<div class="full_record">
        <div class="columns is-6 browse-top container">
            <div class="column is-narrow" :class="isDeleted">
                <thumbnail :thumbnail-data="recordData"></thumbnail>
            </div>
            <div class="column content">
                <h2 :class="isDeleted" class="title is-2 is-text-unc-blue">
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
                        <dd v-for="(exhibit_link, title, index) in recordData.exhibits">
                            <a :href="exhibit_link">{{ title }}</a>
                            <template v-if="hasMoreExhibits(index, recordData.exhibits)">; </template>
                        </dd>
                    </template>
                    <template v-if="fieldExists(recordData.briefObject.creator)">
                        <dt>{{ $t('full_record.creator') }}</dt>
                        <dd>{{ recordData.briefObject.creator.join('; ') }}</dd>
                    </template>
                    <template v-if="fieldExists(recordData.briefObject.filesizeTotal)">
                        <dt>{{ $t('full_record.filesize') }}</dt>
                        <dd>{{ formatFilesize(recordData.briefObject.filesizeTotal) }}</dd>
                    </template>
                    <template v-if="fieldExists(recordData.briefObject.created)">
                        <dt>{{ $t('full_record.date_created') }}</dt>
                        <dd>{{ formatDate(recordData.briefObject.created) }}</dd>
                    </template>
                    <template class="embargo" v-if="fieldExists(recordData.embargoDate)">
                        <dt>{{ $t('full_record.embargo_date') }}</dt>
                        <dd>{{ recordData.embargoDate }}</dd>
                    </template>
                </dl>
                <abstract v-if="recordData.briefObject.abstractText" :brief-object="recordData.briefObject"/>
                <router-link id="parent-url" :to="parentWorkUrl">{{ $t('full_record.view_parent_work') }}</router-link>
            </div>
            <object-actions :record-data="recordData"></object-actions>
        </div>
        <div class="has-background-white">
            <div class="container pb-5" v-if="recordData.viewerType">
                <player :record-data="recordData"></player>
            </div>
            <div class="container pb-6">
                <metadata-display :uuid="recordData.briefObject.id"
                              :can-view-metadata="hasPermission(recordData, 'viewMetadata')">
                </metadata-display>
            </div>
            <neighbor-list :current-record-id="recordData.briefObject.id" :neighbors="recordData.neighborList"></neighbor-list>
        </div>
    </div>
</template>

<script>
import fileUtils from '../../mixins/fileUtils';
import fullRecordUtils from '../../mixins/fullRecordUtils';
import abstract from '@/components/full_record/abstract.vue';
import player from '@/components/full_record/player.vue';
import metadataDisplay from '@/components/full_record/metadataDisplay.vue';
import neighborList from '@/components/full_record/neighborList.vue';
import objectActions from '@/components/full_record/objectActions.vue';

export default {
    name: 'fileRecord',

    components: {abstract, metadataDisplay, neighborList, objectActions, player },

    mixins: [fileUtils, fullRecordUtils],

    computed: {
        parentWorkUrl() {
            return `/record/${this.recordData.containingWorkUUID}`;
        }
    }
}
</script>