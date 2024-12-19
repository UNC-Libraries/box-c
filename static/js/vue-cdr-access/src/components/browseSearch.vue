<!--
Search form component displayed on full record pages, allowing keyword searches and changing display modes.
-->
<template>
    <div class="browse-search field has-addons">
        <div class="control is-expanded">
            <input @keyup.enter="getResults" class="input is-medium" type="text" v-model.trim="search_query"
                    :placeholder="searchText" :aria-label="searchText">
        </div>
        <div class="control">
            <button @click="getResults" class="button is-medium">{{ $t('search.search') }}</button>
        </div>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';
    import clearFacetsButton from "@/components/clearFacetsButton.vue";

    export default {
        name: 'browseSearch',

        components: {clearFacetsButton},

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
                const encode_search = { anywhere: encodeURIComponent(this.search_query), start: 0 };
                const update_parameters = this.urlParams(encode_search, 'displayRecords');
                this.routeWithParams(update_parameters);
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
