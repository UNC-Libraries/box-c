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
        </data-table>
    </div>
</template>

<script>
import fileUtils from '../../mixins/fileUtils';
import DataTable from 'datatables.net-vue3'
import DataTablesLib from 'datatables.net-bm';
import 'datatables.net-buttons-bm';

DataTable.use(DataTablesLib);

export default {
    name: 'fileList',

    mixins: [fileUtils],

    components: {DataTable},

    props: {
        childCount: Number,
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
                { data: this.$t('full_record.download_file') }
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
                            console.log("button is pushed");
                            dt.data.sort = "";
                            dt.ajax.url(`/listJson/${this.workId}?rows=10`).load();
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

                        if ('thumbnail_url' in row) {
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
                        const view_title = this.$t('full_record.view_title', { title: row.title });
                        return `<a href="/record/${row.id}" aria-label="${view_title}">${row.title}</a>`;
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
                        const view_title = this.$t('full_record.view_title', { title: row.title });
                        const view = this.$t('full_record.view');
                        return `<a href="/record/${row.id}" aria-label="${view_title}">` +
                            ` <i class="fa fa-search-plus is-icon" title="${view}"></i></a>`;
                    },
                    targets: 4
                },
                {
                    render: (data, type, row) => {
                        if (row.permissions.indexOf('viewOriginal') === -1) {
                            const unavailable = this.$t('full_record.download_unavailable');
                            return `<i class="fa fa-download is-icon no-download" title="${unavailable}"></i>`;
                        }
                        const label_text = this.$t('full_record.download_title', { title: row.title });
                        const download = this.$t('full_record.download');
                        return `<a href="/indexablecontent/${row.id}?dl=true" aria-label="${label_text}">` +
                            ` <i class="fa fa-download is-icon" title="${download}"></i></a>`;
                    },
                    targets: 5
                }
            ];

            if (this.editAccess) {
                this.columns.push({ data: this.$t('full_record.mods') });
                excluded_columns.push(6); // edit button

                // Add to orderable, searchable exclusions
                [0, 1].forEach((d) => column_defs[d].targets = excluded_columns);

                column_defs.push(
                    {
                        render: (data, type, row) => {
                            const label_text = this.ariaLabelText(row.title);
                            return `<a href="/admin/describe/${row.id}" aria-label="${label_text}">` +
                                '<i class="fa fa-edit is-icon" title="Edit"></i></a>'
                        },
                        targets: 6
                    }
                );
            }

            return column_defs;
        }
    },

    methods: {
        showBadge(data) {
            let markedForDeletion = false;
            let restrictedAccess = true;

            if (data.status !== undefined) {
                const restrictions = data.status.join(',').toLowerCase();
                markedForDeletion = /marked.*?deletion/.test(restrictions);
                restrictedAccess = data.status.indexOf("Public Access") === -1;
            }

            return { markDeleted: markedForDeletion, restricted: restrictedAccess };
        },

        ariaLabelText(row) {
            return this.$t('full_record.edit_title', { title: row.title });
        }
    }
}
</script>

<style lang="scss">
    @import 'datatables.net-bm';
    @import 'datatables.net-buttons-bm';
    #data-display {
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
    }
</style>