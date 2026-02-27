<!--
Top level component wrapper for search pages
-->
<template>
    <header-small/>
    <div v-if="!show_404 && !show_503" class="container pt-5">
        <div>
            <h2 class="subtitle is-5">Search results for "{{ $route.query.anywhere }}"</h2>
            <clear-filters :filter-parameters="filter_parameters"></clear-filters>
        </div>
        <img v-if="is_loading" :src="nonVueStaticImageUrl('ajax-loader-lg.gif')" alt="data loading icon">
        <div v-if="!is_loading">
            <div class="columns">
                <div v-if="hasFacets" class="facet-list column is-one-quarter pt-5">
                    <facets :facet-list="facet_list" :min-created-year="minimumCreatedYear"></facets>
                </div>

                <div v-if="records.length > 0" class="column is-three-quarters">
                    <div class="columns is-vcentered">
                        <h2 class="column subtitle m-0">
                            Showing <span class="has-text-weight-bold">{{ recordDisplayCounts }}</span> of
                            <span class="has-text-weight-bold">{{ total_records }}</span> results found
                        </h2>
                        <div class="column is-narrow">
                            <browse-sort browse-type="search"></browse-sort>
                        </div>
                    </div>
                    <list-display v-if="records.length > 0" :record-list="records" :exclude-browse-type-from-record-urls="true"></list-display>
                </div>
                <p v-else class="spacing" :class="facetsWithNoResults">{{ $t('search.no_results') }}</p>
            </div>

            <div class="columns is-mobile">
                <div class="column is-12 search-pagination-bottom">
                    <pagination browse-type="search" :number-of-records="total_records"></pagination>
                </div>
            </div>
        </div>
    </div>
    <not-found v-if="show_404" :display-header="false"></not-found>
    <not-available v-if="show_503"></not-available>
</template>

<script>
    import browseSort from "@/components/browseSort.vue";
    import clearFilters from "@/components/clearFilters.vue";
    import facets from "@/components/facets.vue";
    import headerSmall from "@/components/header/headerSmall.vue";
    import listDisplay from "@/components/listDisplay.vue";
    import notAvailable from "@/components/error_pages/notAvailable.vue";
    import notFound from "@/components/error_pages/notFound.vue";
    import pagination from "@/components/pagination.vue";
    import analyticsUtils from '../mixins/analyticsUtils';
    import errorUtils from "../mixins/errorUtils";
    import imageUtils from "../mixins/imageUtils";
    import routeUtils from "../mixins/routeUtils";
    import cloneDeep from 'lodash.clonedeep';

    export default {
        name: 'searchWrapper',

        components: {
            browseSort,
            clearFilters,
            facets,
            headerSmall,
            listDisplay,
            notAvailable,
            notFound,
            pagination
        },

        mixins: [analyticsUtils, errorUtils, imageUtils, routeUtils],

        data() {
            return {
                anywhere: '',
                collection: '',
                facet_list: [],
                filter_parameters: {},
                is_loading: true,
                records: [],
                total_records: 0
            }
        },

        head() {
            return {
                title: 'Search Results'
            }
        },

        watch: {
            '$route.query': {
                handler() {
                    this.retrieveData();
                },
                deep: true
            }
        },

        computed: {
            recordDisplayCounts() {
                let search_start = parseInt(this.$route.query.start);
                let start = (search_start > 0) ? search_start + 1 : 1;
                let records = (this.records.length < this.rows_per_page) ? this.records.length : parseInt(this.rows_per_page);
                let offset = (search_start > 0) ? search_start : 0;

                return `${start}-${parseInt(records) + offset}`;
            },

            hasFacets() {
                return this.facet_list.map(f => f.values).flat().length > 0;
            },

            facetsWithNoResults() {
                if (this.hasFacets) {
                    return ['column', 'is-three-quarters'];
                }
                return [];
            }
        },

        methods: {
            async retrieveData() {
                let query = cloneDeep(this.$route.query);
                let param_string = `${this.formatParamsString(query)}&getFacets=true`;
                let search_path = 'searchJson';
                this.collection = this.routeHasPathId ? this.$route.path.split('/')[2] : '';

                try {
                    const response = await fetch(`api/${search_path}${param_string}`);
                    if (!response.ok) {
                        const error = new Error('Network response was not ok');
                        error.response = response;
                        throw error;
                    }

                    const data = await response.json();
                    this.emptyJsonResponseCheck(data);
                    this.records = data.metadata;
                    this.total_records = data.resultCount;
                    this.facet_list = data.facetFields;
                    this.filter_parameters = data.filterParameters;
                    this.min_created_year = data.minSearchYear;
                    this.is_loading = false;
                } catch (error) {
                    this.setErrorResponse(error);
                    this.is_loading = false;
                    console.log(error);
                }
            }
        },

        created() {
            this.retrieveData();
        },

        mounted() {
            this.pageView('Search Results');
        }
    }
</script>

<style scoped lang="scss">
    img {
        display: block;
        margin: 25px auto;
    }

    li.columns {
        margin-left: inherit;
        margin-right: inherit;
    }
</style>