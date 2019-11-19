<template>
    <div>
        <list-display :record-list="records"></list-display>
    </div>
</template>

<script>
    import listDisplay from "./listDisplay";
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

        mounted() {
            this.retrieveData();
        }
    }
</script>

<style scoped lang="scss"></style>