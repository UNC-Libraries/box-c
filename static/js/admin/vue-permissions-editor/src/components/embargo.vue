<template>
    <div id="set-embargo">
        <div class="embargo-details">
            <h3>Set Embargo</h3>
            <p v-if="has_embargo">Embargo stuff</p>
            <p v-else>No embargo set for this object</p>
            <button @click="addEmbargo" id="add-embargo">Add Embargo</button>
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
                embargo_info: [],
                has_embargo: false
            }
        },

        methods: {
            getEmbargo() {

            },

            addEmbargo() {
                this.has_embargo = true;
            },

            removeEmbargo() {
                this.has_embargo = false;
            }
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
            margin-top: 20px;
        }
    }
</style>