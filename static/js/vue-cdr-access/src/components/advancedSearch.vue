<template>
    <header-small/>
    <div class="container py-5">
        <h2 class="title is-3 has-text-centered">{{ $t('adv_search.advanced') }}</h2>
        <form id="advanced-search-form" action="/api/advancedSearch" method="get">
            <div class="lightest columns container">
                <div class="column has-background-white-bis p-4">
                    <h3 class="subtitle is-5">{{ $t('adv_search.search_for') }}</h3>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal">
                            <label class="label" for="anywhere">{{ $t('adv_search.anywhere') }}</label>
                        </div>
                        <div class="field-body">
                            <div class="field">
                                <p class="control">
                                    <input id="anywhere" name="anywhere" class="input" type="text" />
                                </p>
                            </div>
                        </div>
                    </div>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal">
                            <label class="label" for="title">{{ $t('adv_search.title') }}</label>
                        </div>
                        <div class="field-body">
                            <div class="field">
                                <p class="control">
                                    <input id="title" name="titleIndex" class="input" type="text" />
                                </p>
                            </div>
                        </div>
                    </div>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal break">
                            <label class="label" for="contributor">{{ $t('adv_search.creator_contributor') }}</label>
                        </div>
                        <div class="field-body">
                            <div class="field">
                                <p class="control">
                                    <input id="contributor" name="contributorIndex" class="input" type="text" />
                                </p>
                            </div>
                        </div>
                    </div>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal">
                            <label class="label" for="subject">{{ $t('adv_search.subject') }}</label>
                        </div>
                        <div class="field-body">
                            <div class="field">
                                <p class="control">
                                    <input id="subject" name="subjectIndex" class="input" type="text" />
                                </p>
                            </div>
                        </div>
                    </div>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal">
                            <label class="label" for="subject">{{ $t('adv_search.collection_id') }}</label>
                        </div>
                        <div class="field-body">
                            <div class="field">
                                <p class="control">
                                    <input id="collection_id" name="collectionId" class="input" type="text" />
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="column has-background-white-ter p-4">
                    <h3 class="subtitle is-5">{{ $t('adv_search.limit_by') }}</h3>
                    <div class="field">
                        <div class="field">
                            <div class="control">
                                <div class="select is-fullwidth">
                                    <select name="collection" aria-label="Collection">
                                        <option value="">Collection</option>
                                        <template v-for="collection in collections">
                                            <option :value="collection.id">{{ collection.title }}</option>
                                        </template>
                                    </select>
                                </div>
                            </div>
                        </div>
                        <div class="field">
                            <div class="control">
                                <div class="select is-fullwidth">
                                    <select name="format" aria-label="Format">
                                        <option value="">Format</option>
                                        <template v-for="format in formats">
                                            <option :value="format">{{ format }}</option>
                                        </template>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </div>
                    <h3 class="title is-6 mb-2 mt-5">
                        {{ $t('adv_search.date_deposited') }}
                        <span class="has-tooltip-arrow has-tooltip-multiline has-tooltip-right-mobile icon" :data-tooltip="$t('adv_search.dates_end_note')">
                            <i class="fas fa-question-circle fa-lg"></i>
                        </span>
                    </h3>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal">
                            <label class="label" for="addedStart">{{ $t('adv_search.date_from') }}</label>
                        </div>
                        <div class="field">
                            <div class="control">
                                <input aria-label="deposited-start-date" id="addedStart" name="addedStart" placeholder="YYYY" min="0" type="number" class="input" />
                            </div>
                        </div>
                        <div class="field-label is-normal mx-4">
                            <label class="label" for="addedEnd">{{ $t('adv_search.date_to') }}</label>
                        </div>
                        <div class="field">
                            <div class="control">
                                <input aria-label="deposited-end-date" id="addedEnd" name="addedEnd" placeholder="YYYY" min="1" type="number" class="input" />
                            </div>
                        </div>
                    </div>
                    <h3 class="title is-6 mb-2 mt-4">
                        {{ $t('adv_search.date_created') }}
                        <span class="has-tooltip-arrow has-tooltip-multiline has-tooltip-right-mobile icon" :data-tooltip="$t('adv_search.dates_end_note')">
                            <i class="fas fa-question-circle fa-lg"></i>
                        </span>
                    </h3>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal">
                            <label class="label" for="createdYearStart">{{ $t('adv_search.date_from') }}</label>
                        </div>
                        <div class="field">
                            <div class="control">
                                <input aria-label="created-start-date" placeholder="YYYY" min="0" id="createdYearStart" name="createdYearStart" type="number" class="input" />
                            </div>
                        </div>
                        <div class="field-label is-normal mx-4">
                            <label class="label" for="createdYearEnd">{{ $t('adv_search.date_to') }}</label>
                        </div>
                        <div class="field">
                            <div class="control">
                                <input aria-label="created-end-date" placeholder="YYYY" min="1" id="createdYearEnd" name="createdYearEnd" type="number" class="input" />
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="columns">
                <div class="column has-text-right">
                    <input type="submit" class="button is-primary" id="advsearch_submit" :value="$t('adv_search.search')" />
                </div>
            </div>
        </form>
    </div>
</template>

<script>
import headerSmall from '@/components/header/headerSmall.vue';
import analyticsUtils from '../mixins/analyticsUtils';

export default {
    name: "advancedSearch",

    components: {headerSmall},

    mixins: [analyticsUtils],

    data() {
        return {
            collections: [],
            formats: [],
        }
    },

    head() {
        return {
            title: 'Advanced Search'
        }
    },

    methods: {
        async getCollections() {
            try {
                const response = await fetch('/api/advancedSearch/collectionsJson');
                if (!response.ok) {
                    const error = new Error('Network response was not ok');
                    error.response = response;
                    throw error;
                }
                this.collections = await response.json();
            } catch (error) {
                console.log(error);
            }
        },

        async getFormats() {
            try {
                const response = await fetch('/api/advancedSearch/formats');
                if (!response.ok) {
                    const error = new Error('Network response was not ok');
                    error.response = response;
                    throw error;
                }
                this.formats = await response.json();
            } catch (error) {
                console.log(error);
            }
        },
    },

    mounted() {
        this.getCollections();
        this.getFormats();
        this.pageView('Advanced Search');
    }
}
</script>