<template>
    <div>
        <div class="search-query-text">
            Search results for "{{ $route.query.anywhere }}"
            <facet-tags :facet-list="facet_list"></facet-tags>
        </div>
        <img v-if="is_loading" src="/static/images/ajax-loader-lg.gif" alt="data loading icon">
        <div v-if="!is_loading">
            <div class="columns">
                <div v-if="hasFacets" class="facet-list column is-one-quarter facets-border border-box-left-top">
                    <facets :facet-list="facet_list"></facets>
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
                <p v-else class="spacing" :class="facetsWithNoResults">No records were found.</p>
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
    import browseSort from "./browseSort";
    import facets from "./facets";
    import facetTags from "./facetTags";
    import listDisplay from "./listDisplay";
    import pagination from "./pagination";
    import routeUtils from "../mixins/routeUtils";
    import get from 'axios';

    export default {
        name: 'searchWrapper',

        components: {browseSort, facets, facetTags, listDisplay, pagination},

        mixins: [routeUtils],

        data() {
            return {
                anywhere: '',
                collection: '',
                facet_list: [],
                is_loading: true,
                records: [],
                total_records: 0
            }
        },

        watch: {
            '$route.query': 'retrieveData'
        },

        computed: {
            recordDisplayCounts() {
                let search_start = parseInt(this.$route.query['a.setStartRow']);
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
                let param_string = `${this.formatParamsString(this.$route.query)}&getFacets=true`;

                get(`searchJson/${param_string}`).then((response) => {
                    this.records = response.data.metadata;
                    this.total_records = response.data.resultCount;
                    this.facet_list = response.data.facetFields;
                    this.is_loading = false;
                }).catch(function (error) {
                    console.log(error);
                });
            }
        },

        created() {
            this.retrieveData();
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
</style>