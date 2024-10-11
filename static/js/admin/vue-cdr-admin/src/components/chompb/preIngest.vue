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
                    <template v-for="action in props.rowData.allowedActions">
                        <a class="is-capitalized" @click.prevent="actionPath(action, props.rowData.projectProperties.name)">
                            {{ capitalizeAction(action) }}
                        </a>
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
        }
    },

    methods: {
        actionPath(action_type, action_name) {
            return this.$router.push(`/admin/chompb/${action_type}/${action_name}`);
        },

        clearCopyMessage() {
            setTimeout(() => {
                this.copy_error = false;
                this.copy_msg = '';
            }, 4000);
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
        },

        /**
         * For some reason the first word in an action doesn't capitalize correctly
         * using CSS, though subsequent words do. So, just up case the first letter in the string.
         * @param action
         * @returns {*}
         */
        capitalizeAction(action) {
            let text = action.replaceAll('_', ' ');
            return text.charAt(0).toUpperCase() + text.slice(1);
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