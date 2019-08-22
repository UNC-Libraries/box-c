<template>
    <div id="staff-permissions">
        <h1>Set Staff Permissions</h1>
        <table>
            <thead>
            <tr>
                <th>Staff</th>
                <th>Permission</th>
                <th>Inherited from</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="staff_permission in staff_permissions">
                <td>{{ staff_permission.user }}</td>
                <td>{{ staff_permission.permission }}</td>
                <td>{{ staff_permission.from }}</td>
            </tr>
            </tbody>
        </table>
    </div>
</template>

<script>
    export default {
        name: 'staffPermissions',

        data() {
            return {
                staff_permissions: []
            }
        }
    }
</script>

<style scoped lang="scss">
    table, th, td {
        border: 1px solid black;
        border-collapse: collapse;
    }
</style>