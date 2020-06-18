<template>
    <div class="browse-sort select is-medium">
        <select @change="sortRecords" v-model="sort_order">
            <option value="">Sort by...</option>
            <option value="title,normal">Title A-Z</option>
            <option value="title,reverse">Title Z-A</option>
            <option value="dateAdded,normal">Date Created (newest)</option>
            <option value="dateAdded,reverse">Date Created (oldest)</option>
        </select>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';

    export default {
        name: 'browseSort',

        mixins: [routeUtils],

        props: {
            browseType: String
        },

        data() {
            return {
                sort_order: ''
            }
        },

        watch: {
            '$route.query'(route) {
                this.sort_order = this.$route.query.sort || this.setDefaultSort();
            }
        },

        methods: {
            sortRecords() {
                this.setDefaultSort();

                let is_search_sort = this.browseType === 'search';

                this.$router.push({
                    path: this.$route.path,
                    query: this.urlParams({ sort: this.sort_order }, is_search_sort)
                }).catch((e) => {
                    if (this.nonDuplicateNavigationError(e)) {
                        throw e;
                    }
                });
            },

            setDefaultSort() {
                if (this.sort_order === '') {
                    this.sort_order = 'title,normal';
                }
            }
        },

        mounted() {
            this.sort_order = this.$route.query.sort || this.setDefaultSort();
        }
    };
</script>

<style scoped lang="scss">
    .select:not(.is-multiple):not(.is-loading)::after {
        top: 60% !important;
    }

    .browse-sort {
        float: right;
        margin-top: 15px;
        margin-right: 25px;

        select {
            height: 50px;
        }
    }

    @media screen and (max-width: 768px) {
        .browse-sort {
            float: left;
            padding-left: 15px;
            margin-bottom: 10px;
            margin-top: 0;
        }

    }
</style>