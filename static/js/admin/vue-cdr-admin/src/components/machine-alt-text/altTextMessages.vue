<template>
    <div class="is-relative">
        <div id="machine-alt-text" class="notification is-light" :class="messageClasses">
            <button @click="hideMessage()" class="delete"></button>
            <p>{{ alertMessage }}</p>
        </div>
    </div>
</template>

<script>
import {mapActions, mapState} from "pinia";
import {useAltTextStore} from "@/stores/alt-text";

export default {
    name: "altTextMessages",

    computed: {
        ...mapState(useAltTextStore, ['alertMessage', 'alertMessageType']),

        displayMessage() {
            return this.alertMessage !== '';
        },

        messageClasses() {
            return {
                'is-hidden': !this.displayMessage,
                'is-success': this.alertMessageType === 'success',
                'is-danger': this.alertMessageType !== 'success'
            }
        }
    },

    methods: {
        ...mapActions(useAltTextStore, ['setAlertMessage', 'setAlertMessageType']),

        hideMessage() {
            this.setAlertMessage('');
            this.setAlertMessageType('');
        }
    }
}
</script>

<style scoped>
    #machine-alt-text {
        position: fixed;
        top: 50px;
        right: 25px;
        height: fit-content;
        width: fit-content;
        z-index: 100;

        p {
            padding-right: 20px;
        }
    }
</style>