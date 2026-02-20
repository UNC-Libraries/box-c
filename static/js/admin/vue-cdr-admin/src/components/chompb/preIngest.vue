<!--
Injects chompb dashboard into web-admin-app/src/main/webapp/WEB-INF/jsp/report/chompb.jsp
without having to include the vue-admin js app again. It does this via Vue's teleport option
https://vuejs.org/guide/built-ins/teleport.html
-->
<template>
    <teleport to="#chompb-admin">
        <div id="chompb-preingest-ui">
            <h2 class="chompb-ui has-text-weight-semibold is-size-3 has-text-centered">Pre-ingest Projects</h2>
            <data-table id="chompb-projects" class="table is-striped is-bordered is-fullwidth"
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
                <template #actions="props">
                    <a @click.prevent="copyPath(props.rowData.projectPath)" href="#">Copy Path</a>
                    <template v-for="actionInfo in listAllowedActions(props.rowData)">
                        <a v-if="!actionInfo.disabled"
                           @click.prevent="performAction(actionInfo, props.rowData)">
                            {{ actionInfo.label }}
                        </a>
                        <span v-else>
                            {{ actionInfo.label }}
                        </span>
                    </template>
                </template>
            </data-table>
            <div id="copy-msg" class="notification is-light" :class="copyMsgClass" v-if="copy_msg !== ''">{{ copy_msg }}</div>
        </div>
    </teleport>
</template>

<script>
import DataTable from 'datatables.net-vue3';
import DataTablesCore from 'datatables.net-bm';

DataTable.use(DataTablesCore);

export default {
    name: 'preIngest',

    components: {DataTable},

    data() {
        return {
            copy_error: false,
            copy_msg: '',
            columns: [
                { data: 'projectProperties.name', title: 'Chompb Project' },
                { title: 'Source',
                    data: function (row) {
                        return (row.projectProperties.projectSource) ? row.projectProperties.projectSource : '';
                    }
                },
                { data: 'status', title: 'Status' },
                { data: null, title: 'Actions',
                    render: {
                        display: '#actions'
                    }
                }
            ],
            dataSet: []
        }
    },

    computed: {
        tableOptions() {
            return {
                ajax: {
                    url: '/admin/chompb/listProjects',
                    dataSrc: (data) => {
                        this.dataSet = data;
                        return data;
                    }
                },
                bAutoWidth: false,
                columnDefs: this.columnDefs,
                language: { search: '', searchPlaceholder: 'Filter projects' }
            }
        },

        columnDefs() {
            return [
                { orderable: false, targets: [3] },
                { searchable: false, target: [3] }
            ]
        },

        copyMsgClass() {
            return this.copy_error ? 'is-danger' : 'is-success';
        },

        actionMapping() {
            return {
                'velocicroptor_action': {
                    'jobName': 'velocicroptor',
                    'action': 'action',
                    'label': 'Crop color bars',
                    'confirm': true,
                    'confirmMessage': 'Are you sure you want to crop color bars for this project?',
                    'disabled': false,
                    'method': 'post'
                },
                'velocicroptor_processing_results': {
                    'jobName': 'velocicroptor',
                    'action': 'processing_results',
                    'label': 'View crop report',
                    'confirm': false,
                    'disabled': false,
                    'method': 'link'
                },
                'velocicroptor_pending': {
                    'jobName': 'velocicroptor',
                    'action': 'pending',
                    'label': 'Crop in progress',
                    'confirm': false,
                    'disabled': true
                }
            };
        }
    },

    methods: {
        async performAction(action_info, row_data) {
            let projectName = row_data.projectProperties.name;
            // Action requires confirmation, exiting early if the user cancels
            if (action_info.confirm && !confirm(action_info.confirmMessage)) {
                return;
            }
            if (action_info.action === 'action') {
                Object.assign(row_data.processingJobs, { velocicroptor: { status: 'pending' } });
            }
            let actionUrl = `/admin/chompb/project/${projectName}/${action_info.action}/${action_info.jobName}`;
            if (action_info.method === 'post' || action_info.method === 'get') {
                try {
                    const response = await fetch(actionUrl, {
                        method: action_info.method.toUpperCase()
                    });

                    if (!response.ok) {
                        throw new Error('Network response was not ok');
                    }

                    console.log("Successfully triggered action", actionUrl);
                    this.copy_error = false;
                    this.copy_msg = `"${action_info.label}" action successfully triggered for project: ${projectName}`;
                    this.clearCopyMessage();
                } catch (error) {
                    this.copy_error = true;
                    this.copy_msg = `Error encountered while performing action" ${action_info.label}" for project: ${projectName}`;
                    console.log("Error encountered while performing action", error);
                    this.clearCopyMessage();
                }
            } else if (action_info.method === 'link') {
                this.$router.push(actionUrl);
            }
        },

        getActionMapping(action_name) {
            return structuredClone(this.actionMapping[action_name]);
        },

        listAllowedActions(row_data) {
            let resultActions = [];
            let processingJobs = row_data.processingJobs;
            if (row_data.allowedActions.indexOf('crop_color_bars') > -1) {
                let processingResult = processingJobs['velocicroptor'];
                let processingStatus = processingResult ? processingResult.status : '';

                // if status is completed, then allow processing again and allow viewing the report
                if (processingStatus === 'completed') {
                    resultActions.push(this.getActionMapping('velocicroptor_action'));
                    resultActions.push(this.getActionMapping('velocicroptor_processing_results'));
                } else if (processingStatus === 'pending') {
                    // if status is pending, then display "Cropping in progress"
                    resultActions.push(this.getActionMapping('velocicroptor_pending'));
                } else {
                    // if no status or any other status, then allow processing
                    resultActions.push(this.getActionMapping('velocicroptor_action'));
                }
            }
            return resultActions;
        },

        clearCopyMessage() {
            setTimeout(() => {
                this.copy_error = false;
                this.copy_msg = '';
            }, 5000);
        },

        async copyPath(project_path) {
            try {
                await navigator.clipboard.writeText(project_path);
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
}
</script>

<style lang="scss">
    /* Seems to be a bug with datatables 2.x that if styles are scoped the import isn't picked up */
    @import 'datatables.net-bm';

    #chompb-preingest-ui {
        width: 96%;
        margin: 25px auto;

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
    }
</style>