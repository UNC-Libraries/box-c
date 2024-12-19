<template>
    <div class="full_record">
        <div class="columns is-6-desktop browse-top container is-mobile is-1-mobile">
            <div class="column is-narrow" :class="{restrictedContent: 'is-8'}">
                <thumbnail :thumbnail-data="recordData"></thumbnail>
            </div>
            <div class="column content pb-4">
                <h2 :class="isDeleted" class="title is-3 is-text-unc-blue">
                    {{ recordData.briefObject.title }}
                    <span class="tag is-large is-primary">{{ displayChildCount }}</span>
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
                </dl>
                <div class="my-1">
                    <abstract v-if="recordData.briefObject.abstractText" :brief-object="recordData.briefObject"/>
                </div>
                <p><a @click.prevent="displayMetadata()" href="#">{{ $t('full_record.additional_metadata') }}</a></p>
            </div>
            <object-actions :record-data="recordData"></object-actions>
        </div>
        <modal-metadata :title="recordData.briefObject.title"
                        :uuid="recordData.briefObject.id"
                        :open-modal="showMetadata"
                        @display-metadata="toggleMetadata"></modal-metadata>
    </div>
</template>

<script>
import fullRecordUtils from '../../mixins/fullRecordUtils';
import abstract from '@/components/full_record/abstract.vue';
import objectActions from '@/components/full_record/objectActions.vue';

export default {
    name: 'folderRecord',

    mixins: [fullRecordUtils],

    components: { abstract, objectActions }
}
</script>