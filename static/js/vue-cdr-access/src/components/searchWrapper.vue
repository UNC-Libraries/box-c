<template>
    <div>
        <div class="contentarea search-query-text"></div>
        <div class="columns">
            <div class="column is-one-quarter facets-border border-box-left-top">

            </div>
            <div class="column is-three-quarters search-results-border border-box-left-top">
                <img v-if="is_loading" src="/static/images/ajax-loader-lg.gif" alt="data loading icon">
                <list-display :record-list="records"></list-display>
            </div>
        </div>
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

    export default {
        name: 'searchWrapper',

        components: {ListDisplay, Pagination},

        mixins: [routeUtils],

        data() {
            return {
                anywhere: '',
                is_loading: true,
                records: [],
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
</style>