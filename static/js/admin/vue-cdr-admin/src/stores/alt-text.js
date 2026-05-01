import { defineStore } from 'pinia'

export const useAltTextStore = defineStore( 'alt-text',{
    state: () => ({
        activeField: '',
        alertMessage: '',
        alertMessageType: '', // valid options success, error
        currentRow: null,
        currentUuid: null,
        error: null,
        tagPaneValues: [],
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
            if (this.currentUuid !== uuid) {
                this.tagPaneValues = [];
            }
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
        extractTagPaneValues(facetFields) {
            const facets = Array.isArray(facetFields) ? facetFields : [];
            const tagFacet = facets.find((facet) => facet?.name === 'MG_CONTENT_TAGS');
            const values = Array.isArray(tagFacet?.values) ? tagFacet.values : [];

            return values.map((entry) => ({
                label: entry.displayValue,
                searchValue: entry.searchValue,
                count: entry.count
            }));
        },
        /**
         * Retrieves server-side page for DataTables and updates the store item cache.
         * We can't use the fetchWrapper here.
         * @returns {Promise<{data: Array, recordsTotal: number, recordsFiltered: number}>}
         */
        async fetchTableItemsPage({ start = 0, length = 25, search = '' } = {}) {
            const params = new URLSearchParams({
                start: String(start),
                length: String(length),
                q: search
            });
            const response = await fetch(`/services/api/machineGeneratedSearch/${this.currentUuid}?${params.toString()}`);
           // const response = await fetch(`/static/real-alt-text.json?${params.toString()}`);
            if (!response.ok) {
                const error = new Error('Network response was not ok');
                error.response = response;
                throw error;
            }

            const rows = await response.json();
            const data = Array.isArray(rows.metadata) ? rows.metadata : [];
            const totalFallback = Array.isArray(rows.metadata) ? rows.metadata.length : 0;
          //  const facetTagValues = this.extractTagPaneValues(rows.facetFields);

            this.items = data;
         //   this.tagPaneValues = facetTagValues;

            return {
                data,
                recordsTotal: Number.isFinite(rows.recordsTotal) ? rows.recordsTotal : (Number.isFinite(rows.total) ? rows.total : totalFallback),
                recordsFiltered: Number.isFinite(rows.recordsFiltered) ? rows.recordsFiltered : (Number.isFinite(rows.filtered) ? rows.filtered : totalFallback)
            };
        }
    }
});