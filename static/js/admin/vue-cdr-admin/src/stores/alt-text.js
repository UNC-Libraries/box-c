import { defineStore } from 'pinia'

export const useAltTextStore = defineStore( 'alt-text',{
    state: () => ({
        activeField: '',
        alertMessage: '',
        alertMessageType: '', // valid options success, error
        currentRow: null,
        currentUuid: null,
        error: null,
        globalTagCounts: {},
        globalTagCountsLoadedForUuid: null,
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
                this.globalTagCounts = {};
                this.globalTagCountsLoadedForUuid = null;
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
        buildTagCounts(items = []) {
            const counts = {};
            (Array.isArray(items) ? items : []).forEach((item) => {
                const tags = Array.isArray(item?.mgContentTags) ? item.mgContentTags : [];
                tags.forEach((tag) => {
                    counts[tag] = (counts[tag] || 0) + 1;
                });
            });
            return counts;
        },
        parseTagCounts(rawCounts) {
            if (!rawCounts || typeof rawCounts !== 'object') {
                return null;
            }

            if (Array.isArray(rawCounts)) {
                return rawCounts.reduce((acc, entry) => {
                    if (entry?.tag != null && Number.isFinite(entry?.count)) {
                        acc[String(entry.tag)] = entry.count;
                    }
                    return acc;
                }, {});
            }

            return Object.entries(rawCounts).reduce((acc, [tag, count]) => {
                const parsed = Number(count);
                if (Number.isFinite(parsed)) {
                    acc[tag] = parsed;
                }
                return acc;
            }, {});
        },
        async fetchGlobalTagCounts() {
            if (!this.currentUuid) {
                return {};
            }
            if (this.globalTagCountsLoadedForUuid === this.currentUuid) {
                return this.globalTagCounts;
            }

            const response = await fetch(`/services/api/machineGeneratedSearch/${this.currentUuid}`);
            if (!response.ok) {
                const error = new Error('Network response was not ok');
                error.response = response;
                throw error;
            }

            const rows = await response.json();
            const metadata = Array.isArray(rows.metadata) ? rows.metadata : [];
            this.globalTagCounts = this.buildTagCounts(metadata);
            this.globalTagCountsLoadedForUuid = this.currentUuid;
            return this.globalTagCounts;
        },
        /**
         * Fetches a server-side page for DataTables and updates the store item cache.
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
            if (!response.ok) {
                const error = new Error('Network response was not ok');
                error.response = response;
                throw error;
            }

            const rows = await response.json();
            const data = Array.isArray(rows.metadata) ? rows.metadata : [];
            const totalFallback = Array.isArray(rows.metadata) ? rows.metadata.length : 0;
            const parsedTagCounts = this.parseTagCounts(
                rows.globalTagCounts || rows.tagCounts || rows.facets?.mgContentTags
            );

            this.items = data;
            if (parsedTagCounts) {
                this.globalTagCounts = parsedTagCounts;
                this.globalTagCountsLoadedForUuid = this.currentUuid;
            } else {
                try {
                    await this.fetchGlobalTagCounts();
                } catch (error) {
                    // Fallback for APIs without global facets; keep pane data usable.
                    this.globalTagCounts = this.buildTagCounts(data);
                }
            }

            return {
                data,
                recordsTotal: Number.isFinite(rows.recordsTotal) ? rows.recordsTotal : (Number.isFinite(rows.total) ? rows.total : totalFallback),
                recordsFiltered: Number.isFinite(rows.recordsFiltered) ? rows.recordsFiltered : (Number.isFinite(rows.filtered) ? rows.filtered : totalFallback)
            };
        }
    }
});