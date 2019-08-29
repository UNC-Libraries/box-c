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
        <table class="assigned">
            <tr v-if="updated_staff_roles.length > 0"  v-for="(updated_staff_role, index) in updated_staff_roles" :key="index">
                <td class="border">{{ updated_staff_role.principal }}</td>
                <td class="border select-box">
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
            <staff-roles-form @add-user="updateUserList" @form-error="updateErrorMsg"></staff-roles-form>
        </table>
        <p class="message" :class="{ error: is_error_message }">{{ response_message }}</p>
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
    import axios from 'axios';

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
                is_error_message: false,
                response_message: '',
                updated_staff_roles: []
            }
        },

        methods: {
            getRoles() {
                axios.get(`/services/api/acl/staff/${this.uuid}`).then((response) => {
                    this.current_staff_roles = response.data;
                    this.updated_staff_roles = this.current_staff_roles.assigned;
                }).catch((error) => {
                    this.is_error_message = true;
                    this.response_message = `Unable load current staff roles for: ${this.uuid}`;
                    console.log(error);
                });
            },

            setRoles() {
                axios({
                    method: 'put',
                    url: `/services/api/edit/acl/staff/${this.uuid}`,
                    data: JSON.stringify(this.updated_staff_roles),
                    headers: {'content-type': 'application/json; charset=utf-8'}
                }).then((response) => {
                    this.is_error_message = false;
                    this.response_message = `Staff roles successfully updated for: ${this.uuid}`;
                }).catch((error) => {
                    this.is_error_message = true;
                    this.response_message = `Unable to update staff roles for: ${this.uuid}`;
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
            },

            updateErrorMsg(msg) {
                this.is_error_message = true;
                this.response_message = msg;
            }
        },

        mounted() {
            this.getRoles();
        }
    }
</script>

<style scoped lang="scss">
    $border-style: 1px solid lightgray;

    #staff-roles {
        margin: 0 15px;
        text-align: left;

        table {
            border-bottom: none;
            margin-bottom: 0;
        }

        th, .cancel {
            background-color: gray;
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

        button {
            border: none;
            padding: 5px 10px;
        }

        .btn-remove {
            background-color: red;
        }

        .btn-revert {
            background-color: gray;
        }

        .message {
            height: 17px;
            margin-top: 15px;
        }

        .error {
            color: red;
        }
    }
</style>