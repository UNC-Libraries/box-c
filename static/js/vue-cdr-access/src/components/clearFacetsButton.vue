<!--
Button for clearing all currently active facets limiting the search.
-->
<template>
    <a v-if="showButton" class="clear-all-facets button is-link is-small" @click.prevent="clearAllFacets()">{{ $t('facets.clear')}}
        <i class="fas fa-times"></i>
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

<style scoped  lang="scss">
    a {
        margin-left: 10px;
    }
    i {
        padding-left: 5px;
    }
</style>