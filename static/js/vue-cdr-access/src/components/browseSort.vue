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

                this.$router.push({ name: 'browseDisplay', query:  this.urlParams({ sort: this.sort_order }) });
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

        select {
            height: 2.65em;
        }
    }
</style>