<template>
    <div v-if="hasLoaded" class="browse-header">
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
                    <thumbnail :brief-object="recordData.briefObject"></thumbnail>
                    {{ recordData.briefObject.title }}
                </h2>
                <p>
                    <strong>Subjects:</strong>
                    <template v-if="hasSubjects">
                        {{ recordData.briefObject.subject.join(', ')}}
                    </template>
                    <template v-else>
                        {{ $t('full_record.no_subjects') }}
                    </template>
                </p>
                <template v-if="recordData.briefObject.abstractText">
                    <template v-if="truncateAbstract">
                        <p>{{ truncatedAbstractText }}... <a class="abstract-text" @click.prevent="abstractDisplay()"
                                                             href="#">{{ abstractLinkText }}</a></p>
                    </template>
                </template>
                <p><a @click.prevent="metadataDisplay()" href="#">{{ $t('full_record.additional_metadata') }}</a></p>
            </div>
            <modal-metadata :title="recordData.briefObject.title"
                            :uuid="recordData.briefObject.id"
                            :open-modal="displayMetadate"
                            @display-metadata="hideMetadata"></modal-metadata>
        </div>

    </div>
</template>

<script>
import breadCrumbs from '@/components/full_record/breadCrumbs.vue';
import modalMetadata from '@/components/modalMetadata.vue';
import thumbnail from '@/components/full_record/thumbnail.vue';
import get from 'axios';

export default {
    name: 'adminUnit',

    components: { breadCrumbs, modalMetadata, thumbnail },

    data() {
        return {
            displayMetadate: false,
            hasLoaded: false,
            recordData: {},
            showFullAbstract: false
        }
    },

    computed: {
        abstractLinkText() {
            return this.showFullAbstract ? this.$t('full_record.read_less'): this.$t('full_record.read_more');
        },

        truncateAbstract() {
            return this.recordData.briefObject.abstractText !== undefined &&
                this.recordData.briefObject.abstractText.length > 350;
        },

        truncatedAbstractText() {
            if (this.truncateAbstract && !this.showFullAbstract) {
                return this.recordData.briefObject.abstractText.substring(0, 350);
            }

            return this.recordData.briefObject.abstractText;
        },

        isDeleted() {
            if (this.recordData.markedForDeletion) {
                return 'deleted';
            }
            return '';
        },

        hasSubjects() {
            return this.recordData.briefObject.subject !== undefined && this.recordData.briefObject.subject.length > 0
        }
    },

    methods: {
        getBriefObject() {
            get(`${window.location.pathname}json`).then((response) => {
                this.recordData = response.data;
                this.hasLoaded = true;
            }).catch(error => console.log(error));
        },

        abstractDisplay() {
            this.showFullAbstract = !this.showFullAbstract;
        },

        metadataDisplay() {
            this.displayMetadate = true;
        },

        hideMetadata(show) {
            this.displayMetadate = show;
        }
    },

    created() {
        this.getBriefObject();
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