<!--
Button for clearing all currently active facets limiting the search.
-->
<template>
    <a v-if="showButton" class="clear-all-facets button is-primary" @click.prevent="clearAllFacets()"><span>{{ $t('facets.clear')}}</span>
        <span class="icon"><i class="fas fa-times"></i></span>
    </a>
</template>
<script>
import routeUtils from '../mixins/routeUtils';

export default {
    name: 'clearFacetsButton',

    mixins: [routeUtils],

    computed: {
        showButton() {
            return this.anyParamsPopulated(this.possibleFacetFields);
        }
    },
    methods: {
        clearAllFacets() {
            const remove_params = this.removeQueryParameters(this.possibleFacetFields);
            const reset_start_row = this.resetStartRow(remove_params);
            this.routeWithParams(reset_start_row);
        }
    }
};
</script>