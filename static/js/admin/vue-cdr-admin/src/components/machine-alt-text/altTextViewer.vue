<template>
    <teleport to="#alt-text-admin">
        <div id="alt-text-viewer">
            <h1 class="has-text-weight-semibold is-size-3 has-text-centered">Machine Generated Alt Text for {{ currentUuid }}</h1>
            <div v-if="hasSearchPaneOptions" class="tag-filter-pane">
                <button class="tag-filter-toggle" @click="paneExpanded = !paneExpanded">
                    <span class="tag-filter-title">Search Tags</span>
                    <span class="icon is-small">
                        <i :class="paneExpanded ? 'fas fa-chevron-up' : 'fas fa-chevron-down'"></i>
                    </span>
                </button>
                <div v-if="paneExpanded" class="tag-filter-options">
                    <div v-for="facet in contentTagFacets" :key="facet.value"
                         class="tag-filter-option"
                         :class="{ 'is-selected': selectedTags.includes(facet.value) }"
                         @click="toggleTag(facet.value)">
                        <span class="tag-filter-label">{{ fieldName(facet.displayValue) }}</span>
                        <span class="tag-filter-count">{{ facet.count }}</span>
                    </div>
                </div>
            </div>
            <data-table v-if="currentUuid" :key="`alt-text-table-${currentUuid}`" class="display table is-bordered is-striped is-fullwidth" ref="alt_text_table"
                        :columns="columns"
                        :options="tableOptions"
                        :ajax="ajaxOptions">
                <thead>
                <tr>
                    <th><span class="is-sr-only">Thumbnail</span></th>
                    <th>Filename</th>
                    <th>Full Description (AI)</th>
                    <th>Full Description (Human)</th>
                    <th>Alt Text (AI)</th>
                    <th>Alt Text (Human)</th>
                    <th>Transcript (AI)</th>
                    <th>Transcript (Human)</th>
                    <th>Risk Score</th>
                    <th>Safety Assessment (AI)</th>
                    <th>Output Assessment (AI)</th>
                    <th><span class="is-sr-only">Rerun Alt Text Generation</span></th>
                </tr>
                </thead>
                <tbody></tbody>
            </data-table>
            <p v-else class="has-text-centered mt-4">No UUID provided.</p>
            <alt-text-messages></alt-text-messages>
            <alt-text-editor-modal></alt-text-editor-modal>
        </div>
    </teleport>
</template>

<script>
import altTextEditorModal from '@/components/machine-alt-text/altTextEditorModal.vue';
import AltTextMessages from '@/components/machine-alt-text/altTextMessages.vue';
import fetchUtils from "@/mixins/fetchUtils";
import DataTable from 'datatables.net-vue3';
import DataTablesLib from 'datatables.net-bm';
import FixedHeader from 'datatables.net-fixedheader';
import 'datatables.mark.js';
import 'datatables.net-buttons-bm'
import 'datatables.net-buttons/js/buttons.colVis.js';
import {mapActions, mapState} from 'pinia';
import {useAltTextStore} from '@/stores/alt-text';

DataTable.use(DataTablesLib);
DataTable.use(FixedHeader);

const DEFAULT_PER_PAGE = 25;

export default {
    name: 'modalAltText',

    components: {AltTextMessages, altTextEditorModal, DataTable},

    mixins: [fetchUtils],

    data() {
        return {
            altTextTableClickHandler: null,
            selected_field: '',
            contentTagFacets: [],
            selectedTags: [],
            paneExpanded: false
        }
    },

    watch: {
        lastSuccessfulEdit(edit) {
            if (!edit) {
                return;
            }
            this.$nextTick(() => {
                this.applySuccessfulEdit(edit);
            });
        }
    },

    computed: {
        ...mapState(useAltTextStore, ['currentUuid', 'lastSuccessfulEdit']),

        ajaxOptions() {
            let lastDraw = 0;
            return {
                url: `/services/api/machineGeneratedSearch/${this.currentUuid}`,
                data: (d) => {
                    lastDraw = d.draw;
                    // Column id, column name
                    const sortFieldByColumn = {
                        1: 'title',
                        8: 'mgRiskScore'
                    };
                    const sortOrder = {'asc': 'normal', 'desc': 'reverse'};

                    if (d.order[0] !== undefined) {
                        const columnIndex = d.order[0].column;
                        const direction = sortOrder[d.order[0].dir];
                        const sortField = sortFieldByColumn[columnIndex];
                        d.sort = sortField && direction ? `${sortField},${direction}` : undefined;
                    }

                    return {
                        format: 'Image',
                        rows: d.length,
                        page: Math.floor(d.start / d.length) + 1,
                        start: d.start || 0,
                        anywhere: d.search?.value,
                        getFacets: true,
                        facetSelect: 'mgContentTags',
                        rollup: false,
                        // Conditionally adds sort and/or search tag depending on whether there's an active
                        // sort or search, to avoid sending unnecessary query parameters
                        ...(d.sort ? { sort: d.sort } : {}),
                        ...(this.selectedTags.length > 0 ? { mgContentTags: encodeURIComponent(this.selectedTags.join('||')) } : {})
                    };
                },
                dataSrc: (d) => {
                    const items = Array.isArray(d.metadata) ? d.metadata : [];
                    this.setItems(items);

                    // Keep pane facets in sync with each consumed response so counts/options stay current in the UI.
                    const mgTagsFacet = (d.facetFields || []).find(f => f.name === 'MG_CONTENT_TAGS');
                    this.contentTagFacets = Array.isArray(mgTagsFacet?.values) ? [...mgTagsFacet.values] : [];
                    const availableFacetValues = new Set(this.contentTagFacets.map(facet => facet.value));
                    this.selectedTags = this.selectedTags.filter(tag => availableFacetValues.has(tag));

                    return items;
                },
                dataFilter: (data) => {
                    const json = JSON.parse(data);
                    // Echo the draw counter back so DataTables processes responses in the correct sequence
                    json.draw = lastDraw;
                    // Map the API's resultCount to the field names DataTables expects for server-side pagination
                    json.recordsTotal = Number(json.resultCount) || 0;
                    json.recordsFiltered = Number(json.resultCount) || 0;
                    return JSON.stringify(json);
                }
            };
        },

        tableOptions() {
            return {
                serverSide: true,
                columnDefs: this.columnDefs,
                mark: true,
                searching: true,
                order: [[1, 'asc']],
                fixedHeader: true,
                layout: {
                    top2Start: {
                        buttons: [
                            {
                                extend: 'colvis',
                                postfixButtons: ['colvisRestore']
                            }
                        ]
                    },
                    top2End: {
                        buttons: [
                            {
                                text: 'Reset Table',
                                action: function(e, dt, node, config) {
                                    dt.state.clear();
                                    window.location.reload();
                                }
                            }
                        ]
                    },
                    topEnd: {
                        search: {
                            placeholder: 'Search'
                        }
                    }
                },
                pageLength: DEFAULT_PER_PAGE
            }
        },

        hasSearchPaneOptions() {
            return this.contentTagFacets.length > 0;
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
                    data: 'fullDescription',
                    render: (data) => this.longText(data, 'fullDescription')
                },
                {
                    data: 'mgAltText',
                    render: (data) => this.longText(data, 'mgAltText')
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
                    data: 'transcript',
                    render: (data) => this.longText(data, 'transcript')
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
                    data: 'mgReviewAssessment',
                    render: (data) => this.renderSafetyData(data)
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
                { width: '10%', targets: [0] },
                { width: '5%', targets: [1, 8] },
                { orderable: false, targets: [0, 2, 3, 4, 5, 6, 7, 9, 10, 11] },
                { searchable: false, targets: [0, 11] }
            ]
        }
    },

    methods: {
        ...mapActions(useAltTextStore, ['setActiveField', 'setAlertMessage',
            'setCurrentRow', 'setCurrentUuid', 'setItems', 'clearLastSuccessfulEdit', 'setShowAltTextModal', 'setViewType']),

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
            let text_display = `${text}${sub_text}`;
            // Machine generated fields should not be editable
            if (!/^mg/.test(field_name)) {
                text_display += `<a data-action="edit" data-action-field="${field_name}" href="#">Edit</a>`;
            }
            return `${text_display}</div>`;
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

        renderSafetyData(data) {
            let text = '<ul class="is-capitalized">';
            Object.entries(data || {}).forEach(([field, value]) => {
                text += `<li><span class="has-text-weight-semibold">${this.fieldName(field)}</span>: ${this.formatSafetyValue(value)}</li>`;
            });
            text += '</ul>';

            return text;
        },

        rerunAltTextGeneration() {},

        getTags(rowData) {
            return Array.isArray(rowData?.mgContentTags) ? rowData.mgContentTags : [];
        },

        bindTableEvents() {
            const dtApi = this.$refs.alt_text_table?.dt;
            if (!dtApi || this.altTextTableClickHandler) {
                return;
            }
            this.altTextTableClickHandler = (e) => {
                const action_fields = ['mgFullDescription', 'fullDescription', 'mgAltText', 'altText', 'mgTranscript', 'transcript'];
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
        },

        toggleTag(value) {
            const idx = this.selectedTags.indexOf(value);
            if (idx === -1) {
                this.selectedTags.push(value);
            } else {
                this.selectedTags.splice(idx, 1);
            }
            const dtApi = this.$refs.alt_text_table?.dt;
            if (dtApi) {
                dtApi.ajax.reload();
            }
        },

        applySuccessfulEdit(edit) {
            const dtApi = this.$refs.alt_text_table?.dt;
            if (!dtApi || !edit?.id || !edit?.field) {
                this.clearLastSuccessfulEdit();
                return;
            }

            dtApi.rows().every(function() {
                const rowData = this.data();
                if (rowData?.id === edit.id) {
                    const updatedRow = {
                        ...rowData,
                        [edit.field]: edit.value
                    };
                    this.data(updatedRow);
                    return false;
                }
                return true;
            });

            this.clearLastSuccessfulEdit();
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
@import 'datatables.net-buttons-bm';

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

    .tag-filter-pane {
        border: 1px solid #dee2e6;
        border-radius: 4px;
        margin: auto;
        overflow: hidden;
        width: 40%;
    }
    .tag-filter-toggle {
        display: flex;
        align-items: center;
        width: 100%;
        background: #f5f5f5;
        border: none;
        cursor: pointer;
        padding: 8px 12px;
        font-weight: 600;
    }
    .tag-filter-title {
        flex-grow: 1;
        text-align: left;
    }
    .tag-filter-options {
        padding: 4px 0;
    }
    .tag-filter-option {
        display: flex;
        align-items: center;
        padding: 6px 12px;
        cursor: pointer;
    }
    .tag-filter-option:hover {
        background: #f0f0f0;
    }
    .tag-filter-option.is-selected {
        background: #dbf0fa;
        font-weight: 600;
    }
    .tag-filter-label {
        flex-grow: 1;
        text-transform: capitalize;
    }
    .tag-filter-count {
        background: #e0e0e0;
        border-radius: 10px;
        padding: 1px 8px;
        font-size: 0.85em;
    }
    .tag-filter-option.is-selected .tag-filter-count {
        background: #1a698c;
        color: white;
    }
}
</style>