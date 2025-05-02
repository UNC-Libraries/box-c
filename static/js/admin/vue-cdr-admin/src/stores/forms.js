import { defineStore } from 'pinia'

export const useFormsStore = defineStore( 'forms',{
    state: () => ({
        alertHandler: {},
        containerId: '',
        showFormsModal: false
    }),
    actions: {
        setAlertHandler(alertHandler) {
            this.alertHandler = alertHandler;
        },
        setContainerId(containerId) {
            this.containerId = containerId;
        },
        setShowFormsModal(showFormsModal) {
            this.showFormsModal = showFormsModal;
        }
    }
});