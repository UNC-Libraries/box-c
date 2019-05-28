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
    import {utils} from "../utils/helper_methods";

    export default {
        name: 'browseSort',

        props: {
            pageBaseUrl: ''
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

                let params = utils.urlParams();
                params.sort = this.sort_order;

                this.$router.push({ name: 'browseDisplay', params: params , query:  params });
                this.$emit('sort-ordering');
                this.sort_order = '';
            }
        }
    };
</script>

<style scoped lang="scss">
    .browse-sort {
        margin-top: 15px;
    }
</style>