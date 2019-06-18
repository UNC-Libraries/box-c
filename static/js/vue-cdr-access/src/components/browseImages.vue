<template>
    <span class="imgs-only" v-if="container_type === 'Collection'">
        Show images only? <input @click="update_images" title="show images only" class="checkbox" type="checkbox" v-model="images_only">
    </span>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';

    export default {
        name: 'browseImages',

        props: {
            container_type: String
        },

        mixins: [routeUtils],

        data() {
            return {
                images_only: false
            }
        },

        watch: {
            '$route.query'(d) {
                this.images_only = 'format' in d;
            },

        },

        methods: {
            update_images() {
                let update_params = {
                    page: 1,
                    start: 0
                };

                let url_params = this.urlParams(update_params);

                if (this.images_only) {
                    url_params.format = 'image';
                } else {
                    delete url_params.format;
                }

                this.$router.push({ name: 'browseDisplay', query: url_params });
            }
        },

        mounted() {
            this.images_only = this.paramExists('format', this.urlParams());
        }
    }
</script>

<style lang="scss" scoped>
    $unc-blue: #4B9CD3;

    .imgs-only {
        color: $unc-blue;

        .checkbox {
            height: 50px;
            vertical-align: middle;
            width: 40px;
        }
    }
</style>