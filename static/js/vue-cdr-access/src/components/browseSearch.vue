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
    export default {
        name: 'browseSearch',

        props: {
            recordId: String
        },

        data() {
            return {
                search_query: ''
            }
        },

        methods: {
            getResults() {
                let self = this;

                fetch(`/listJson/${this.recordId}?anywhere=${encodeURI(this.search_query)}`)
                    .then(function(response) {
                        return response.json();
                    }).then(function(data) {
                    self.$emit('browse-query-results', data);
                });
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