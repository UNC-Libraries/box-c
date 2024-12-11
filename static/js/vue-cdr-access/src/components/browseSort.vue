<!--
Sort drop down menu used in search results
-->
<template>
    <div class="browse-sort field is-narrow">
        <div class="control">
            <div class="select is-medium">
                <select @change="sortRecords" v-model="sort_order" aria-label="Sort options">
                    <option value="default,normal">{{ $t('sort.relevance') }}</option>
                    <option value="title,normal">{{ $t('sort.title_a-z') }}</option>
                    <option value="title,reverse">{{ $t('sort.title_z-a') }}</option>
                    <option value="dateCreated,normal">{{ $t('sort.date_created_newest') }}</option>
                    <option value="dateCreated,reverse">{{ $t('sort.date_created_oldest') }}</option>
                    <option value="dateAdded,normal">{{ $t('sort.date_added_newest') }}</option>
                    <option value="dateAdded,reverse">{{ $t('sort.date_added_oldest') }}</option>
                </select>
            </div>
        </div>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';
    const DEFAULT_SEARCH = 'default,normal';

    export default {
        name: 'browseSort',

        mixins: [routeUtils],

        props: {
            browseType: String
        },

        data() {
            return {
                sort_order: DEFAULT_SEARCH
            }
        },

        watch: {
            '$route.query': {
                handler(route) {
                    this.setSort();
                },
                deep: true
            }
        },

        methods: {
            /**
             * Pushes url with the desired sort order and resets start row to 0, since a new query is being run
             */
            sortRecords() {
                let is_search_sort = this.browseType === 'search';

                this.$router.push({
                    path: this.$route.path,
                    query: this.urlParams({ sort: this.sort_order, start: 0 }, is_search_sort)
                }).catch((e) => {
                    if (this.nonDuplicateNavigationError(e)) {
                        throw e;
                    }
                });
            },

            setSort() {
                this.sort_order = this.$route.query.sort || DEFAULT_SEARCH;
            }
        },

        mounted() {
            this.setSort();
        }
    };
</script>