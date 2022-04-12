<!--
Button for clearing all currently active facets limiting the search.
-->
<template>
    <a v-if="showButton" class="clear-all-facets button is-link is-small" @click.prevent="clearAllFacets()">
            <span class="icon is-small">
                <i class="fas fa-times"></i>
            </span> {{ $t('facets.clear')}}</a>
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
            this.routeWithParams(this.removeQueryParameters(this.possibleFacetFields));
        }
    }
};
</script>