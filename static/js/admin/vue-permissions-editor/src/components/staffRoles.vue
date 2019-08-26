<template>
    <div id="staff-roles">
        <h1>Set Staff Roles</h1>
        <table v-if="current_staff_roles.inherited.length > 0">
            <thead>
            <tr>
                <th>Staff</th>
                <th>Permission</th>
                <th>Inherited from</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="inherited_staff_permission in current_staff_roles.inherited">
                <td>{{ inherited_staff_permission.principal }}</td>
                <td>{{ inherited_staff_permission.role }}</td>
                <td>Inherited</td>
            </tr>
            </tbody>
        </table>
        <p v-else>There are no inherited staff permissions at this level</p>

        <h4>Add or remove staff permissions</h4>
        <table v-if="current_staff_roles.assigned.length > 0">
            <tr v-for="(assigned_staff_permission, index) in current_staff_roles.assigned" :key="index">
                <td>{{ assigned_staff_permission.principal }}</td>
                <td>{{ assigned_staff_permission.role }}</td>
                <td>
                    <button class="btn-remove" @click="removeUser(index)">Remove</button>
                </td>
            </tr>
        </table>
        <staff-roles-form :current-roles="current_staff_roles.assigned" @add-user="updateUserList"></staff-roles-form>
        <ul>
            <li><button @click="setRoles" type="submit">Save Changes</button></li>
            <li><button class="cancel" type="reset">Cancel</button></li>
        </ul>
    </div>
</template>

<script>
    import staffRolesForm from "./staffRolesForm";
    import get from 'axios';
    import post from 'axios';

    export default {
        name: 'staffRoles',

        components: {
            staffRolesForm
        },

        props: {
            uuid: String
        },

        data() {
            return {
                current_staff_roles: { inherited: [], assigned: [] },
                response_message: '',
            }
        },

        methods: {
            getRoles() {
                get(`/get/acl/staff/${this.uuid}`).then((response) => {
                    this.current_staff_roles = response.data;
                }).catch((error) => {
                    this.response_message = `Unable load current staff roles for ${this.uuid}`;
                    console.log(error);
                });
            },

            setRoles() {
                post(`/edit/acl/staff/${this.uuid}`, this.current_staff_roles.assigned).then((response) => {
                    this.response_message = `Staff roles successfully updated for ${this.uuid}`;
                }).catch((error) => {
                    this.response_message = `Unable to update staff roles for ${this.uuid}`;
                    console.log(error);
                });
            },

            removeUser(index) {
                this.current_staff_roles.assigned = this.current_staff_roles.assigned.splice(index, 1);
            },

            updateUserList(user) {
                this.current_staff_roles.assigned.push(user);
            }
        },

        mounted() {
          //  this.getRoles();
        }
    }
</script>

<style scoped lang="scss">
    $border-style: 1px solid lightgray;

    #staff-roles {
        margin: 0 15px;
        text-align: left;

        table, th, td {
            border: $border-style;
            border-collapse: collapse;
            padding: 5px 10px;
        }

        th, .cancel {
            background-color: gray;
        }

        table {
            text-align: center;
            width: 100%;
        }

        h4 {
            margin-bottom: 10px;
            margin-top: 25px;
        }

        ul {
            border-top: $border-style;
            list-style-type: none;
            margin-top: 25px;
            padding-top: 20px;
            text-align: center;

            li {
                display: inline;

                button {
                    border: none;
                    font-size: 14px;
                    padding: 5px 10px;
                }
            }
        }
    }
</style>