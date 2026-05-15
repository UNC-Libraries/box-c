import { defineStore } from 'pinia'

export const useAltTextStore = defineStore('alt-text', {
    state: () => ({
        activeField: '',
        alertMessage: '',
        alertMessageType: '', // valid options success, error
        currentRow: null,
        currentUuid: null,
        error: null,
        showAltTextModal: false,
        viewType: 'view' // view or edit
    }),
    actions: {
        setActiveField(activeField) {
            this.activeField = activeField;
        },
        setAlertMessage(alertMessage) {
            this.alertMessage = alertMessage;
        },
        setAlertMessageType(alertMessageType) {
            this.alertMessageType = alertMessageType;
        },
        setCurrentRow(row) {
            this.currentRow = row;
        },
        setCurrentUuid(uuid) {
            this.currentUuid = uuid;
        },
        setCurrentRowFieldValue(field, value) {
            this.currentRow[field] = value;
        },
        setShowAltTextModal(showAltTextModal) {
            this.showAltTextModal = showAltTextModal;
        },
        setViewType(viewType) {
            this.viewType = viewType;
        }
    }
});