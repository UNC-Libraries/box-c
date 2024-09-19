<!-- Teleport reports into #chompb-admin to get around issues with the admin app not being a Vue SPA -->
<template>
    <teleport to="#chompb-admin">
        <div class="cropping-report">
            <h2 class="chompb-ui has-text-weight-semibold is-size-3 has-text-centered">Cropping Report</h2>
            <div class="buttons has-addons">
                <span class="button is-success is-selected">
                    <a class="download" download="originals.csv" :href="downloadOriginals">
                        Original CSV
                    </a>
                </span>
                <span class="button">
                    <a class="download" download="corrections.csv" :href="downloadProblems">
                        Save Access Surrogate Mappings (with {{ problems.length }} corrections)
                    </a>
                </span>
            </div>
            <data-table @click.prevent="markAsProblem($event)" class="table is-striped is-bordered is-fullwidth"
                        :data="dataSet"
                        :columns="columns"
                        :options="tableOptions">
                <thead>
                <tr>
                    <th>Image</th>
                    <th>Path</th>
                    <th>Class</th>
                    <th>Confidence</th>
                    <th>Problem</th>
                    <th></th>
                </tr>
                </thead>
            </data-table>
        </div>
    </teleport>
</template>

<script>
import DataTable from 'datatables.net-vue3';
import DataTablesLib from 'datatables.net-bm';
import 'datatables.net-searchpanes-bm';
import 'datatables.net-select-bm'

DataTable.use(DataTablesLib);

export default {
    name: 'croppingReport',

    components: {DataTable},

    data() {
        return {
            columns: [
                {data: 'Image'},
                {data: 'Path'},
                {data: 'Class'},
                {data: 'Confidence'},
                {data: 'Problem'},
                {data: ''}
            ],
            problems: [],
            dataSet: []
        }
    },

    computed: {
        tableOptions() {
            return {
                columnDefs: this.columnDefs,
                searching: true,
                layout: {
                    top1: 'searchPanes',
                    topStart: 'info',
                    topEnd: {
                        search: {
                            placeholder: 'Search'
                        }
                    }
                },
                searchPanes: {
                    viewTotal: true,
                    columns: [2, 4],
                    layout: 'columns-2',
                    initCollapsed: true
                },
                language: { search: '' },
                order: [[1, 'asc']]
            }
        },

        columnDefs() {
            const excluded_columns = [0, 5];

            return [
                { orderable: false, targets: excluded_columns },
                { searchable: false, target: excluded_columns },
                {
                    render: (data, type, row) => {
                        return `<img src="${row.image}" alt="" role="presentation" />`;
                    }, targets: 0
                },
                {
                    render: (data, type, row) => {
                        return row.original;
                    }, targets: 1
                },
                {
                    render: (data, type, row) => {
                        return row.pred_class;
                    }, targets: 2
                },
                {
                    render: (data, type, row) => {
                        return row.pred_conf;
                    }, targets: 3
                },
                {
                    render: (data, type, row) => {
                        return row.problem ? "problem detected" : "";
                    }, targets: 4
                },
                {
                    render: (data, type, row) => {
                        return `<button class="button is-outlined incorrect" value="incorrect" data-path="${row.original}"` + `
                                data-predicted="${row.pred_class}">Mark incorrect</button>`;
                    }, targets: 5
                }
            ];
        },

        downloadOriginals() {
            let csvContent = "original_path,normalized_path,predicted_class,predicted_conf\n";
            csvContent += this.dataSet.map(d => {
                return `${d.original},${d.image},${d.pred_class},${d.pred_conf}`;
            }).join("\n");
            return  this.downloadResponse(csvContent);
        },

        downloadProblems() {
            let csvContent = "path,predicted_class,corrected_class\n"
            csvContent += this.problems.map(d => {
                return `${d.path},${d.predicted},${d.predicted === '1' ? '0' : '1'}`
            }).join("\n");
            return  this.downloadResponse(csvContent);
        }
    },

    methods: {
        downloadResponse(csv) {
            let data = new Blob([csv]);
            return URL.createObjectURL(data);
        },
        /**
         * We can't make updating button classes in the table reactive, so update them as we add/remove items from
         * the problem bin
         * @param e
         */
        markAsProblem(e) {
            if (e.target.classList.contains('incorrect')) {
                const image_path = e.target.dataset.path;
                const marked_as_problem = this.problems.findIndex(d => d.path === image_path);

                if (marked_as_problem === -1) {
                    this.problems.push({
                        path: image_path,
                        predicted: e.target.dataset.predicted
                    });
                    e.target.classList.add('is-danger');
                    e.target.classList.remove('is-outlined');
                } else {
                    this.problems.splice(marked_as_problem, 1);
                    e.target.classList.add('is-outlined');
                    e.target.classList.remove('is-danger');
                }
            }
        }
    }
}
</script>

<style lang="scss">
/* Seems to be a bug with datatables 2.x that if styles are scoped the import isn't picked up */
@import 'datatables.net-bm';
@import 'datatables.net-searchpanes-bm';
@import 'datatables.net-select-bm';

.cropping-report {
    width: 96%;
    margin: auto;

    div.dtsp-panesContainer {
        margin-bottom: 2.5rem;
        margin-left: 0;
        margin-right: 0;
        width: 100%;
    }

    h2 {
        margin: 25px;
    }

    .download {
        text-decoration: none;
        &:hover,
        &:focus {
            text-decoration: none;
        }
        color: black;
    }

    .is-success {
        .download {
            color: white;
        }
    }
}
</style>