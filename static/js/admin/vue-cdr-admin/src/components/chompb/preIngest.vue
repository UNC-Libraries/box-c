<template>
    <div id="chompb-preingest-ui">
        <h2>Pre-ingest Projects</h2>
        <data-table id="child-files" class="table is-striped is-bordered is-fullwidth"

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
    </div>
</template>

<script>
import DataTable from 'datatables.net-vue3'
import DataTablesLib from 'datatables.net-bm';
import 'datatables.net-buttons-bm';

DataTable.use(DataTablesLib);

export default {
    name: 'preIngest',

    components: {DataTable},

    data() {
        return {
            columns: [
                { data: 'Chompb Project' },
                { data: 'Source' },
                { data: 'Status' },
                { data: 'Actions' }
            ]
        }
    },

    computed: {
        tableOptions() {
            return {
                serverSide: true,
                bAutoWidth: false,
                columnDefs: this.columnDefs,
                language: { search: '', searchPlaceholder: 'Filter projects' },
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
    }
}
</script>

<style scoped lang="scss">
    @import 'datatables.net-bm';
    @import 'datatables.net-buttons-bm';
</style>