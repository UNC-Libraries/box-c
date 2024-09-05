<!--
Injects chompb dashboard into web-admin-app/src/main/webapp/WEB-INF/jsp/report/chompb.jsp
without having to include the vue-admin js app again. It does this via Vue's teleport option
https://vuejs.org/guide/built-ins/teleport.html
-->
<template>
    <teleport to="#chompb-admin">
        <div id="chompb-preingest-ui">
            <h2 class="chompb-ui has-text-weight-semibold is-size-3 has-text-centered">Pre-ingest Projects</h2>
            <data-table v-if="dataSet.length > 0" @click="copyPath($event)" id="chompb-projects" class="table is-striped is-bordered is-fullwidth"
                        :data="dataSet"
                        :columns="columns"
                        :options="tableOptions">
                <thead>
                <tr>
                    <th>Chompb Project</th>
                    <th>Source</th>
                    <th>Status</th>
                    <th>Actions</th>
                </tr>
                </thead>
            </data-table>
            <p v-else>There are no pre-ingest projects to show</p>
            <div id="copy-msg" class="notification is-light" :class="copyMsgClass" v-if="copy_msg !== ''">{{ copy_msg }}</div>
        </div>
    </teleport>
</template>

<script>
import DataTable from 'datatables.net-vue3';
import DataTablesLib from 'datatables.net-bm';
import axios from "axios";
import isEmpty from "lodash.isempty";

DataTable.use(DataTablesLib);

export default {
    name: 'preIngest',

    components: {DataTable},

    data() {
        return {
            copy_error: false,
            copy_msg: '',
            columns: [
                { data: 'Chompb Project' },
                { data: 'Source' },
                { data: 'Status' },
                { data: 'Actions' }
            ],
            dataSet: []
        }
    },

    computed: {
        tableOptions() {
            return {
                ajax: '/admin/chompb/project',
                bAutoWidth: false,
                columnDefs: this.columnDefs,
                language: { search: '', searchPlaceholder: 'Filter projects' }
            }
        },

        columnDefs() {
            const excluded_columns = [3];

            return [
                { orderable: false, targets: excluded_columns },
                { searchable: false, target: excluded_columns },
                {
                    render: (data, type, row) => {
                        return row.name;
                    }, targets: 0
                },
                {
                    render: (data, type, row) => {
                        return row.projectSource;
                    }, targets: 1
                },
                {
                    render: (data, type, row) => {
                        return `<span class="is-capitalized">${row.status.replaceAll('_', ' ')}</span>`
                    }, targets: 2
                },
                {
                    render: (data, type, row) => {
                        let actions = [`<a id="${row.name}" href="#">Copy Path</a>`];
                        if (row.allowedActions.length === 0) {
                            return actions;
                        }
                        row.allowedActions.forEach((d) => {
                            actions.push(`<a class="is-capitalized" href="/admin/${d}">${d.replaceAll('_', ' ')}</a>`);
                        });
                        return actions.join(' ');
                    }, targets: 3
                }
            ];
        },

        copyMsgClass() {
            return this.copy_error ? 'is-danger' : 'is-success';
        }
    },

    methods: {
        retrieveProjects() {
            axios.get('/admin/chompb/project').then((response) => {
               // if (!isEmpty(response.data)) {
                    this.dataSet = [{
                        "name": "file_source_test",
                        "cdmCollectionId": null,
                        "createdDate": "2024-07-16T15:20:08.579384Z",
                        "creator": "bbpennel",
                        "exportedDate": null,
                        "indexedDate": "2024-08-15T12:38:28.662654Z",
                        "destinationsGeneratedDate": null,
                        "sourceFilesUpdatedDate": "2024-08-14T17:06:34.182417Z",
                        "accessFilesUpdatedDate": null,
                        "groupMappingsUpdatedDate": "2024-08-14T17:42:49.626664Z",
                        "groupMappingsSyncedDate": null,
                        "descriptionsExpandedDate": null,
                        "sipsGeneratedDate": null,
                        "sipsSubmitted": [],
                        "hookId": null,
                        "collectionNumber": null,
                        "cdmEnvironmentId": null,
                        "bxcEnvironmentId": "test",
                        "projectSource": "files",
                        "status": "sources_mapped",
                        "allowedActions": ["color_bar_crop", "color_bar_report"],
                        "projectPath": "/opt/data/cdm_migrations/file_source_test"

                    }]
              //  }
            }).catch((error) => {
                console.log(error);
            });
        },

        clearCopyMessage() {
            setTimeout(() => {
                this.copy_error = false;
                this.copy_msg = '';
            }, 5000);
        },

        async copyPath(e) {
            const project_id = e.target.id;
            if (project_id === '') {
                return true;
            }

            const project = this.dataSet.find(d => d.name === project_id)
            if (project !== undefined) {
                e.preventDefault();

                try {
                    await navigator.clipboard.writeText(project.projectPath);
                    this.copy_error = false;
                    this.copy_msg = 'Project path copied to the clipboard!';
                } catch (err) {
                    this.copy_error = true;
                    this.copy_msg = 'Unable to copy project path the to clipboard!';
                    console.error('Failed to copy: ', err);
                }
                this.clearCopyMessage();
            }
        }
    },

    beforeMount() {
       // this.retrieveProjects();
    }
}
</script>

<style scoped lang="scss">
    @import 'datatables.net-bm';

    #chompb-preingest-ui {
        width: 96%;
        margin: 25px auto;
    }

    p {
        text-align: center;
        margin-top: 50px;
    }

    #copy-msg {
        padding: 15px;
        position: absolute;
        right: 25px;
        top: 0;
        width: auto;
    }
</style>