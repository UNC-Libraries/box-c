<template>
    <div class="full_record pb-4">
        <div class="columns is-6-desktop browse-top container is-mobile is-1-mobile">
            <div class="column is-narrow">
                <thumbnail :thumbnail-data="recordData"></thumbnail>
            </div>
            <div class="column content is-medium">
                <h2 :class="isDeleted" class="title is-2 is-text-unc-blue">
                    {{ recordData.briefObject.title }}
                </h2>
                <dl class="property-grid">
                    <dt>{{ $t('full_record.subjects') }}</dt>
                    <dd v-if="hasSubjects">
                        {{ recordData.briefObject.subject.join(', ')}}
                    </dd>
                    <dd v-else>
                        {{ $t('full_record.no_subjects') }}
                    </dd>
                </dl>
                <abstract v-if="recordData.briefObject.abstractText" :brief-object="recordData.briefObject"/>
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
import abstract from "@/components/full_record/abstract.vue";

export default {
    name: 'adminUnit',

    mixins: [fullRecordUtils],

    components: {
        abstract
    },

    computed: {
        hasSubjects() {
            return this.recordData.briefObject.subject !== undefined && this.recordData.briefObject.subject.length > 0
        }
    }
}
</script>
