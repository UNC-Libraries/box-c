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
            <div class="column">
                <h2 :class="isDeleted">
                    <thumbnail :thumbnail-data="recordData"
                               :allows-full-access="allowsFullAuthenticatedAccess"></thumbnail>
                    {{ recordData.briefObject.title }}
                </h2>
                <p>
                    <strong>{{ $t('full_record.subjects') }}:</strong>
                    <template v-if="hasSubjects">
                        {{ recordData.briefObject.subject.join(', ')}}
                    </template>
                    <template v-else>
                        {{ $t('full_record.no_subjects') }}
                    </template>
                </p>
                <template v-if="recordData.briefObject.abstractText">
                        <p v-if="truncateAbstract" class="abstract">{{ truncatedAbstractText }}... <a class="abstract-text" @click.prevent="abstractDisplay()"
                                                             href="#">{{ abstractLinkText }}</a></p>
                        <p v-else class="abstract">{{ recordData.briefObject.abstractText }}</p>
                </template>
                <p><a @click.prevent="metadataDisplay()" class="metadata-link" href="#">{{ $t('full_record.additional_metadata') }}</a></p>
            </div>
        </div>
        <modal-metadata :title="recordData.briefObject.title"
                        :uuid="recordData.briefObject.id"
                        :open-modal="displayMetadate"
                        @display-metadata="hideMetadata"></modal-metadata>
    </div>
</template>

<script>
import fullRecordUtils from '../../mixins/fullRecordUtils';

export default {
    name: 'adminUnit',

    mixins: [fullRecordUtils],

    computed: {
        hasSubjects() {
            return this.recordData.briefObject.subject !== undefined && this.recordData.briefObject.subject.length > 0
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