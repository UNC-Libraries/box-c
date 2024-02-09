<template>
    <div class="content-wrap full_record">
        <div class="columns browse-top">
            <div class="column" :class="{restrictedContent: 'is-8'}">
                <thumbnail :thumbnail-data="recordData"></thumbnail>
                <h2 :class="isDeleted">
                    {{ recordData.briefObject.title }}
                    <span class="item_container_count">{{ displayChildCount }}</span>
                </h2>
                <p v-if="fieldExists(recordData.briefObject.added)">
                    <strong>{{ $t('full_record.date_added') }}: </strong>
                    {{ formatDate(recordData.briefObject.added) }}
                </p>
                <p class="parent_collection" v-if="fieldExists(recordData.briefObject.parentCollectionName)">
                    <strong>{{ $t('full_record.collection') }}: </strong> <a :href="collectionLink">{{ recordData.briefObject.parentCollectionName }}</a>
                </p>
                <p v-if="fieldExists(recordData.findingAidUrl)">
                    <strong>{{ $t('full_record.finding_aid') }}: </strong>
                    <a :href="recordData.findingAidUrl">{{ recordData.findingAidUrl }}</a>
                </p>
                <p class="exhibits" v-if="fieldExists(recordData.exhibits)">
                    <strong>{{ $t('full_record.related_digital_exhibits') }}: </strong>
                    <template v-for="(exhibit_link, title, index) in recordData.exhibits">
                        <a :href="exhibit_link">{{ title }}</a>
                        <template v-if="hasMoreExhibits(index, recordData.exhibits)">; </template>
                    </template>
                </p>
                <abstract v-if="recordData.briefObject.abstractText" :brief-object="recordData.briefObject"/>
                <p><a @click.prevent="displayMetadata()" href="#">{{ $t('full_record.additional_metadata') }}</a></p>
            </div>
            <restricted-content :record-data="recordData"></restricted-content>
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
import restrictedContent from '@/components/full_record/restrictedContent.vue';

export default {
    name: 'folderRecord',

    mixins: [fullRecordUtils],

    components: { abstract, restrictedContent },

    computed: {
        collectionLink() {
            return `record/${this.recordData.briefObject.parentCollectionId}`;
        }
    }
}
</script>

<style scoped lang="scss">
.actionlink {
    margin: 5px auto;
    max-width: 300px;
}

.browse-header {
    h2 {
        font-size: 1.5rem;
    }
}

.restricted-access {
    h2 {
        text-align: center;
    }
}
</style>