<template>
    <div class="content-wrap full_record">
        <div class="full_record_top">
            <div class="browse-header aggregate-record">
                <div class="columns">
                    <div class="column">
                        <bread-crumbs :object-path="recordData.briefObject.objectPath">
                        </bread-crumbs>
                    </div>
                </div>
                <div class="columns">
                    <div class="column">
                        <h2>{{ recordData.briefObject.title }}</h2>
                        <div class="columns columns-resize aggregate-info">
                            <div class="column is-narrow-tablet" :class="isDeleted">
                                <thumbnail :thumbnail-data="recordData"
                                           :allows-full-access="hasGroupRole('canViewOriginals')"></thumbnail>
                            </div>
                        </div>
                        <div class="column">
                            <ul>
                                <li v-if="fieldExists(recordData.briefObject.added)">
                                    <span class="has-text-weight-bold">{{ $t('full_record.date_added') }}: </span>
                                    {{ formatDate(recordData.briefObject.added) }}
                                </li>
                                <li v-if="fieldExists(recordData.briefObject.parentCollectionName)">
                                    <span class="has-text-weight-bold">{{ $t('display.collection') }}: </span>
                                    <a class="parent-collection" :href="parentUrl">{{ recordData.briefObject.parentCollectionName }}</a>
                                </li>
                                <li v-if="fieldExists(recordData.briefObject.collectionId)">
                                    <span class="has-text-weight-bold">{{ $t('full_record.collection_id') }}: </span>
                                    {{ recordData.briefObject.collectionId }}
                                </li>
                                <li><span class="has-text-weight-bold">{{ $t('full_record.finding_aid') }}: </span>
                                    <template v-if="fieldExists(recordData.findingAidUrl)">
                                        <a class="finding-aid" :href="recordData.findingAidUrl">">{{ recordData.findingAidUrl }}</a>
                                    </template>
                                    <template v-else>{{ $t('full_record.no_finding_aid') }}</template>
                                </li>
                                <li v-if="fieldExists(recordData.briefObject.creator)">
                                    <span class="has-text-weight-bold">{{ $t('full_record.creator') }}: </span>
                                    {{ recordData.briefObject.creator.join('; ') }}
                                </li>
                                <template>
                                    <template v-if="fieldExists(recordData.briefObject.fileFormatType)">
                                        <li>
                                            <span class="has-text-weight-bold">{{ $t('full_record.file_type') }}: </span>
                                            {{ getFileType(recordData.briefObject) }}
                                        </li>
                                        <li v-if="recordData.briefObject.filesizeSort !== -1">
                                            <span class="has-text-weight-bold">{{ $t('full_record.filesize') }}: </span>
                                            {{ formatFilesize(recordData.briefObject.filesizeSort) }}
                                        </li>
                                    </template>
                                    <li v-else>
                                        <span class="has-text-weight-bold">{{ $t('full_record.contains') }}: </span>
                                        {{ displayChildCount }}
                                    </li>
                                </template>
                                <li v-if="fieldExists(recordData.briefObject.created)">
                                    <span class="has-text-weight-bold">{{ $t('full_record.date_created') }}: </span>
                                    {{ formatDate(recordData.briefObject.created) }}
                                </li>
                                <li v-if="fieldExists(recordData.briefObject.embargoDate)">
                                    <span class="has-text-weight-bold">{{ $t('full_record.embargo_date') }}: </span>
                                    {{ formatDate(recordData.briefObject.embargoDate) }}
                                </li>
                                <template v-if="fieldExists(recordData.briefObject.abstractText)">
                                    <li v-if="truncateAbstract" class="abstract">{{ truncatedAbstractText }}...
                                        <a class="abstract-text" @click.prevent="toggleAbstractDisplay()" href="#">{{ abstractLinkText }}</a>
                                    </li>
                                    <li v-else class="abstract">{{ recordData.briefObject.abstractText }}</li>
                                </template>
                                <li v-if="fieldExists(recordData.exhibits)">
                                    <span class="has-text-weight-bold">{{ $t('full_record.related_digital_exhibits') }}: </span>
                                    <template v-for="(exhibit, index) in recordData.exhibits">
                                        <a :href="exhibit.value">{{ exhibit.key }}</a><template v-if="index < recordData.exhibits.length - 1">;</template>
                                    </template>
                                </li>
                            </ul>
                        </div>
                    </div>
                    <div class="column is-narrow action-btn item-actions">
                        <div v-if="restrictedContent && !isLoggedIn" class="column is-narrow item-actions">
                            <div class="restricted-access">
                                <h2>{{ $t('full_record.restricted_content', { resource_type: recordData.briefObject.type.toLowerCase() }) }}</h2>
                                <div v-if="hasGroupRole('canViewOriginals', 'authenticated')" class="actionlink"><a class="button login-link" :href="loginUrl"><i class="fa fa-id-card"></i> {{ $t('access.login') }}</a></div>
                                <div class="actionlink"><a class="button contact" href="https://library.unc.edu/wilson/contact/"><i class="fa fa-envelope"></i> {{ $t('access.contact') }}</a></div>
                            </div>
                        </div>
                        <div v-if="hasPermission('editDescription')" class="actionlink right">
                            <a class="edit button" :href="editDescriptionUrl(recordData.briefObject.id)"><i class="fa fa-edit"></i> {{ $t('full_record.edit') }}</a>
                        </div>
                        <div v-if="fieldExists(recordData.dataFileUrl) && hasPermission('viewOriginal')" class="actionlink right download">
                            <a class="download button" :href="downloadLink"><i class="fa fa-download"></i> {{ $t('full_record.download') }}</a>
                        </div>
                        <div v-else-if="fieldExists(recordData.briefObject.embargoDate) && fieldExists(recordData.briefObject.dataFileUrl)" class="noaction right">
                            Available after {{ formatDate(recordData.briefObject.embargoDate) }}
                        </div>
                    </div>
                </div>
            </div>
            <div class="clear">
                <template v-if="(recordData.viewerType === 'uv' && hasPermission('viewAccessCopies')) ||
                (recordData.viewerType === 'pdf' && hasPermission('viewOriginal'))">
                    <iframe :src="viewer(recordData.viewerType)" allow="fullscreen" scrolling="no"></iframe>
                </template>
                <template v-else-if="recordData.viewerType === 'audio' && hasPermission('viewAccessCopies')">
                    <audio-player :datafile-url="recordData.dataFileUrl"></audio-player>
                </template>
            </div>
            <file-list v-if="childCount > 0"
                       :child-count="childCount"
                       :work-id="recordData.briefObject.id"
                       :edit-access="hasPermission('editDescription')"></file-list>
            <div v-if="hasPermission('viewMetadata')">
                <h2 class="full-metadata">{{ $t('full_record.detailed_metadata') }}</h2>
                <div id="mods_data_display" v-html="metadata"></div>
            </div>
            <neighbor-list :current-record-id="recordData.briefObject.id" :neighbors="recordData.neighborList"></neighbor-list>
        </div>
    </div>
</template>

<script>
import fileUtils from '../../mixins/fileUtils';
import fullRecordUtils from '../../mixins/fullRecordUtils';
import audioPlayer from '@/components/full_record/audioPlayer.vue';
import fileList from '@/components/full_record/fileList.vue';
import neighborList from '@/components/full_record/neighborList.vue';
import get from 'axios';

export default {
    name: 'aggregateRecord',

    components: {audioPlayer, fileList, neighborList},

    mixins: [fileUtils, fullRecordUtils],

    data() {
        return {
            metadata: ''
        }
    },

    computed: {
        parentUrl() {
            return `record/${this.recordData.briefObject.parentCollectionId}`
        },

        downloadLink() {
            return `${this.recordData.dataFileUrl}?dl=true`;
        }
    },

    methods: {
        loadMetadata() {
            get(`record/${this.recordData.briefObject.id}/metadataView`).then((response) => {
                this.metadata = response.data;
                this.hasLoaded = true;
            }).catch((error) => {
                console.log(error);
                this.metadata = '';
                this.hasLoaded = true;
            });
        },

        viewer(viewer_type) {
            return `record/${this.recordData.briefObject.id}/${viewer_type}Viewer`;
        }
    },

    mounted() {
        if (this.hasPermission('viewMetadata')) {
            this.loadMetadata();
        }
    }
}
</script>
