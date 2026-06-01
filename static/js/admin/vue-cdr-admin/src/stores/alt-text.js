import { defineStore } from 'pinia'

export const useAltTextStore = defineStore('alt-text', {
    state: () => ({
        activeField: '',
        alertMessage: '',
        alertMessageType: '', // valid options success, error
        currentRow: null,
        currentUuid: null,
        error: null,
        items: [],
        lastSuccessfulEdit: null,
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
        setItems(items) {
            this.items = Array.isArray(items) ? items : [];
        },
        setCurrentRowFieldValue(field, value) {
            if (!this.currentRow) {
                return;
            }
            this.currentRow[field] = value;
            if (this.currentRow.id) {
                const idx = this.items.findIndex(item => item?.id === this.currentRow.id);
                if (idx !== -1) {
                    this.items[idx] = {
                        ...this.items[idx],
                        [field]: value
                    };
                }
            }
        },
        setLastSuccessfulEdit(edit) {
            this.lastSuccessfulEdit = edit;
        },
        clearLastSuccessfulEdit() {
            this.lastSuccessfulEdit = null;
        },
        setShowAltTextModal(showAltTextModal) {
            this.showAltTextModal = showAltTextModal;
        },
        setViewType(viewType) {
            this.viewType = viewType;
        },
        closeModalWindow() {
            this.setShowAltTextModal(false);
            this.setViewType('view');
            this.setActiveField('');
            this.setCurrentRow(null);
        },
    }
});