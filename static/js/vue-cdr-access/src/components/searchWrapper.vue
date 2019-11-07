<template>
    <div class="contentarea">
        <img v-if="is_loading" src="/static/images/ajax-loader-lg.gif" alt="data loading icon">
        <list-display :record-list="records"></list-display>
    </div>
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
                is_loading: true,
                records: [],
            }
        },

        methods: {
            retrieveData() {
                let param_string = this.formatParamsString(this.$route.query);

                get(`searchJson/${param_string}`).then((response) => {
                    this.records = response.data.metadata;
                    this.is_loading = false;
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
    img {
        display: block;
        margin: 25px auto;
    }
</style>