import { defineStore } from 'pinia'

export const useAltTextStore = defineStore( 'alt-text',{
    state: () => ({
        activeField: '',
        alertMessage: '',
        alertMessageType: '', // valid options success, error
        currentRow: null,
        currentUuid: null,
        error: null,
        items: [],
        showAltTextModal: false,
        viewType: 'view' // view or edit
    }),
    actions: {
        setActiveField(activeField) {
            this.activeField = activeField
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
        },
        /**
         * Fetches the items for the alt text table from the server and updates the store's state.
         * We can't use the fetchWrapper here.
         * @returns {Promise<void>}
         */
        async fetchTableItems() {
            const response = await fetch(`/services/api/machineGeneratedSearch/${this.currentUuid}?format=Image`);
           // const response = await fetch('/static/real-alt-text.json');
            if (!response.ok) {
                const error = new Error('Network response was not ok');
                error.response = response;
                throw error;
            }

            const rows = await response.json();
            this.items = Array.isArray(rows.metadata) ? rows.metadata : [];
        }
    }
});