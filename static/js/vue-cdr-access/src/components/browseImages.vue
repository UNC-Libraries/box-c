<template>
    <span class="imgs-only" v-if="container_type === 'Collection'">
        Show images only? <input title="show images only" class="checkbox" type="checkbox" v-model="images_only">
    </span>
</template>

<script>
    import {utils} from "../utils/helper_methods";

    export default {
        name: 'browseImages',

        props: {
            container_type: String
        },

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
                let params = utils.urlParams();

                if (this.images_only) {
                    params.page = 1;
                    params.start = 0;
                    params.format = 'image';
                } else {
                    delete params.format;
                }

                this.$router.push({ name: 'browseDisplay', query: params });
            }
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