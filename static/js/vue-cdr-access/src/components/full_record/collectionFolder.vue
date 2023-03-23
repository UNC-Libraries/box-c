<template>
    <div class="browse-header">
        <div class="columns">
            <div class="column">
                <bread-crumbs :object-path="recordData.briefObject.objectPath">
                </bread-crumbs>
            </div>
        </div>
        <div class="columns">
            <div class="column" :class="{restrictedContent: 'is-8'}">
                <h2 :class="isDeleted">
                    <thumbnail :thumbnail-data="recordData"
                               :allows-full-access="allowsFullAuthenticatedAccess"></thumbnail>
                    {{ recordData.briefObject.title }}
                    <span class="item_container_count">{{ displayChildCount }}</span>
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
                <abstract v-if="recordData.briefObject.abstractText"/>
                <p v-if="fieldExists(recordData.exhibits)">
                    <strong>{{ $t('full_record.related_digital_exhibits') }}: </strong>
                    <template v-for="(exhibit, index) in recordData.exhibits">
                        <a :href="exhibit.value">{{ exhibit.key }}</a><template v-if="index < recordData.exhibits.length - 1">;</template>
                    </template>
                </p>
                <p><a @click.prevent="displayMetadata()" href="#">{{ $t('full_record.additional_metadata') }}</a></p>
            </div>
            <div v-if="restrictedContent" class="column is-narrow-desktop item-actions">
                <div class="restricted-access">
                    <h2>This {{ recordData.briefObject.resourceType.toLowerCase() }} has restricted content</h2>
                    <div v-if="allowsFullAuthenticatedAccess" class="actionlink"><a class="button login-link" :href="loginUrl"><i class="fa fa-id-card"></i> {{ $t('access.login') }}</a></div>
                    <div class="actionlink"><a class="button contact" href="https://library.unc.edu/wilson/contact/"><i class="fa fa-envelope"></i> {{ $t('access.contact') }}</a></div>
                </div>
            </div>
        </div>
        <modal-metadata :title="recordData.briefObject.title"
                        :uuid="recordData.briefObject.id"
                        :open-modal="showMetadata"
                        @display-metadata="toggleMetadata"></modal-metadata>
    </div>
</template>

<script>
import fullRecordUtils from '../../mixins/fullRecordUtils';
import {format} from 'date-fns';
import abstract from "../full_record/abstract.vue";

export default {
    name: 'collectionFolder',

    mixins: [fullRecordUtils],

    components: {
        abstract
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