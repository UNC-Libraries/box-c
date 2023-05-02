<!--
Top level component wrapper for search pages
-->
<template>
    <header-small/>
    <div>
        <div class="search-query-text">
            Search results for "{{ $route.query.anywhere }}"
        </div>
        <clear-filters :filter-parameters="filter_parameters"></clear-filters>
        <img v-if="is_loading" :src="nonVueStaticImageUrl('ajax-loader-lg.gif')" alt="data loading icon">
        <div v-if="!is_loading">
            <div class="columns">
                <div v-if="hasFacets" class="facet-list column is-one-quarter facets-border border-box-left-top">
                    <facets :facet-list="facet_list" :min-created-year="minimumCreatedYear"></facets>
                </div>

                <div v-if="records.length > 0" class="column is-three-quarters search-results-border border-box-left-top">
                    <div class="bottomline paddedline">
                        <p>
                            Showing <span class="has-text-weight-bold">{{ recordDisplayCounts }}</span> of
                            <span class="has-text-weight-bold">{{ total_records }}</span> results found
                        </p>
                        <browse-sort browse-type="search"></browse-sort>
                    </div>
                    <list-display v-if="records.length > 0" :record-list="records" :use-saved-browse-type="true"></list-display>
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
</template>

<script>
    import browseSort from "@/components/browseSort.vue";
    import clearFilters from "@/components/clearFilters.vue";
    import facets from "@/components/facets.vue";
    import headerSmall from "@/components/header/headerSmall.vue";
    import listDisplay from "@/components/listDisplay.vue";
    import pagination from "@/components/pagination.vue";
    import analyticsUtils from '../mixins/analyticsUtils';
    import imageUtils from "../mixins/imageUtils";
    import routeUtils from "../mixins/routeUtils";
    import get from 'axios';
    import cloneDeep from 'lodash.clonedeep';

    export default {
        name: 'searchWrapper',

        components: {browseSort, clearFilters, facets, headerSmall, listDisplay, pagination},

        mixins: [analyticsUtils, imageUtils, routeUtils],

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
            retrieveData() {
                let query = cloneDeep(this.$route.query);
                let param_string = `${this.formatParamsString(query)}&getFacets=true`;
                let search_path = 'searchJson';
                this.collection = this.routeHasPathId ? this.$route.path.split('/')[2] : '';

                get(`${search_path}/${param_string}`).then((response) => {
                    this.records = response.data.metadata;
                    this.total_records = response.data.resultCount;
                    this.facet_list = response.data.facetFields;
                    this.filter_parameters = response.data.filterParameters;
                    this.min_created_year = response.data.minSearchYear;
                    this.is_loading = false;
                }).catch(function (error) {
                    console.log(error);
                });
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
    $light-gray: #E1E1E1;
    $border-style: 1px solid $light-gray;

    img {
        display: block;
        margin: 25px auto;
    }

    .facets-border {
        border-bottom: $border-style;
        border-top: $border-style;
        font-size: 16px;
        padding-bottom: 0;
        padding-right: 0;
        padding-top: 0;
    }

    .bottomline {
        float: none;
    }

    li.columns {
        margin-left: inherit;
        margin-right: inherit;
    }

    p {
        margin: auto 0;
        width: 100%;
    }

    @media screen and (max-width: 1024px) {
        .bottomline {
            display: inline-flex;
        }
    }

    @media screen and (max-width: 768px) {
        .bottomline {
            display: inline-block;
            text-align: center;
        }
    }
</style>