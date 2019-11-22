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

        methods: {
            sortRecords() {
                if (this.sort_order === '') {
                    this.sort_order = 'title,normal';
                }

                let is_search_sort = this.browseType === 'search';
                this.$router.push({
                    name: this.$route.name,
                    query: this.urlParams({ sort: this.sort_order }, is_search_sort)
                });
                this.sort_order = '';
            }
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
            height: 2.65em;
        }
    }

    @media screen and (max-width: 768px) {
        .browse-sort {
            float: left;
            margin-bottom: 0;
            margin-top: 0;

            select {
                height: inherit;
            }
        }
    }
</style>