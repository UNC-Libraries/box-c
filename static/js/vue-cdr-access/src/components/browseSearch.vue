<!--
Search form component displayed on full record pages, allowing keyword searches and changing display modes.
-->
<template>
    <div>
        <div class="browse-search field has-addons">
            <div class="control">
                <input @keyup.enter="getResults" class="input" type="text" v-model.trim="search_query"
                       :placeholder="searchText" :aria-label="searchText">
            </div>
            <div class="control">
                <button @click="getResults" class="button">{{ $t('search.search') }}</button>
            </div>
        </div>
        <div class="clear-options">
            <a id="clear-results" class="button is-link is-small" href="#" @click.prevent="clearSearch">
                <span class="icon is-small">
                    <i class="fas fa-times"></i>
                </span> {{ $t('search.clear_search')}}</a>
            <filter-tags :facet-list="facetList"></filter-tags>
        </div>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';
    import filterTags from "./filterTags";

    export default {
        name: 'browseSearch',

        components: {filterTags},

        props: {
            objectType: {
                default: 'object',
                type: String
            },
            facetList: Array
        },

        watch: {
            // Checks for route changes and updates the search box text as needed, keeping it up
            // to date on forward and backward navigation on the same page.
            '$route.query': {
                handler(d) {
                    if ('anywhere' in d) {
                        this.search_query = decodeURIComponent(d.anywhere);
                    } else {
                        this.search_query = '';
                    }
                },
                deep: true
            }
        },

        mixins: [routeUtils],

        data() {
            return {
                search_query: ''
            }
        },

        computed: {
            searchText() {
                const object_type = this.objectType.toLowerCase();
                const object_text = (object_type === 'adminunit') ? 'collection' : object_type;
                return `Search within this ${object_text}`
            }
        },

        methods: {
            getResults() {
                let update_params = { anywhere: encodeURIComponent(this.search_query) };
                this.routeWithParams(this.urlParams(update_params, 'displayRecords'));
            },

            clearSearch() {
                this.routeWithParams(this.removeQueryParameters(['anywhere']));
            }
        },

        mounted() {
            // Updates the search box text from the url parameter on page load
            if ('anywhere' in this.$route.query) {
                this.search_query = decodeURIComponent(this.$route.query.anywhere);
            }
        }
    };
</script>

<style scoped lang="scss">
    .browse-search {
        margin-right: 15px;
        input, div:first-child  {
            width: 100%;
        }
    }

    .clear-options {
        display: flex;
    }

    .clear-results {
        font-size: 16px;
    }

    .button {
        background-color: #F0F0F0;
        margin-top: 10px;
    }

    input, button {
        font-size: 1.1rem;
        height: 50px;
        margin-top: 15px;
    }
</style>