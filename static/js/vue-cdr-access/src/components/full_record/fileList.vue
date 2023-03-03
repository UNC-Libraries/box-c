<template>
    <div class="child-records">
        <h3>List of Items in This Work ({{ childCount }})</h3>
        <data-table id="child-files" class="responsive table is-striped is-bordered is-fullwidth"
                    :ajax="ajaxOptions"
                    :columns="columns"
                    :options="tableOptions">
            <thead>
            <tr>
                <th><span class="sr-only">Thumbnail for file</span></th>
                <th>Title</th>
                <th>File Type</th>
                <th>File Size</th>
                <th><span class="sr-only">View file</span></th>
                <th><span class="sr-only">Download file</span></th>
                <th v-if="editAccess"><span class="sr-only">Edit MODS</span></th>
            </tr>
            </thead>
        </data-table>
    </div>
</template>

<script>
import fileUtils from '../../mixins/fileUtils';
import DataTable from 'datatables.net-vue3'
import DataTablesLib from 'datatables.net-bm';

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
        workId: String,
    },

    data() {
        return {
            columns: [
                { data: 'Thumbnail for file' },
                { data: 'Title' },
                { data: 'File Type' },
                { data: 'File Size' },
                { data: 'View file' },
                { data: 'Download file' }
            ]
        }
    },

    computed: {
        ajaxOptions() {
            return  {
                url: `/listJson/${this.workId}?rows=10`,
                dataSrc: (d) => d.metadata,
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
                processing: true,
                serverSide: true,
                bLengthChange: false, // Remove option to show different number of results
                columnDefs: this.columnDefs,
                language: { search: '', searchPlaceholder: 'Search within this work' },
                order: [], // do not set initial sort in case there is member order
                rowCallback: (row, data) => {
                    if (this.showBadge(data).markDeleted) {
                        ///$(row).addClass('deleted');
                    }
                }
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
                            img = `<img class="data-thumb" loading="lazy" src="${row.thumbnail_url}"` +
                            ` alt="Thumbnail image for ${row.title}">`;
                        } else {
                            img = '<i class="fa fa-file default-img-icon data-thumb" title="Default thumbnail image"></i>';
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

                        return img
                    }, targets: 0
                },
                {
                    render: (data, type, row) => {
                        return `<a href="/record/${row.id}" aria-label="View ${row.title}">${row.title}</a>`;
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
                        return `<a href="/record/${row.id}" aria-label="View ${row.title}">` +
                            '<i class="fa fa-search-plus is-icon"' + ' title="View"></i></a>';
                    },
                    targets: 4
                },
                {
                    render: (data, type, row) => {
                        if (row.permissions.indexOf('viewOriginal') === -1) {
                            return '<i class="fa fa-download is-icon no-download" title="Download Unavailable"></i>';
                        }
                        return `<a href="/indexablecontent/${row.id}?dl=true" aria-label="Download ${row.title}">` +
                            ' <i class="fa fa-download is-icon" title="Download"></i></a>';
                    },
                    targets: 5
                }
            ];

            if (this.editAccess) {
                this.columns.push({ data: 'Edit MODS' });
                excluded_columns.push(6); // edit button

                // Add to orderable, searchable exclusions
                [0, 1].forEach((d) => column_defs[d].targets = excluded_columns);

                column_defs.push(
                    {
                        render: (data, type, row) => {
                            return `<a href="/admin/describe/${row.id}" aria-label="Edit ${row.title}">` +
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
        }
    }
}
</script>

<style lang="scss">
    @import 'datatables.net-bm';
    @import 'datatables.net-responsive-bm';

    .dataTables_filter {
        margin-top: -45px;
    }

    input[type=search] {
        margin-bottom: 15px;
        max-width: 300px;
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
</style>