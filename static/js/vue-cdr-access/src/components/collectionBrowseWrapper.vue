<!--
Top level component used for the collection browse page
-->
<template>
    <header-small/>
    <div v-if="!is_loading && !show_404 && !show_503">
        <div class="search-query-text collection-list has-text-centered">
            <h2>{{ $t('collecting_units') }}</h2>
        </div>

        <div class="columns">
            <div class="column collection-browse">
        <list-display :record-list="records" :exclude-browse-type-from-record-urls="true"></list-display>
            </div>
        </div>
    </div>
    <not-found v-if="show_404" :display-header="false"></not-found>
    <not-available v-if="show_503" :display-header="false"></not-available>
</template>

<script>
    import headerSmall from "@/components/header/headerSmall.vue";
    import listDisplay from "@/components/listDisplay.vue";
    import notFound from "@/components/error_pages/notFound.vue";
    import notAvailable from "@/components/error_pages/notAvailable.vue";
    import errorUtils from "../mixins/errorUtils";
    import get from 'axios';

    export default {
        name: 'collectionBrowseWrapper',

        components: {headerSmall, listDisplay, notAvailable, notFound},

        mixins: [errorUtils],

        data() {
            return {
                is_loading: true,
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
                    this.is_loading = false;
                }).catch((error) => {
                    this.setErrorResponse(error);
                    this.is_loading = false;
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