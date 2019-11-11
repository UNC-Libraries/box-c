<template>
    <div>
        <div class="contentarea search-query-text">

        </div>
        <img v-if="is_loading" src="/static/images/ajax-loader-lg.gif" alt="data loading icon">
        <div v-if="records.length > 0" class="columns">
            <div class="column is-one-quarter facets-border border-box-left-top">

            </div>
            <div class="column is-three-quarters search-results-border border-box-left-top">
                <div class="bottomline paddedline">
                    <p>
                        Showing <span class="has-text-weight-bold">{{ recordDisplayCounts }}</span> of
                        <span class="has-text-weight-bold">{{ records.length }}</span> results found
                    </p>
                    <browse-sort browse-type="search"></browse-sort>
                </div>
                <list-display v-if="records.length > 0" :record-list="records"></list-display>
            </div>
        </div>
        <p v-else class="spacing">No records were found.</p>
        <div class="columns is-mobile">
            <div class="column is-12 search-pagination-bottom">
                <pagination browse-type="search" :number-of-records="records.length"></pagination>
            </div>
        </div>
    </div>
</template>

<script>
    import ListDisplay from "./listDisplay";
    import Pagination from "./pagination";
    import routeUtils from "../mixins/routeUtils";
    import get from 'axios';
    import BrowseSort from "./browseSort";

    export default {
        name: 'searchWrapper',

        components: {BrowseSort, ListDisplay, Pagination},

        mixins: [routeUtils],

        data() {
            return {
                anywhere: '',
                is_loading: true,
                records: [],
            }
        },

        watch: {
            '$route.query': 'retrieveData'
        },

        computed: {
            recordDisplayCounts() {
                let search_start = parseInt(this.$route.query['a.setStartRow']);
                let start = (search_start > 0) ? search_start : 1;
                let finish = (search_start > 0) ? search_start : 0;

                return `${start}-${parseInt(this.rows_per_page) + finish}`;
            }
        },

        methods: {
            retrieveData() {
                let param_string = this.formatParamsString(this.$route.query);

                get(`searchJson/${param_string}`).then((response) => {
                    this.records = response.data.metadata;
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
    img {
        display: block;
        margin: 25px auto;
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
    }
</style>