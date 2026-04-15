import { defineStore } from 'pinia'

export const useAltTextStore = defineStore( 'alt-text',{
    state: () => ({
        activeField: '',
        alertHandler: {
            alertHandler: () => {}
        },
        currentRow: {},
        error: null,
        items: [],
        showAltTextModal: false,
        viewType: 'view' // view or edit
    }),
    actions: {
        setActiveField(activeField) {
            this.activeField = activeField
        },
        setAlertHandler(alertHandler) {
            this.alertHandler = alertHandler;
        },
        setCurrentRow(row) {
            this.currentRow = row;
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
            // const response = await fetch(`/api/record/${this.uuid}/metadataView`);
            const response = await fetch('/static/alt-text.json');
            if (!response.ok) {
                const error = new Error('Network response was not ok');
                error.response = response;
                throw error;
            }

            this.items = await response.json();
        }
    }
});