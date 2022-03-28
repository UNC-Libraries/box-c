<!--
Top level component used for the collection browse page
-->
<template>
    <div>
        <list-display :record-list="records" :use-saved-browse-type="true"></list-display>
    </div>
</template>

<script>
    import listDisplay from "@/components/listDisplay.vue";
    import get from 'axios';

    export default {
        name: 'collectionBrowse',

        components: {listDisplay},

        data() {
            return {
                records: [],
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