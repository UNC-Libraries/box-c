<!--
Search form component displayed on full record pages, allowing keyword searches and changing display modes.
-->
<template>
    <div>
        <div class="browse-search field has-addons">
            <div class="control">
                <input @keyup.enter="getResults" class="input" type="text" v-model.trim="search_query"
                       :placeholder="searchText" :aria-label="searchText">
            </div>
            <div class="control">
                <button @click="getResults" class="button">{{ $t('search.search') }}</button>
            </div>
        </div>
        <div class="clear-options">
            <a id="clear-results" class="button is-link is-small" v-bind:class="{ 'disabled' : !this.enableStartOverButton}"
                    href="#" @click.prevent="clearSearch">
                <span class="icon is-small">
                    <i class="fas fa-times"></i>
                </span> {{ $t('search.clear_search')}}</a>
            <clear-facets-button></clear-facets-button>
            <filter-tags :filter-parameters="filterParameters"></filter-tags>
        </div>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';
    import filterTags from "@/components/filterTags.vue";
    import clearFacetsButton from "@/components/clearFacetsButton.vue";

    export default {
        name: 'browseSearch',

        components: {filterTags, clearFacetsButton},

        props: {
            objectType: {
                default: 'object',
                type: String
            },
            filterParameters: Object
        },

        watch: {
            // Checks for route changes and updates the search box text as needed, keeping it up
            // to date on forward and backward navigation on the same page.
            '$route.query': {
                handler(d) {
                    if ('anywhere' in d) {
                        this.search_query = decodeURIComponent(d.anywhere);
                    } else {
                        this.search_query = '';
                    }
                },
                deep: true
            }
        },

        mixins: [routeUtils],

        data() {
            return {
                search_query: ''
            }
        },

        computed: {
            searchText() {
                const object_type = this.objectType.toLowerCase();
                const object_text = (object_type === 'adminunit') ? 'collection' : object_type;
                return `Search within this ${object_text}`
            },
            enableStartOverButton() {
                return this.anyParamsPopulated(this.allPossibleSearchParameters);
            }
        },

        methods: {
            getResults() {
                const reset_search_start_row = this.resetStartRow({ anywhere: encodeURIComponent(this.search_query) });
                const update_parameters = this.urlParams(reset_search_start_row, 'displayRecords');
                this.routeWithParams(update_parameters);
            },

            clearSearch() {
                this.clear(this.allPossibleSearchParameters);
            },

            clearAllFacets() {
                this.clear(this.possibleFacetFields);
            },

            /**
             * Clears facets/searches
             * @param param_to_clear
             */
            clear(param_to_clear) {
                const reset_params = this.removeQueryParameters(param_to_clear);
                const reset_start_row = this.resetStartRow(reset_params);
                this.routeWithParams(reset_start_row);
            }
        },

        mounted() {
            // Updates the search box text from the url parameter on page load
            if ('anywhere' in this.$route.query) {
                this.search_query = decodeURIComponent(this.$route.query.anywhere);
            }
        }
    };
</script>

<style scoped lang="scss">
    .browse-search {
        margin-right: 15px;
        input, div:first-child  {
            width: 100%;
        }
    }

    .clear-options {
        display: flex;
    }

    .clear-results {
        font-size: 16px;
    }

    .button {
        margin-right: 6px;
    }

    input, button {
        font-size: 1.1rem;
        height: 50px;
        margin-top: 15px;
    }
</style>