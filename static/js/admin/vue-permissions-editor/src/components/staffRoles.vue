<template>
    <div id="staff-roles">
        <h1 v-if="canSetPermissions">Set Staff Permissions</h1>
        <h1 v-else>Inherited Staff Permissions</h1>

        <table class="border inherited-permissions" v-if="current_staff_roles.inherited !== undefined && current_staff_roles.inherited.length > 0">
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
                <td>{{ containerName }}</td>
            </tr>
            </tbody>
        </table>
        <p v-else>There are no inherited staff permissions.</p>

        <div class="assigned" v-if="canSetPermissions">
            <div>
                <h4>Add or remove staff permissions</h4>
                <div @click="showDescriptions" class="info">?</div>
            </div>
            <transition name="slide">
            <div v-if="display_descriptions" class="role-description">
                <ul id="role-list">
                    <li><strong>can Access</strong> - Can view/download restricted objects.</li>
                    <li><strong>can Ingest</strong> - Can ingest new Works or files along with any supporting metadata files.</li>
                    <li><strong>can Describe</strong> - Can edit individual MODS descriptions, can perform bulk MODS export and import.</li>
                    <li><strong>can Manage</strong> - Can arrange objects, can change patron access, can embargo, can mark for deletion, can ingest, can describe, can bulk update description.</li>
                    <li><strong>is Unit Owner</strong> - Can grant or remove staff roles, can create collections, can destroy objects, and all rights from "can Manage".</li>
                </ul>
            </div>
            </transition>
            <table class="assigned-permissions">
                <tr v-if="updated_staff_roles.length > 0"  v-for="(updated_staff_role, index) in updated_staff_roles" :key="index">
                    <td class="border" :class="{'marked-for-deletion': checkUserRemoved(updated_staff_role)}">{{ updated_staff_role.principal }}</td>
                    <td class="border select-box size" :class="{'marked-for-deletion': checkUserRemoved(updated_staff_role)}">
                        <staff-roles-select
                                :container-type="containerType"
                                :are-deleted="deleted_users"
                                :user="updated_staff_role"
                                @staff-role-update="updateUserRole">
                        </staff-roles-select>
                    </td>
                    <td class="btn">
                        <button v-if="updated_staff_role.type === 'new'" class="btn-revert" @click="fullyRemoveUser(index)">Undo Add</button>
                        <button v-else-if="checkUserRemoved(updated_staff_role)"
                                class="btn-revert"
                                @click="revertRemoveUser(updated_staff_role)">Undo Remove</button>
                        <button v-else class="btn-remove" @click="markUserForDeletion(index)">Remove</button>
                    </td>
                </tr>
                <staff-roles-form
                        :container-type="containerType"
                        :is-submitting="is_submitting"
                        :is-canceling="is_closing_modal"
                        @username-set="addUserFilledOut"
                        @add-user="updateUserList"
                        @form-error="updateErrorMsg"></staff-roles-form>
            </table>
            <p class="message" :class="{error: is_error_message}">{{ response_message }}</p>
        </div>
        <p class="no-updates-allowed" v-else>Go to previous level(s) to modify the staff permission settings.</p>

        <ul>
            <li v-if="canSetPermissions">
                <button id="is-submitting"
                        type="submit"
                        @click="setSubmitting"
                        :class="{'btn-disabled': is_submitting}"
                        :disabled="is_submitting">Save Changes</button>
            </li>
            <li><button @click="showModal" id="is-canceling" class="cancel" type="reset">Cancel</button></li>
        </ul>
    </div>
</template>

<script>
    import staffRolesForm from "./staffRolesForm";
    import staffRolesSelect from "./staffRolesSelect";
    import staffRoleList from "../mixins/staffRoleList";
    import axios from 'axios';
    import cloneDeep from 'lodash.clonedeep';
    import isEmpty from 'lodash.isempty';

    export default {
        name: 'staffRoles',

        components: {
            staffRolesForm,
            staffRolesSelect
        },

        mixins: [staffRoleList],

        props: {
            alertHandler: Object,
            changesCheck: Boolean,
            containerName: String,
            containerType: String,
            title: String,
            uuid: String
        },

        data() {
            return {
                current_staff_roles: { inherited: [], assigned: [] },
                deleted_users: [],
                is_closing_modal: false,
                is_error_message: true,
                is_submitting: false,
                response_message: '',
                unsaved_changes: false,
                updated_staff_roles: []
            }
        },

        watch: {
            changesCheck(check) {
                if (check) {
                    this.showModal();
                }
            }
        },

        computed: {
            canSetPermissions() {
                return ['AdminUnit', 'Collection'].includes(this.containerType);
            }
        },

        methods: {
            getRoles() {
                axios.get(`/services/api/acl/staff/${this.uuid}`).then((response) => {
                    if (!isEmpty(response.data)) {
                        this.current_staff_roles = response.data;
                        /* Add as clone so it doesn't update this.current_staff_roles.assigned by reference
                           when a user is is added/updated */
                        let update_roles = cloneDeep(response.data);
                        this.updated_staff_roles = update_roles.assigned;
                    } else {
                        console.log(response);
                    }
                }).catch((error) => {
                    let response_msg = `Unable load current staff roles for: ${this.title}`;
                    this.alertHandler.alertHandler('error', response_msg);
                    console.log(error);
                });
            },

            setSubmitting() {
                this.is_submitting = true;
            },

            setRoles() {
                this.updated_staff_roles = this.removeDeletedAssignedRoles();
                this.is_error_message = false;
                this.response_message = 'Saving permissions ..';

                axios({
                    method: 'put',
                    url: `/services/api/edit/acl/staff/${this.uuid}`,
                    data: JSON.stringify(this.updated_staff_roles),
                    headers: {'content-type': 'application/json; charset=utf-8'}
                }).then((response) => {
                    this.getRoles(); // Reset role list so user can close modal without a prompt.
                    let response_msg = `Staff roles successfully updated for: ${this.title}`;
                    this.alertHandler.alertHandler('success', response_msg);
                    this.unsaved_changes = false;
                    this.is_submitting = false;
                    this.deleted_users = [];
                    this.is_error_message = true; // Reset, as "save" is the only non-error status
                    this.response_message = '';
                }).catch((error) => {
                    let response_msg = `Unable to update staff roles for: ${this.title}`;
                    this.is_submitting = false;
                    this.alertHandler.alertHandler('error', response_msg);
                    console.log(error);
                });
            },

            userExists(user) {
                return this.updated_staff_roles.findIndex((u) => u.principal === user.principal);
            },

            /**
             * Remove users to be deleted from roles before submitting
             * @returns {[]|*[]}
             */
            removeDeletedAssignedRoles() {
                return this.updated_staff_roles.filter((user) => {
                    return this.findDeletedUserIndex(user) === -1;
                });
            },

            findDeletedUserIndex(user) {
                return this.deleted_users.findIndex((u) => u.principal === user.principal);
            },

            /**
             * Add user with already assigned permissions to list of user/permissions to delete
             * @param index
             */
            markUserForDeletion(index) {
                this.deleted_users.push(this.updated_staff_roles[index]);
            },

            /**
             * Fully purge a user without marking for deletion first
             * @param index
             */
            fullyRemoveUser(index) {
                this.updated_staff_roles.splice(index, 1);
            },

            /**
             * Check if user is in list of users to delete
             * @param user
             */
            checkUserRemoved(user) {
                return this.findDeletedUserIndex(user) !== -1;
            },

            /**
             * Remove user with already assigned permissions from list of users to delete upon form submission
             * @param user
             * @returns {undefined|*}
             */
            revertRemoveUser(user) {
                let user_index = this.findDeletedUserIndex(user) ;
                if (user_index !== -1) {
                    return this.deleted_users.splice(user_index, 1)[0];
                }

                return undefined;
            },

            /**
             * Add new user with new role
             * @param user
             */
            updateUserList(user) {
                if (this.userExists(user) === -1) {
                    this.updated_staff_roles.push(user);
                } else if (!this.is_submitting) {
                    this.response_message = `User: ${user.principal} already exists. User not added.`;

                    setTimeout(() => {
                        this.response_message = '';
                    }, 2000);
                }
            },

            /**
             * Reacts to event that username has been filled out in the form.
             * True on 'Add', false otherwise
             * @param is_filled
             */
            addUserFilledOut(is_filled) {
                this.unsaved_changes = is_filled;

                if (this.is_submitting && !this.unsaved_changes) {
                    this.setRoles();
                }
            },

            /**
             * Update a current user's role
             * @param user
             */
            updateUserRole(user) {
                let user_index = this.userExists(user);

                if (user_index !== -1) {
                    this.updated_staff_roles[user_index].role = user.role;
                }
            },

            updateErrorMsg(msg) {
                this.response_message = msg;
            },

            /**
             * Emit a close modal event
             * Checks if there are unsaved changes and asks user to confirm exit, if so.
             */
            showModal() {
                this.unsavedUpdates();

                if (!this.is_submitting && this.unsaved_changes) {
                    let message = 'There are unsaved permission updates. Are you sure you would like to exit?';
                    if (window.confirm(message)) {
                        this.$emit('show-modal', false);
                    }
                } else {
                    this.$emit('show-modal', false);
                }

                // Reset changes check in parent component
                this.$emit('reset-changes-check', false);
                this.is_closing_modal = false;
            },

            /**
             * Checks for unsaved role updates
             */
            unsavedUpdates() {
                this.is_closing_modal = true;

                let unsaved_staff_roles = this.updated_staff_roles.some((user) => {
                    let current_user = this.current_staff_roles.assigned.find((u) => user.principal === u.principal);
                    return (current_user === undefined || current_user.role !== user.role);
                });

                this.unsaved_changes = unsaved_staff_roles || this.deleted_users.length > 0;
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

        h1 {
            font-size: 18px;
            font-weight: normal;
        }

        h4 {
            font-size: 14px;
            font-weight: normal;
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
                margin-left: 0;

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

        .btn-disabled {
            opacity: .3;
            cursor: not-allowed;
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