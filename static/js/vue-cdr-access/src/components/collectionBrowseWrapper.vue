<!--
Top level component used for the collection browse page
-->
<template>
    <header-small/>
    <div>
        <div class="search-query-text collection-list has-text-centered">
            <h2>{{ $t('collecting_units') }}</h2>
        </div>

        <div class="columns">
            <div class="column collection-browse">
        <list-display :record-list="records" :use-saved-browse-type="true"></list-display>
            </div>
        </div>
    </div>
</template>

<script>
    import headerSmall from "@/components/header/headerSmall.vue";
    import listDisplay from "@/components/listDisplay.vue";
    import get from 'axios';

    export default {
        name: 'collectionBrowseWrapper',

        components: {headerSmall, listDisplay},

        data() {
            return {
                records: [],
            }
        },

        head() {
            return {
                title: 'Collections'
            }
        },

        methods: {
            retrieveData() {
                get('collectionsJson').then((response) => {
                    this.records = response.data.metadata;
                }).catch(function (error) {
                    console.log(error);
                });
            }
        },

        created() {
            this.retrieveData();
        }
    }
</script>

<style scoped lang="scss">
    ul {
        margin: inherit;
    }
</style>