import { defineStore } from 'pinia'

const PER_PAGE = 20;
export const useAltTextStore = defineStore( 'alt-text',{
    state: () => ({
        activeField: '',
        alertMessage: '',
        alertMessageType: '', // valid options success, error
        currentPage: 1,
        currentRow: null,
        currentUuid: null,
        error: null,
        isLoading: false,
        items: [],
        showAltTextModal: false,
        totalPages: 1,
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
            this.isLoading = true;
            this.items = [];
            this.currentPage = 1;

            try {
                await this.fetchTableItemsPages();
            } finally {
                this.isLoading = false;
                this.currentPage = 1;
            }
        },

        async fetchTableItemsPages() {
            const response = await fetch(`/services/api/machineGeneratedSearch/${this.currentUuid}?format=Image&page=${this.currentPage}`);
            // const response = await fetch('/static/real-alt-text.json');
            if (!response.ok) {
                const error = new Error('Network response was not ok');
                error.response = response;
                throw error;
            }

            const rows = await response.json();
            if (this.currentPage === 1) {
                const resultCount = Number(rows.resultCount);
                this.totalPages = Number.isFinite(resultCount) ? Math.max(1, Math.ceil(resultCount / PER_PAGE)) : 1;
            }

            const pageItems = Array.isArray(rows.metadata) ? rows.metadata : [];
            this.items.push(...pageItems);

            if (this.currentPage < this.totalPages) {
                this.currentPage += 1;
                await this.fetchTableItemsPages();
            }
        }
    }
});