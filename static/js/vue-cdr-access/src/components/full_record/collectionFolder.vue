<template>
    <div class="browse-header">
        <div class="columns">
            <div class="column">
                <bread-crumbs :ignore-search-state="false"
                              :object-path="recordData.briefObject.objectPath">
                </bread-crumbs>
            </div>
        </div>
        <div class="columns">
            <div class="column" :class="{restrictedContent: 'is-8'}">
                <h2 :class="isDeleted">
                    <thumbnail :thumbnail-data="recordData"
                               :allows-full-access="allowsFullAuthenticatedAccess"></thumbnail>
                    {{ recordData.briefObject.title }}
                    <span class="item_container_count">{{ childCount }} {{ pluralizeItems }}</span>
                </h2>
                <p v-if="fieldExists(recordData.briefObject.dateAdded)">
                    <strong>{{ $t('full_record.date_added') }}: </strong>
                    {{ formatDate(recordData.briefObject.dateAdded) }}
                </p>
                <p v-if="fieldExists(recordData.briefObject.collectionId)">
                    <strong>{{ $t('full_record.collection_id') }}: </strong>
                    {{ recordData.briefObject.collectionId }}
                </p>
                <p><strong>{{ $t('full_record.finding_aid') }}: </strong>
                    <template v-if="fieldExists(recordData.findingAidUrl)">
                        <a :href="recordData.findingAidUrl">">{{ recordData.findingAidUrl }}</a>
                    </template>
                    <template v-else>Doesn't have a finding aid</template>
                </p>
                <template v-if="fieldExists(recordData.briefObject.abstractText)">
                    <template v-if="truncateAbstract">
                        <p>{{ truncatedAbstractText }}... <a class="abstract-text" @click.prevent="abstractDisplay()"
                                                             href="#">{{ abstractLinkText }}</a></p>
                    </template>
                    <template v-else><p>{{ recordData.briefObject.abstractText }}</p></template>
                </template>
                <p v-if="fieldExists(recordData.exhibits)">
                    <strong>{{ $t('full_record.related_digital_exhibits') }}: </strong>
                    <template v-for="(exhibit, index) in recordData.exhibits">
                        <a :href="exhibit.value">{{ exhibit.key }}</a><template v-if="index < recordData.exhibits.length - 1">;</template>
                    </template>
                </p>
                <p><a @click.prevent="metadataDisplay()" href="#">{{ $t('full_record.additional_metadata') }}</a></p>
            </div>
            <div v-if="restrictedContent" class="column is-narrow-desktop item-actions">
                <div class="restricted-access">
                    <h2>This {{ recordData.briefObject.resourceType.toLowerCase() }} has restricted content</h2>
                    <div v-if="allowsFullAuthenticatedAccess" class="actionlink"><a class="button" :href="loginUrl"><i class="fa fa-id-card"></i> {{ $t('access.login') }}</a></div>
                    <div class="actionlink"><a class="button" href="https://library.unc.edu/wilson/contact/"><i class="fa fa-envelope"></i> {{ $t('access.contact') }}</a></div>
                </div>
            </div>
        </div>
        <modal-metadata :title="recordData.briefObject.title"
                        :uuid="recordData.briefObject.id"
                        :open-modal="displayMetadate"
                        @display-metadata="hideMetadata"></modal-metadata>
    </div>
</template>

<script>
import breadCrumbs from '@/components/full_record/breadCrumbs.vue';
import modalMetadata from '@/components/modalMetadata.vue';
import thumbnail from '@/components/full_record/thumbnail.vue';
import fullRecordUtils from '../../mixins/fullRecordUtils';
import {format} from "date-fns";

export default {
    name: 'collectionFolder',

    mixins: [fullRecordUtils],

    components: { breadCrumbs, modalMetadata, thumbnail },

    props: {
        recordData: Object
    },

    methods: {
        fieldExists(value) {
            return value !== undefined;
        },

        formatDate(value) {
            return format(value, 'yyyy-MM-dd');
        }
    }
}
</script>

<style scoped lang="scss">
h2 {
    font-size: 2.5rem;
    line-height: 1;
    margin: 0 20px 20px 25px;
    padding-left: 0;
    font-weight: bold;
    color: #0A5274;
}
</style>