<template>
    <div id="staff-roles">
        <h1>Set Staff Roles</h1>
        <table class="border" v-if="current_staff_roles.inherited.length > 0">
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
        <table v-if="updated_staff_roles.length > 0">
            <tr v-for="(updated_staff_role, index) in updated_staff_roles" :key="index">
                <td class="border">{{ updated_staff_role.principal }}</td>
                <td class="border">
                    <staff-roles-select
                            :user="updated_staff_role"
                            @staff-role-update="updateUserRole">
                    </staff-roles-select>
                </td>
                <td class="btn">
                    <button v-if="updated_staff_role.type === 'new'" class="btn-remove" @click="removeUser(index)">Remove</button>
                    <button v-else class="btn-revert" @click="removeUser(index)">Undo Add</button>
                </td>
            </tr>
        </table>
        <staff-roles-form @add-user="updateUserList"></staff-roles-form>
        <ul>
            <li><button @click="setRoles" type="submit">Save Changes</button></li>
            <li><button class="cancel" type="reset">Cancel</button></li>
        </ul>
    </div>
</template>

<script>
    import staffRolesForm from "./staffRolesForm";
    import staffRolesSelect from "./staffRolesSelect";
    import staffRoleList from "../mixins/staffRoleList";
    import get from 'axios';
    import post from 'axios';

    export default {
        name: 'staffRoles',

        components: {
            staffRolesForm,
            staffRolesSelect
        },

        mixins: [staffRoleList],

        props: {
            uuid: String
        },

        data() {
            return {
                current_staff_roles: { inherited: [], assigned: [] },
                response_message: '',
                updated_staff_roles: []
            }
        },

        methods: {
            getRoles() {
                get(`/get/acl/staff/${this.uuid}`).then((response) => {
                    this.current_staff_roles = response.data;
                    this.updated_staff_roles = this.current_staff_roles
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
                this.updated_staff_roles.splice(index, 1);
            },

            updateUserList(user) {
                this.updated_staff_roles.push(user);
            },

            updateUserRole(user) {
                let user_index = this.updated_staff_roles.findIndex((u) => u.principal === user.principal);

                if (user_index !== -1) {
                    this.updated_staff_roles[user_index].role = user.role;
                }
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

        .border, .border th, .border td {
            border: $border-style;
            border-collapse: collapse;
            padding: 5px 10px;
        }

        th, .cancel {
            background-color: gray;
        }

        table {
            margin-bottom: 25px;
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
                    font-size: 14px;
                }
            }
        }

        select {
            border: 1px;
            height: 34px;
            width: 100%;
        }

        button {
            border: none;
            padding: 5px 10px;
        }

        .btn {
            text-align: left;
        }

        .btn-remove {
            background-color: red;
        }

        .btn-revert {
            background-color: gray;
        }
    }
</style>