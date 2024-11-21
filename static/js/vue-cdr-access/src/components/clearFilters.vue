<template>
    <div class="clear-options field is-grouped">
        <a id="clear-results" class="button is-primary" v-bind:class="{ 'disabled' : !this.enableStartOverButton}"
           href="#" @click.prevent="clearSearch">
            <span>{{ $t('search.clear_search')}}</span><span class="icon"><i class="fas fa-times"></i></span></a>
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
    .clear-options {
        display: inline-flex;
        flex-wrap: wrap;
        margin-bottom: 30px;
        margin-left: 25px;
    }
</style>