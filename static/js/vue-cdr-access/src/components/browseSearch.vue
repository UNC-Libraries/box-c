<template>
    <div>
        <div class="browse-search field has-addons">
            <div class="control">
                <input @keyup.enter="getResults" class="input" type="text" v-model.trim="search_query"
                       :placeholder="searchText" :aria-label="searchText">
            </div>
            <div class="control">
                <button @click="getResults" class="button">Search</button>
            </div>
        </div>
        <a class="clear-results" href="#" @click.prevent="clearSearch">Clear search results</a>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';

    export default {
        name: 'browseSearch',

        props: {
            objectType: {
                default: 'object',
                type: String
            }
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
                this.$router.push({ name: 'displayRecords', query: this.urlParams(update_params) })
                    .catch((e) => {
                        if (this.nonDuplicateNavigationError(e)) {
                            throw e;
                        }
                    });
            },

            clearSearch() {
                this.search_query = '';
                this.getResults();
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

    .clear-results {
        font-size: 16px;
    }

    .button {
        background-color: #F0F0F0;
    }

    input, button {
        font-size: 1.1rem;
        height: 50px;
        margin-top: 15px;
    }
</style>