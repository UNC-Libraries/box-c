<template>
    <div id="staff-roles">
        <h1 v-if="canSetPermissions">Set Staff Permissions</h1>
        <h1 v-else>Inherited Staff Permissions</h1>

        <table class="border inherited-permissions" v-if="current_staff_roles.inherited !== undefined && current_staff_roles.inherited.roles.length > 0">
            <thead>
            <tr>
                <th>Staff</th>
                <th>Permission</th>
                <th>Inherited from</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="(inherited_staff_permission, index) in current_staff_roles.inherited.roles">
                <td>
                  <div class="text-only">
                    <span @mouseover="hover_row_inherited = index" @mouseleave="hover_row_inherited = ''">
                          {{ truncatePermissionText(inherited_staff_permission.principal) }}</span>
                    <span @click="copyPermission(inherited_staff_permission.principal)">
                          <i class="fas fa-copy" title="Copy full permission to clipboard"></i></span>
                  </div>
                  <div class="tooltip" v-if="hover_row_inherited === index">
                    {{ inherited_staff_permission.principal }}
                  </div>
                </td>
                <td>{{ inherited_staff_permission.role }}</td>
                <td>{{ assignedToName(inherited_staff_permission) }}</td>
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
                    <td class="border" :class="{'marked-for-deletion': checkUserRemoved(updated_staff_role)}">
                      <div class="text-only">
                        <span @mouseover="hover_row = index" @mouseleave="hover_row = ''">
                          {{ truncatePermissionText(updated_staff_role.principal) }}</span>
                        <span @click="copyPermission(updated_staff_role.principal)">
                          <i class="fas fa-copy" title="Copy full permission to clipboard"></i></span>
                      </div>
                      <div class="tooltip" v-if="hover_row === index">
                        {{ updated_staff_role.principal }}
                      </div>
                    </td>
                    <td class="border select-box size" :class="{'marked-for-deletion': checkUserRemoved(updated_staff_role)}">
                        <staff-roles-select
                                :container-type="containerType"
                                :are-deleted="deleted_users"
                                :user="updated_staff_role">
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
                <tr>
                    <td class="border size">
                        <input @focus="updateErrorMsg('')"
                               type="text"
                               placeholder="ONYEN/Group"
                               v-model.trim="user_name">
                    </td>
                    <td class="border select-box">
                        <div class="select-wrapper">
                            <select v-model="selected_role" @focus="updateErrorMsg('')">
                                <option v-for="role in containerRoles(containerType)" :value="role.value">{{ role.text }}</option>
                            </select>
                        </div>
                    </td>
                    <td class="btn">
                        <button class="btn-add" @click.prevent="updateUserList">Add</button>
                    </td>
                </tr>
            </table>
            <p class="message" :class="{error: is_error_message}">{{ response_message }}</p>
        </div>
        <p class="no-updates-allowed" v-else>Go to previous level(s) to modify the staff permission settings.</p>

        <ul>
            <li v-if="canSetPermissions">
                <button id="is-submitting"
                        type="submit"
                        @click="setSubmitting"
                        :class="{'btn-disabled': enableSubmit}"
                        :disabled="enableSubmit">Save Changes</button>
            </li>
            <li><button @click="showModal" id="is-canceling" class="cancel" type="reset">Cancel</button></li>
        </ul>
    </div>
</template>

<script>
    import staffRolesSelect from "@/components/staffRolesSelect.vue";
    import staffRoleList from "../mixins/staffRoleList";
    import displayModal from "../mixins/displayModal";
    import axios from 'axios';
    import cloneDeep from 'lodash.clonedeep';
    import isEmpty from 'lodash.isempty';
    import { mapState, mapStores } from 'pinia';
    import {usePermissionsStore} from "../stores/permissions";

    export default {
        name: 'staffRoles',

        components: {
            staffRolesSelect
        },

        mixins: [staffRoleList, displayModal],

        data() {
            return {
                current_staff_roles: { inherited: { roles: [] }, assigned: { roles: [] } },
                deleted_users: [],
                hover_row: '',
                hover_row_inherited: '',
                is_closing_modal: false,
                is_error_message: true,
                selected_role: 'canAccess',
                updated_staff_roles: [],
                user_name: ''
            }
        },

        computed: {
            // Get needed state from Pinia
            ...mapState(usePermissionsStore, {
                alertHandler: store => store.alertHandler,
                changesCheck: store => store.checkForUnsavedChanges,
                objectPath: store => store.metadata.objectPath,
                containerType: store => store.metadata.type,
                uuid: store => store.metadata.id,
                title: store => store.metadata.title
            }),

            ...mapStores(usePermissionsStore),

            canSetPermissions() {
                return ['AdminUnit', 'Collection'].includes(this.containerType);
            },

            enableSubmit() {
                return this.user_name === '' && !this.unsaved_changes;
            }
        },

        methods: {
            getRoles() {
                axios.get(`/services/api/acl/staff/${this.uuid}`).then((response) => {
                    if (!isEmpty(response.data)) {
                        this.current_staff_roles = response.data;
                        /* Add as clone so it doesn't update this.current_staff_roles.assigned by reference
                           when a user is added/updated */
                        let update_roles = cloneDeep(response.data);
                        this.updated_staff_roles = update_roles.assigned.roles;
                    }
                }).catch((error) => {
                    let response_msg = `Unable load current staff roles for: ${this.title}`;
                    this.alertHandler.alertHandler('error', response_msg);
                    console.log(error);
                });
            },

            setSubmitting() {
                this.is_submitting = true;
                this.updateUserList();
                this.setRoles();
            },

            setRoles() {
                this.updated_staff_roles = this.removeDeletedAssignedRoles();
                this.is_error_message = false;
                this.response_message = 'Saving permissions \u2026';

                axios({
                    method: 'put',
                    url: `/services/api/edit/acl/staff/${this.uuid}`,
                    data: JSON.stringify( { roles: this.updated_staff_roles } ),
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

            getUserIndex(user, use_update_list = true) {
                let user_list = (use_update_list) ? this.updated_staff_roles : this.deleted_users;
                return user_list.findIndex((u) => u.principal === user.principal);
            },

            /**
             * Remove users to be deleted from roles before submitting
             * @returns {[]|*[]}
             */
            removeDeletedAssignedRoles() {
                return this.updated_staff_roles.filter((user) => {
                    return this.getUserIndex(user, false) === -1;
                });
            },

            /**
             * Add user with already assigned permissions to list of user/permissions to delete
             * @param index
             */
            markUserForDeletion(index) {
                this.deleted_users.push(this.updated_staff_roles[index]);
                this.unsaved_changes = true;
            },

            /**
             * Fully purge a user without marking for deletion first
             * @param index
             */
            fullyRemoveUser(index) {
                this.updated_staff_roles.splice(index, 1);
                this.unsavedUpdates();
            },

            /**
             * Check if user is in list of users to delete
             * @param user
             */
            checkUserRemoved(user) {
                return this.getUserIndex(user, false) !== -1;
            },

            /**
             * Remove user with already assigned permissions from list of users to delete
             * @param user
             * @returns {undefined|*}
             */
            revertRemoveUser(user) {
                let user_index = this.getUserIndex(user, false) ;
                if (user_index !== -1) {
                    let reverted_user = this.deleted_users.splice(user_index, 1)[0];
                    this.unsavedUpdates();
                    return reverted_user;
                }

                return undefined;
            },

            /**
             * Add new user with new role
             */
            updateUserList() {
                let user = { principal: this.user_name, role: this.selected_role, type: 'new' };

                if (this.user_name === '') {
                    this.response_message = 'Please add a username before adding';
                } else if (this.getUserIndex(user) === -1) {
                    this.updated_staff_roles.push(user);
                    this.unsaved_changes = true;
                    this.user_name = '';
                    this.selected_role = 'canAccess';
                } else if (!this.is_submitting) {
                    this.response_message = `User: ${user.principal} already exists. User not added.`;

                    setTimeout(() => {
                        this.response_message = '';
                    }, 2000);
                }
            },

            truncatePermissionText(text) {
              if (text.length > 25) {
                let permissions = text.split(':');
                return ` \u2026 ${permissions[permissions.length - 1]}`;
              }

              return text;
            },

            async copyPermission(text) {
              try {
                await navigator.clipboard.writeText(text);
                this.alertHandler.alertHandler('success', `Permission copied to clipboard: ${text}`);
              } catch(err) {
                this.alertHandler.alertHandler('error', 'Unable to copy permission to clipboard');
              }
            },

            updateErrorMsg(msg) {
                this.response_message = msg;
            },

            /**
             * Close the modal window
             * Checks if there are unsaved changes and asks user to confirm exit, if so.
             */
            showModal() {
                this.is_closing_modal = true;
                this.unsavedUpdates();
                this.displayModal();
                this.is_closing_modal = false;
            },

            /**
             * Checks for unsaved role updates
             */
            unsavedUpdates() {
                let unsaved_staff_roles = this.updated_staff_roles.some((user) => {
                    let current_user = this.current_staff_roles.assigned.roles.find((u) => user.principal === u.principal);
                    return (current_user === undefined || current_user.role !== user.role);
                });

                this.unsaved_changes = unsaved_staff_roles || this.deleted_users.length > 0 || this.user_name !== '';
            },

            assignedToName(role_assignment) {
                if (this.objectPath === undefined || this.objectPath === null) {
                    return "--";
                }
                let assignedTo = role_assignment.assignedTo;
                for (const pathObj of this.objectPath) {
                    if (pathObj.pid === assignedTo) {
                        return pathObj.name;
                    }
                }
                return "--";
            }
        },

        mounted() {
            /* Subscribe to changes in the store. It's nearly impossible to "watch" for
            *  changes in a pinia store without switching everything to Vue's newer composition API.
            *  See this discussion https://github.com/vuejs/pinia/discussions/794 and
            *  https://vuejs.org/guide/extras/composition-api-faq.html */
            this.permissionsStore.$subscribe((mutation, state) => {
                let user = state.staffRole
                let user_index = this.getUserIndex(user);

                if (user_index !== -1) {
                    this.updated_staff_roles[user_index].role = user.role;
                    this.unsavedUpdates();
                }
            });
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

        th {
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

        span + span {
          float: right;
          padding-right: 5px;

          .fa-copy {
            color: #4B9CD3;

            &:hover {
              cursor: pointer;
            }
          }
        }

        .btn-remove {
            background-color: red;
        }

        .btn-revert {
            background-color: gray;
        }

        .error {
            color: red;
        }

        .tooltip {
            position: absolute;
            text-align: left;
            width: auto;
            height: auto;
            padding: 5px;
            color: white;
            background-color: #3D4D5B;
            border: 1px solid black;
            border-radius: 8px;
            pointer-events: none;
            max-width: 500px;
            word-break: break-word;
            z-index: 100;
        }
    }
</style>