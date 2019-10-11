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

        data() {
            return {
                embargo_info: {},
                has_embargo: false
            }
        },

        computed: {
            embargoText() {
                return this.has_embargo ? 'Remove Embargo' : 'Add Embargo';
            }
        },

        methods: {
            changeEmbargo() {
                this.has_embargo = !this.has_embargo;
                this.embargo_info = (!this.has_embargo) ? {} : { text: 'May 5 2099' };
                this.$emit('embargo-info', this.embargo_info);
            },
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