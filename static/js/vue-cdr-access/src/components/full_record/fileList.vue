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
import fileDownloadUtils from '../../mixins/fileDownloadUtils';
import fileUtils from '../../mixins/fileUtils';
import DataTable from 'datatables.net-vue3'
import DataTablesLib from 'datatables.net-bm';
import 'datatables.net-buttons-bm';

DataTable.use(DataTablesLib);

export default {
    name: 'fileList',

    mixins: [fileDownloadUtils, fileUtils],

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
                },
                {
                    render: (data, type, row) => {
                        return this.downloadButtonHtml(row);
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
                            return `<a href="/admin/describe/${row.id}" aria-label="${this.ariaLabelText(row)}">` +
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
        ariaLabelText(brief_object) {
            return this.$t('full_record.view_title', { title: brief_object.title });
        },

        showNonImageDownload(brief_object) {
            return this.hasPermission(brief_object, 'viewOriginal') &&
                !brief_object.format.includes('Image');
        },

        showBadge(brief_object) {
            let markedForDeletion = false;
            let restrictedAccess = true;

            if (brief_object.status !== undefined) {
                const restrictions = brief_object.status.join(',').toLowerCase();
                markedForDeletion = /marked.*?deletion/.test(restrictions);
                restrictedAccess = brief_object.status.indexOf("Public Access") === -1;
            }

            return { markDeleted: markedForDeletion, restricted: restrictedAccess };
        },

        showDropdownList(e) {
            // Close any currently open dropdowns
            this.closeDropdownLists(e);

            if (e.target.id.startsWith('dcr-download')) {
                let drop_down = e.target.parentElement.parentElement.querySelector('.dropdown-menu');
                if (drop_down !== null) {
                    drop_down.setAttribute('aria-hidden', 'false');
                    drop_down.classList.add('show-list');
                }
            }
        },

        closeDropdownLists() {
            document.querySelectorAll('.show-list').forEach(element => {
                element.setAttribute('aria-hidden', 'true');
                element.classList.remove('show-list');
            });
        },

        downloadButtonHtml(brief_object) {
            if (this.showNonImageDownload(brief_object)) {
                return `<div class="actionlink download">
                            <a class="download button action" href="/content/${brief_object.id}?dl=true"><i class="fa fa-download"></i> ${this.$t('full_record.download')}</a>
                        </div>`;
            } else if (this.showImageDownload(brief_object)) {
                let html = `<div class="dropdown actionlink download image-download-options">
                <div class="dropdown-trigger">
                    <button id="dcr-download-${brief_object.id}" class="button download-images" aria-haspopup="true" aria-controls="dropdown-menu">
                    ${this.$t('full_record.download')} <i class="fas fa-angle-down" aria-hidden="true"></i>
                    </button>
                </div>
                <div class="dropdown-menu table-downloads" id="dropdown-menu" role="menu" aria-hidden="true">
                    <div class="dropdown-content">`;

                if (this.validSizeOption(brief_object, 800)) {
                   html += `<a href="${this.imgDownloadLink(brief_object.id, '800')}" class="dropdown-item">${this.$t('full_record.small') } JPG (800px)</a>`;
                }
                if (this.validSizeOption(brief_object, 1600)) {
                    html += `<a href="${this.imgDownloadLink(brief_object.id, '1600')}" class="dropdown-item">${this.$t('full_record.medium') } JPG (1600px)</a>`;
                }
                if (this.validSizeOption(brief_object, 2500)) {
                    html += `<a href="${this.imgDownloadLink(brief_object.id, '2500')}" class="dropdown-item">${this.$t('full_record.large') } JPG (2500px)</a>`;
                }


                html += `<a href="${this.imgDownloadLink(brief_object.id, 'full')}" class="dropdown-item">${this.$t('full_record.full_size')} JPG</a>`;
                html += '<hr class="dropdown-divider">';
                html += `<a href="/indexablecontent/${brief_object.id}?dl=true" class="dropdown-item">${this.$t('full_record.original_file')}</a>`;

                html += '</div>'
                html += '</div>'

                return html;
            } else {
                return `<div class="dropdown actionlink download image-download-options">
                            <button class="button download-images" title="${this.$t('full_record.download_unavailable')}" disabled>
                                <i class="fa fa-download"></i> ${this.$t('full_record.download')}
                            </button>
                        </div>`;
            }
        }
    },

    mounted() {
        document.addEventListener('click', this.showDropdownList);
        document.addEventListener('keyup', this.closeDropdownLists);
    },

    unmounted() {
        document.removeEventListener('click', this.showDropdownList);
        document.removeEventListener('keyup', this.closeDropdownLists);
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
            }

            .button[disabled] {
                background-color: #084b6b;
                color: white;
            }

            .fa-angle-down {
                pointer-events: none;
            }
        }
    }

    .show-list {
        display: block;
    }
</style>