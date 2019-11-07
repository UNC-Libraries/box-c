<template>
    <list-display :record-list="records"></list-display>
</template>

<script>
    import ListDisplay from "./listDisplay";
    import routeUtils from "../mixins/routeUtils";
    import get from 'axios';

    export default {
        name: 'searchWrapper',

        components: {ListDisplay},

        mixins: [routeUtils],

        data() {
            return {
                anywhere: '',
                records: [],
            }
        },

        methods: {
            retrieveData() {
                let param_string = this.formatParamsString(this.$route.query);

                get(`searchJson/${param_string}`).then((response) => {
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

</style>