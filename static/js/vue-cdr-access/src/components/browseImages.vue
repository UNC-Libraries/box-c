<template>
    <span class="imgs-only" v-if="container_type === 'Collection'">
        Show images only? <input title="show images only" class="checkbox" type="checkbox" v-model="images_only">
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

            images_only() {
                let params = this.urlParams();

                if (this.images_only && !this.paramExists('format', params)) {
                    params = this.urlParams({
                        page: 1,
                        start: 0,
                        format: 'image'
                    });
                }

                if (!this.images_only) {
                    params = this.urlParams({
                        format: 'delete'
                    });
                }

                this.$router.push({ name: 'browseDisplay', query: params });
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