<template>
    <teleport to="#alt-text-admin">
        <div id="alt-text-viewer">
            <h1 class="has-text-weight-semibold is-size-3 has-text-centered">Machine Generated Alt Text for {{ currentUuid }}</h1>
            <data-table v-if="currentUuid" :key="`alt-text-table-${currentUuid}-${columns.length}`" class="display table is-bordered is-striped is-fullwidth" ref="alt_text_table"
                        :columns="columns"
                        :options="tableOptions">
                <thead>
                <tr>
                    <th><span class="is-sr-only">Thumbnail</span></th>
                    <th>Filename</th>
                    <th>Full Description (AI)</th>
                    <th>Alt Text (AI)</th>
                    <th>Transcript (AI)</th>
                    <th>Risk Score</th>
                    <th>Safety Assessment (AI)</th>
                    <th>Output Assessment (AI)</th>
                    <th><span class="is-sr-only">Rerun Alt Text Generation</span></th>
                </tr>
                </thead>
                <tbody></tbody>
            </data-table>
            <p v-else class="has-text-centered mt-4">Loading table data...</p>
            <alt-text-messages></alt-text-messages>
            <alt-text-editor-modal></alt-text-editor-modal>
        </div>
    </teleport>
</template>

<script>
import altTextEditorModal from '@/components/machine-alt-text/altTextEditorModal.vue';
import AltTextMessages from '@/components/machine-alt-text/altTextMessages.vue';
import DataTable from 'datatables.net-vue3';
import DataTablesLib from 'datatables.net-bm';
import FixedHeader from 'datatables.net-fixedheader';
import SearchPanes from 'datatables.net-searchpanes-bm';
import 'datatables.mark.js';
import 'datatables.net-select-bm';
import {mapActions, mapState} from 'pinia';
import {useAltTextStore} from '@/stores/alt-text';

DataTable.use(DataTablesLib);
DataTable.use(FixedHeader);
DataTable.use(SearchPanes);

export default {
    name: 'modalAltText',

    components: {AltTextMessages, altTextEditorModal, DataTable},

    data() {
        return {
            altTextTableClickHandler: null,
            selected_field: ''
        }
    },

    computed: {
        ...mapState(useAltTextStore, ['currentUuid', 'alertMessage', 'tagPaneValues']),

        tagPaneOptions() {
            return (Array.isArray(this.tagPaneValues) ? this.tagPaneValues : [])
                .map(({ label, searchValue, count }) => ({
                    label: `${label} (${count})`,
                    total: count,
                    count,
                    value: (rowData) => {
                        const tags = Array.isArray(rowData?.mgContentTags) ? rowData.mgContentTags : [];
                        return tags.includes(searchValue);
                    }
                }));
        },

        tableOptions() {
            return {
                columnDefs: this.columnDefs,
                mark: true, // Enables the mark.js integration for search highlighting
                searching: true,
                serverSide: true,
                processing: false,
                stateSave: false,
                ajax: this.tableAjax,
                order: [[1, 'asc']],
                fixedHeader: true,
                searchPanes: {
                    panes: [
                        {
                            header: 'Search Tags',
                            orderable: false,
                            options: this.tagPaneOptions
                        }
                    ],
                    threshold: 1,
                    initCollapsed: true,
                    orderable: false
                },
                layout: {
                    topStart: 'searchPanes',
                    topEnd: {
                        search: {
                            placeholder: 'Search'
                        }
                    }
                },
                select: true,
                pageLength: 25
            }
        },

        columns() {
            return [
                {
                    data: 'id',
                    render: (data) => `<figure class="thumbnail"><a href="/record/${data}" target="_blank"><img alt="" loading="lazy" src="/services/api/thumb/${data}/small"></a></figure>`
                },
                {
                    data: 'title',
                    render: (data, type, row) => `<a href="/record/${row.id}" target="_blank">${data}</a>`
                },
                {
                    data: 'mgFullDescription',
                    render: (data) => this.longText(data, 'mgFullDescription')
                },
                {
                    data: 'altText',
                    render: (data) => this.longText(data, 'altText')
                },
                {
                    data: 'mgTranscript',
                    render: (data) => this.longText(data, 'mgTranscript')
                },
                {
                    data: null,
                    defaultContent: '',
                    render: (data, type, row) => row?.mgRiskScore
                },
                {
                    data: 'mgSafetyAssessment',
                    render: (data) => this.renderSafetyData(data)
                },
                {
                    data: null,
                    defaultContent: '',
                    render: (data, type, row) => this.renderSafetyData(row?.mgReviewAssessment)
                },
                {
                    data: null,
                    defaultContent: '',
                    render: () => '<button class="button is-dark is-small rerun">Rerun</button>'
                }
            ]
        },

        columnDefs() {
            return [
                { width: '15%', targets: [0] },
                { width: '5%', targets: [1, 5, 8] },
                { orderable: false, targets: [0, 6, 7, 8] },
                { searchable: false, targets: [0, 8] },
                // Ensure no non-custom pane is generated from any column.
                { searchPanes: { show: false }, targets: '_all' }
            ]
        }
    },

    methods: {
        ...mapActions(useAltTextStore, ['fetchTableItemsPage', 'setActiveField', 'setAlertMessage',
            'setCurrentRow', 'setCurrentUuid', 'setShowAltTextModal', 'setViewType']),

        async tableAjax(data, callback) {
            try {
                const result = await this.fetchTableItemsPage({
                    start: data.start,
                    length: data.length,
                    search: data.search?.value || ''
                });

                callback({
                    data: result.data,
                    recordsTotal: result.recordsTotal,
                    recordsFiltered: result.recordsFiltered
                });

                this.$nextTick(() => {
                    this.unbindTableEvents();
                    this.bindTableEvents();
                    this.rebuildTagPaneSafely();
                });
            } catch (error) {
                this.setAlertMessage('Unable to load alt text rows.');
                callback({
                    data: [],
                    recordsTotal: 0,
                    recordsFiltered: 0
                });
            }
        },

        rebuildTagPaneSafely() {
            const dtApi = this.$refs.alt_text_table?.dt;
            const settings = dtApi?.settings?.()?.[0];
            if (!dtApi || !settings || !Array.isArray(settings.aoColumns)) {
                return;
            }

            // SearchPanes can throw during redraw if the table columns are not fully initialized yet.
            if (settings.aoColumns.length !== this.columns.length) {
                return;
            }

            const pane = settings.oInit?.searchPanes?.panes?.[0];
            if (pane) {
                pane.options = this.tagPaneOptions;
            }

            try {
                if (dtApi.searchPanes?.rebuildPane) {
                    // Rebuild only the custom pane to avoid column-index rebuilds on removed columns.
                    dtApi.searchPanes.rebuildPane(0, true);
                }
            } catch (error) {
                // Ignore transient rebuild timing errors; panes will rebuild on the next draw.
            }
        },

        fieldName(field) {
            const parts = field.split('_')
            return parts.join(' ');
        },

        longText(data, field_name) {
            const normalized_text = data || '';
            const has_long_text = normalized_text.length > 250;
            const text = (has_long_text) ? `${normalized_text.substring(0, 250)}... ` : normalized_text;
            let sub_text = '<div class="mt-2">';
            if (has_long_text) {
                sub_text += `<div class="is-hidden">${normalized_text}</div>`;
                sub_text += `<a data-action="view" data-action-field="${field_name}" href="#">View All</a><br/>`
            }
            return `${text}${sub_text}<a data-action="edit" data-action-field="${field_name}" href="#">Edit</a></div>`;
        },

        formatSafetyValue(data) {
            if (Array.isArray(data)) {
                if (data.length === 0) {
                    return 'none';
                }
                let text = '<div class="content"><ul>';
                data.forEach(item => {
                    text += `<li>${this.formatSafetyValue(item)}</li>`;
                });
                text += '</ul></div>';
                return text;
            }

            if (data && typeof data === 'object') {
                let text = '<div class="content"><ul>';
                Object.entries(data).forEach(([field, value]) => {
                    text += `<li><span class="has-text-weight-semibold">${this.fieldName(field)}</span>: ${this.formatSafetyValue(value)}</li>`;
                });
                text += '</ul></div>';
                return text;
            }

            if (data === null || data === undefined || data === '') {
                return 'None';
            }

            return String(data).toLowerCase();
        },

        renderSafetyData(data, type = 'display') {
            if (type === 'sp') {
                return this.extractLeafValues(data);
            }

            let text = '<ul class="is-capitalized">';
            Object.entries(data || {}).forEach(([field, value]) => {
                text += `<li><span class="has-text-weight-semibold">${this.fieldName(field)}</span>: ${this.formatSafetyValue(value)}</li>`;
            });
            text += '</ul>';

            return text;
        },

        rerunAltTextGeneration() {},


        bindTableEvents() {
            const dtApi = this.$refs.alt_text_table?.dt;
            if (!dtApi || this.altTextTableClickHandler) {
                return;
            }
            this.altTextTableClickHandler = (e) => {
                const action_fields = ['mgFullDescription', 'altText', 'mgTranscript'];
                if (action_fields.includes(e.target.dataset.actionField)) {
                    e.preventDefault();
                    this.setCurrentRow(dtApi.row(e.currentTarget).data());
                    this.setActiveField(e.target.dataset.actionField);
                    this.setViewType(e.target.dataset.action)
                    this.setShowAltTextModal(true);
                }
                if (e.target.className.includes('rerun')) {
                    console.log('rerun')
                }
            };
            dtApi.on('click', 'tbody tr', this.altTextTableClickHandler);
        },

        unbindTableEvents() {
            const dtApi = this.$refs.alt_text_table?.dt;
            if (dtApi && this.altTextTableClickHandler) {
                dtApi.off('click', 'tbody tr', this.altTextTableClickHandler);
            }
            this.altTextTableClickHandler = null;
        },

        editCell() {
            this.bindTableEvents();
        }
    },

    beforeMount() {
        const uuid = this.$route.params.uuid ?? null;
        this.setCurrentUuid(uuid);
    },

    mounted() {
        this.$nextTick(() => {
            this.editCell();
        });
    },

    beforeUnmount() {
        this.unbindTableEvents();
    }
}
</script>

<style>
@import 'datatables.net-bm';
@import 'datatables.net-searchpanes-bm';
@import 'datatables.net-select-bm';

#alt-text-viewer {
    h1 {
        margin-top: 15px;
        margin-bottom: 10px;
    }
    div.datatable {
        width: 98%;
        margin: auto;
    }
    td.filename {
        overflow-wrap: break-word;
        word-break: break-all;
    }
    div.dt-container div.dt-length select {
        min-width: 70px;
    }
    div.dtsp-panesContainer div.dtsp-title {
        padding-right: 5px;
    }
}
</style>