<template>
    <teleport to="#alt-text-admin">
        <div id="alt-text-viewer">
            <h1 style="margin-top: 15px" class="has-text-weight-semibold is-size-3 has-text-centered">Machine Generated Alt Text for {{ container_info.title }}</h1>
            <data-table class="display table is-bordered is-striped is-fullwidth" ref="alt_text_table"
                        :columns="columns"
                        :options="tableOptions"
                        :data="items">
                <thead>
                <tr>
                    <th></th>
                    <th>Filename</th>
                    <th>Full Description (AI)</th>
                    <th>Alt Text (AI)</th>
                    <th>Transcript (AI)</th>
                    <th>Safety Assessment (AI)</th>
                    <th>Output Assessment (AI)</th>
                    <th></th>
                </tr>
                </thead>
                <tbody></tbody>
            </data-table>
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
import 'datatables.mark.js';
import 'datatables.net-searchpanes-bm';
import 'datatables.net-select-bm';
import {mapActions, mapState} from "pinia";
import {useAltTextStore} from '@/stores/alt-text';

DataTable.use(DataTablesLib);
DataTable.use(FixedHeader);

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export default {
    name: 'modalAltText',

    components: {AltTextMessages, altTextEditorModal, DataTable},

    data() {
        return {
            altTextTableClickHandler: null,
            container_info: null,
            selected_field: '',
            uuid: ''
        }
    },

    computed: {
        ...mapState(useAltTextStore, ['items', 'alertMessage']),

        tableOptions() {
            return {
                columnDefs: this.columnDefs,
                mark: true, // Enables the mark.js integration
                searching: true,
                order: [[1, 'asc']],
                fixedHeader: true,
                pageLength: 25,
               /* initComplete: function() {
                    this.api()
                        .columns()
                        .every(function() {
                            let column = this;
                            let title = column.header().textContent;
                            if (title === '' || title === 'Thumbnail') {
                                return true;
                            }

                            // Create input element
                            let input = document.createElement('input');
                            input.placeholder = title;
                            input.ariaLabel = title;
                            column.header().replaceChildren(input);

                            // Event listener for user input
                            input.addEventListener('keyup', () => {
                                if (column.search() !== input.value) {
                                    column.search(input.value).draw();
                                }
                            });
                        });
                }*/
            }
        },

        columns() {
            return [
                {
                    data: 'filename',
                    render: (data) => `<figure class="thumbnail"><a href="${data}" target="_blank"><img alt="''" src="${data}"></a></figure>`
                },
                {
                    data: 'filename',
                    className: 'filename',
                    render: (data) => `<a href="${data}" target="_blank">${this.imageName(data)}</a>`
                },
                {
                    data: 'full_desc',
                    render: (data) => this.longText(data, 'full_desc')
                },
                {
                    data: 'alt_text',
                    render: (data) => this.longText(data, 'alt_text')
                },
                {
                    data: 'transcript',
                    render: (data) => this.longText(data, 'transcript')
                },
                {
                    data: 'safety_review',
                    render: (data) => this.renderSafetyData(data)
                },
                {
                    data: 'safety_form',
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
                { width: '15%', targets: [0] },
                { width: '5%', targets: [1] },
                { width: '17%', targets: [2, 3, 4, 5, 6] },
                { orderable: false, targets: [0, 5, 6, 7] },
                { searchable: false, targets: [0, 7] }
            ]
        }
    },

    methods: {
        ...mapActions(useAltTextStore, ['fetchTableItems', 'setActiveField', 'setAlertMessage', 'setCurrentRow', 'setShowAltTextModal', 'setViewType']),

        fieldName(field) {
            const parts = field.split('_')
            return parts.join(' ');
        },

        imageName(data) {
            const text = data.split('/');
            return text[text.length - 1];
        },

        longText(data, field_name) {
            const normalized_text = data || '';
            const has_long_text = normalized_text.length > 250;
            const text = (has_long_text) ? `${normalized_text.substring(0, 250)}... ` : normalized_text;
            let sub_text = '<div class="mt-2">';
            if (has_long_text) {
                // Add a hidden div with the full text so that searching works correctly
                sub_text += `<div class="is-hidden">${normalized_text}</div>`;
                // Add a view all text button
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

        renderSafetyData(data) {
            let text = '<ul class="is-capitalized">';

            Object.entries(data || {}).forEach(([field, value]) => {
                text += `<li><span class="has-text-weight-semibold">${this.fieldName(field)}</span>: ${this.formatSafetyValue(value)}</li>`;
            });
            text += '</ul>';

            return text;
        },

        rerunAltTextGeneration() {

        },

        /**
         * Add click event to the table
         * The click opens a modal with editable field for the allowed fields
         * Full description, alt text and transcript
         * It also allows click events on the rerun action button
         */
        bindTableEvents() {
            const dtApi = this.$refs.alt_text_table?.dt;

            if (!dtApi || this.altTextTableClickHandler) {
                return;
            }

            this.altTextTableClickHandler = (e) => {
                const action_fields = ['full_desc', 'alt_text', 'transcript'];

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
        this.uuid = location.pathname.split('/')[3];
        this.container_info = JSON.parse(localStorage.getItem(this.uuid));
    },

    mounted() {
        this.fetchTableItems();
        this.$nextTick(() => {
            this.editCell();
        });
    },

    beforeUnmount() {
        this.unbindTableEvents();
        localStorage.removeItem(this.uuid);
    }
}
</script>

<style>
#alt-text-viewer {
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
}
</style>