<template>
    <div class="browse-search field has-addons">
        <div class="control">
            <input class="input" type="text" v-model.trim="search_query" placeholder="Search within this level">
        </div>
        <div class="control">
            <button @click="getResults" class="button">Search</button>
        </div>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';

    export default {
        name: 'browseSearch',

        mixins: [routeUtils],

        data() {
            return {
                search_query: ''
            }
        },

        methods: {
            getResults() {
                let update_params = { anywhere: encodeURIComponent(this.search_query) };
                this.$router.push({ name: 'browseDisplay', query: this.urlParams(update_params) });
                this.search_query = '';
            }
        }
    };
</script>

<style scoped lang="scss">
    .browse-search {
        input, div:first-child  {
            width: 100%;
        }
    }

    input, button {
        font-size: 1.1rem;
        height: 44px;
        margin-top: 15px;
    }
</style>