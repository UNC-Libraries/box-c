<!--
Loads associated files of an object as a DataTable.
Note, :key on <data-table> must be something that changes on update,
so the table updates its contents when switching between records.
The datatables component only watches for changes on its "data" prop, which we don't use. So we use "key" to
force it to reload
-->
<template>
    <div class="child-records table-container" id="data-display">
        <h3>{{ $t('full_record.item_list') }} ({{ childCount }})</h3>
        <data-table :key="workId" id="child-files" class="table is-striped is-bordered is-fullwidth"
                    :ajax="ajaxOptions"
                    :columns="columns"
                    :options="tableOptions">
            <thead>
            <tr>
                <th><span class="sr-only">{{ $t('full_record.thumbnail') }}</span></th>
                <th>{{ $t('full_record.title') }}</th>
                <th>{{ $t('full_record.file_type') }}</th>
                <th>{{ $t('full_record.filesize') }}</th>
                <th><span class="sr-only">{{ $t('full_record.view_file') }}</span></th>
                <th><span class="sr-only">{{ $t('full_record.download_file') }}</span></th>
                <th v-if="editAccess"><span class="sr-only">{{ $t('full_record.mods') }}</span></th>
            </tr>
            </thead>
            <template #downloads="props">
                <download-options :t="$t" :record-data="props.rowData"></download-options>
            </template>
        </data-table>
    </div>
</template>

<script>
import DownloadOptions from "@/components/full_record/downloadOptions.vue";
import fileUtils from '../../mixins/fileUtils';
import fullRecordUtils from '../../mixins/fullRecordUtils';
import DataTable from 'datatables.net-vue3'
import DataTablesLib from 'datatables.net-bm';
import 'datatables.net-buttons-bm';

DataTable.use(DataTablesLib);

export default {
    name: 'fileList',

    mixins: [fileUtils, fullRecordUtils],

    components: {DownloadOptions, DataTable},

    props: {
        childCount: Number,
        downloadAccess: {
            default: false,
            type: Boolean
        },
        editAccess: {
            default: false,
            type: Boolean
        },
        resourceType: {
            default: 'Work',
            type: String
        },
        workId: String
    },

    data() {
        return {
            columns: [
                { data: this.$t('full_record.thumbnail') },
                { data: this.$t('full_record.title') },
                { data: this.$t('full_record.file_type') },
                { data: this.$t('full_record.filesize') },
                { data: this.$t('full_record.view_file') },
                { data: null, width: '120px', render: {
                        display: '#downloads'
                    }
                }
            ]
        }
    },

    computed: {
        // Datatables expects dataSrc to return an array
        // File objects don't have any child metadata, so wrap the file object in an array
        ajaxOptions() {
            return  {
                url: `/listJson/${this.workId}?rows=10`,
                dataSrc: (d) => this.resourceType === 'Work' ? d.metadata : [d.container],
                data: (d) => {
                    const sorts = ['title', 'fileFormatDescription', 'fileSize'];
                    const sortOrder = {'asc': 'normal', 'desc': 'reverse'};
                    d.anywhere = d.search['value'];
                    d.length = 10;
                    d.rollup = false;
                    if (d.order[0] !== undefined) {
                        d.sort = sorts[d.order[0]['column'] - 1] + ',' + sortOrder[d.order[0]['dir']];
                    }
                },
                dataFilter: (data) => {
                    let json = JSON.parse(data);
                    json.recordsTotal = json.resultCount;
                    json.recordsFiltered = json.resultCount;
                    return JSON.stringify(json);
                }
            }
        },

        tableOptions() {
            return {
                serverSide: true,
                bAutoWidth: false,
                bLengthChange: false, // Remove option to show different number of results
                columnDefs: this.columnDefs,
                language: { search: '', searchPlaceholder: this.$t('full_record.search_within') },
                order: [], // do not set initial sort in case there is member order
                rowCallback: (row, data) => {
                    if (this.showBadge(data).markDeleted) {
                        row.classList.add('deleted');
                    }
                },
                dom: 'Bfrtip',
                buttons: [
                    {
                        text: 'Clear Sort',
                        action: function (e, dt, node, config) {
                            dt.data().order().length = 0;
                            dt.ajax.reload();
                        }
                    }
                ]
            }
        },

        columnDefs() {
            const excluded_columns = [0, 4, 5];

            let column_defs = [
                { orderable: false, targets: excluded_columns },
                { searchable: false, target: excluded_columns },
                { type: 'file-size', targets: 3 },
                {
                    render: (data, type, row) => {
                        let img;

                        if ('thumbnail_url' in row && this.hasPermission(row,'viewAccessCopies')) {
                            const thumbnail_title = this.$t('full_record.thumbnail_title', { title: row.title })
                            img = `<img class="data-thumb" loading="lazy" src="${row.thumbnail_url}"` +
                                ` alt="${thumbnail_title}">`;
                        } else {
                            const thumbnail_default = this.$t('full_record.thumbnail_default');
                            img = `<i class="fa fa-file default-img-icon data-thumb" title="${thumbnail_default}"></i>`;
                        }

                        const trashBadge = this.showBadge(row).markDeleted;
                        const lockBadge = this.showBadge(row).restricted;

                        if (trashBadge || lockBadge) {
                            let whichBadge = '';

                            if (trashBadge) {
                                whichBadge = 'trash';
                            } else if (lockBadge) {
                                whichBadge = 'lock';
                            }

                            img += `<div class="thumbnail-badge thumbnail-badge-${whichBadge}">` +
                                '<div class="fa-stack">' +
                                '<i class="fas fa-circle fa-stack-2x background"></i>' +
                                `<i class="fas fa-${whichBadge} fa-stack-1x foreground"></i>` +
                                '</div>' +
                                '</div>';
                        }

                        return img;
                    }, targets: 0
                },
                {
                    render: (data, type, row) => {
                        return `<a href="/record/${row.id}" aria-label="${this.ariaLabelText(row)}">${row.title}</a>`;
                    }, targets: 1
                },
                {
                    render: (data, type, row) => {
                        return this.getFileType(row);
                    }, targets: 2
                },
                {
                    render: (data, type, row) => {
                        return this.getOriginalFileValue(row.datastream, 'file_size');
                    }, targets: 3
                },
                {
                    render: (data, type, row) => {
                        const view = this.$t('full_record.view');
                        const aria_title = this.$t('full_record.edit_title', { title: row.title });
                        return `<a href="/record/${row.id}" aria-label="${aria_title}">` +
                            ` <i class="fa fa-search-plus is-icon" title="${view}"></i></a>`;
                    },
                    targets: 4
                }
            ];

            if (this.editAccess) {
                // Check for the correct column number, in the unlikely event a user has edit access, but not download access
                const column_number = 6;
                this.columns.push({ data: this.$t('full_record.mods') });
                excluded_columns.push(column_number); // edit button

                // Add to orderable, searchable exclusions
                [0, 1].forEach((d) => column_defs[d].targets = excluded_columns);

                column_defs.push(
                    {
                        render: (data, type, row) => {
                            return `<a href="/admin/describe/${row.id}" aria-label="${this.ariaLabelText(row)}">` +
                                '<i class="fa fa-edit is-icon" title="Edit"></i></a>'
                        },
                        targets: column_number
                    }
                );
            }

            return column_defs;
        }
    },

    methods: {
        ariaLabelText(brief_object) {
            return this.$t('full_record.view_title', { title: brief_object.title });
        },

        showNonImageDownload(brief_object) {
            return this.hasPermission(brief_object, 'viewOriginal') &&
                !brief_object.format.includes('Image');
        },

        showBadge(brief_object) {
            return { markDeleted: this.markedForDeletion(brief_object), restricted: this.isRestricted(brief_object) };
        }
    }
}
</script>

<style lang="scss">
    @import 'datatables.net-bm';
    @import 'datatables.net-buttons-bm';
    #data-display {
        overflow: visible;

        .dataTables_wrapper {
            margin: 5px;
        }

        .dataTables_filter {
            margin-top: -45px;
        }

        input[type=search] {
            margin-bottom: 15px;
            min-width: 225px;
        }

        #child-files {
            border: none;
            margin-bottom: 20px;
        }

        ul.pagination-list {
            justify-content: flex-end;

            li {
                text-indent: 0 !important;
                margin: 0 !important;
            }
        }

        .pagination-link.is-current {
            background-color: #1A698C;;
            border-color: #1A698C;
        }

        .dtr-details {
            text-align: left;
            li {
                margin: 0;
                text-indent: 0;
            }
        }

        tr.deleted {
            a.dropdown-item {
                color: black
            }
        }

        .actionlink {
            margin: 0;

            a.action {
                display: flex;
            }

            a.dropdown-item {
                padding-left: 0;
                padding-right: 0;
                text-indent: 10px;
            }

            .fa-download {
                margin-right: 5px;
            }

            a.download {
                padding: 0 10px;
            }

            .button {
                font-size: .9rem;
                padding: 0 10px;
                height: 2rem;
            }

            .button[disabled] {
                background-color: #084b6b;
                color: white;
            }
        }
    }

    .show-list {
        display: block;
    }
</style>