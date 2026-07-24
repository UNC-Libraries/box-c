import { defineStore } from 'pinia'

export const useFileVersioningStore = defineStore('fileVersioning', {
    state: () => ({
        actionHandler: {},
        alertHandler: {},
        checkForUnsavedChanges: false,
        resultObject: {},
        showFileVersioningModal: false
    }),
    actions: {
        setActionHandler(actionHandler) {
            this.actionHandler = actionHandler;
        },
        setAlertHandler(alertHandler) {
            this.alertHandler = alertHandler;
        },
        setCheckForUnsavedChanges(unsavedChanges) {
            this.checkForUnsavedChanges = unsavedChanges;
        },
        setResultObject(resultObject) {
            this.resultObject = resultObject;
        },
        setShowFileVersioningModal(showFileVersioningModal) {
            this.showFileVersioningModal = showFileVersioningModal;
        }
    }
});