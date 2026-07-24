<template>
    <div class="vue-dcr-admin-wrapper">
        <div id="file-versioning" class="modal" :class="{'is-active': showModal}">
            <div class="modal-background"></div>
            <div class="modal-card">
                <header class="modal-card-head modal-head">
                    <p class="modal-card-title is-capitalized">{{ modalHeader }}</p>
                    <button class="delete" aria-label="close" @click="closeModal"><span class="is-sr-only">Close</span></button>
                </header>
                <section class="modal-card-body">
                    <h1>Dean</h1>
                </section>
                <footer class="modal-card-foot">
                    <div class="buttons">
                        <button @click="closeModal" class="button is-danger">{{ closeModalButtonText }}</button>
                    </div>
                </footer>
            </div>
        </div>
    </div>
</template>

<script>
import {mapActions, mapState} from "pinia";
import {useFileVersioningStore} from "@/stores/file-versioning";

export default {
    name: 'modalFileVersioning',

    data() {
        return {
            modalHeader: 'File Versioning',
            closeModalButtonText: 'Close'
        }
    },

    computed: {
        ...mapState(useFileVersioningStore, {
            actionHandler: state => state.actionHandler,
            alertHandler: state => state.alertHandler,
            checkForUnsavedChanges: state => state.checkForUnsavedChanges,
            resultObject: state => state.resultObject,
            showModal: state => state.showFileVersioningModal
        })
    },

    methods: {
        ...mapActions(useFileVersioningStore, ['setCheckForUnsavedChanges', 'setShowFileVersioningModal']),

        closeModalCheck() {
            this.setCheckForUnsavedChanges(true);
        },

        resetChangesCheck(check_changes) {
            this.setCheckForUnsavedChanges(check_changes);
        },

        closeModal() {
            this.setShowFileVersioningModal(false);
        }
    }
}
</script>

<style>
#file-versioning.modal {
    z-index: 99;
}
</style>
