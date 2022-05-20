<template>
    <div class="clear-options">
        <a id="clear-results" class="button is-link is-small" v-bind:class="{ 'disabled' : !this.enableStartOverButton}"
           href="#" @click.prevent="clearSearch">
                {{ $t('search.clear_search')}} <i class="fas fa-times"></i></a>
        <clear-facets-button></clear-facets-button>
        <filter-tags :filter-parameters="filterParameters"></filter-tags>
    </div>
</template>

<script>
import clearFacetsButton from '@/components/clearFacetsButton.vue';
import filterTags from '@/components/filterTags.vue';
import routeUtils from '../mixins/routeUtils';

export default {
    name: "clearFilters",

    components: {
        clearFacetsButton,
        filterTags
    },

    mixins: [routeUtils],

    props: {
        filterParameters: Object
    },

    computed: {
        enableStartOverButton() {
            return this.anyParamsPopulated(this.allPossibleSearchParameters);
        }
    },

    methods: {
        clearSearch() {
            this.clearParameters(this.allPossibleSearchParameters);
        },

        clearAllFacets() {
            this.clearParameters(this.possibleFacetFields);
        },

        /**
         * Clears facets/searches
         * @param param_to_clear
         */
        clearParameters(param_to_clear) {
            const reset_params = this.removeQueryParameters(param_to_clear);
            const reset_start_row = this.resetStartRow(reset_params);
            this.routeWithParams(reset_start_row);
        }
    }
}
</script>

<style scoped  lang="scss">
    i {
        padding-left: 10px;
    }
    .clear-options {
        display: inline-flex;
        flex-wrap: wrap;
        margin-bottom: 10px;
        margin-left: 35px;
        margin-top: -20px
    }
</style>