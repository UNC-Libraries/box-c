<template>
    <div id="set-embargo">
        <div class="embargo-details">
            <h3>Set Embargo</h3>
            <p v-if="has_embargo">{{ embargo_info.text }}</p>
            <p v-else>No embargo set for this object</p>
            <button @click="changeEmbargo" id="add-embargo">{{ embargoText }}</button>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'embargo',

        props: {
            uuid: String,
        },

        data() {
            return {
                embargo_info: { text: 'May 5 2099'},
                has_embargo: false
            }
        },

        computed: {
            embargoText() {
                return this.has_embargo ? 'Remove Embargo' : 'Add Embargo';
            }
        },

        methods: {
            getEmbargo() {
            },

            changeEmbargo() {
                this.has_embargo = !this.has_embargo;
                this.$emit('embargo', this.embargo_info);
            },
        },

        mounted() {
            this.getEmbargo();
        }
    }
</script>

<style scoped lang="scss">
    #set-embargo {
        border-top: 1px solid gray;
        margin: 25px -25px;

        .embargo-details {
            margin: 15px 25px;
        }

        #add-embargo {
            margin-left: 0;
            margin-top: 20px;
            width: 115px;
        }
    }
</style>