<!--
Top level component used for the collection browse page
-->
<template>
    <header-small/>
    <div v-if="!is_loading && !show_404 && !show_503">
        <div class="collection-list container has-text-centered py-5">
            <h2 class="title is-3">{{ $t('collecting_units') }}</h2>
        </div>

        <div class="container collection-browse">
            <list-display :record-list="records" :exclude-browse-type-from-record-urls="true"></list-display>
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
            async retrieveData() {
                try {
                    const response = await fetch('api/collectionsJson');
                    if (!response.ok) {
                        const error = new Error('Network response was not ok');
                        error.response = response;
                        throw error;
                    }
                    const data = await response.json();
                    this.records = data.metadata;
                    this.is_loading = false;
                } catch (error) {
                    this.setErrorResponse(error);
                    this.is_loading = false;
                    console.log(error);
                }
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