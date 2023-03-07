<template>
    <div class="browse-header">
        <div class="columns">
            <div class="column">
                <bread-crumbs :object-path="recordData.briefObject.objectPath">
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
                        <p v-if="truncateAbstract" class="abstract">{{ truncatedAbstractText }}... <a class="abstract-text" @click.prevent="toggleAbstractDisplay()"
                                                             href="#">{{ abstractLinkText }}</a></p>
                        <p v-else class="abstract">{{ recordData.briefObject.abstractText }}</p>
                </template>
                <p><a @click.prevent="displayMetadata()" class="metadata-link" href="#">{{ $t('full_record.additional_metadata') }}</a></p>
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
