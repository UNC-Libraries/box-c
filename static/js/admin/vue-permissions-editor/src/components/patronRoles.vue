<template>
    <div id="patron-roles">
        <h1>Set Patron Access</h1>
        <h3 v-if="hasParentRole || hasObjectRole">Current Settings</h3>

        <table v-if="hasParentRole || hasObjectRole" class="border inherited-permissions">
            <thead>
                <tr>
                    <th></th>
                    <th class="access-display">Who can access</th>
                    <th>What can be viewed</th>
                </tr>
            </thead>
            <tbody>
            <template v-if="hasParentRole" v-for="user in display_roles.inherited.roles">
                <patron-display-row
                        :display-roles="display_roles"
                        :possible-roles="possibleRoles"
                        type="inherited"
                        :user="user"></patron-display-row>
            </template>
            <template v-if="hasObjectRole" v-for="user in display_roles.assigned.roles">
                <patron-display-row
                        :display-roles="display_roles"
                        :possible-roles="possibleRoles"
                        type="assigned"
                        :user="user"></patron-display-row>
            </template>
            </tbody>
        </table>
        <p v-else>There are no current permissions assigned</p>

        <ul class="set-patron-roles">
            <li>
                <input @click="updateRoleList" id="patron" type="radio" v-model="user_type" value="patron"> Allow patron access
                <ul class="patron">
                    <li class="public-role">
                        <p>Public users</p>
                        <div class="select-wrapper" :class="{'is-disabled': shouldDisable}">
                            <select id="public" @change="updateRole('patrons')" class="public-select" v-model="patrons_role" :disabled="shouldDisable">
                                <template v-for="(role, index) in possibleRoles">
                                    <option v-if="index > 0" :value="role.role">{{ role.text }}</option>
                                </template>
                            </select>
                        </div>
                    </li>
                    <li>
                        <p>Onyen users</p>
                        <div class="select-wrapper" :class="{'is-disabled': shouldDisable}">
                            <select id="onyen" @change="updateRole('onyen')" v-model="onyen_role" :disabled="shouldDisable">
                                <template v-for="(role, index) in possibleRoles">
                                    <option v-if="index > 0" :value="role.role">{{ role.text }}</option>
                                </template>
                            </select>
                        </div>
                    </li>
                </ul>
            </li>
            <li><input @click="updateRoleList" id="staff" type="radio" v-model="user_type" value="staff"> Staff only access</li>
        </ul>

        <embargo @embargo-info="setEmbargo"></embargo>
        <p class="message">{{ response_message }}</p>
        <ul>
            <li>
                <button id="is-submitting"
                        type="submit"
                        @click="setRoles"
                        :class="{'btn-disabled': !unsaved_changes}"
                        :disabled="!unsaved_changes">Save Changes</button>
            </li>
            <li><button @click="showModal" id="is-canceling" class="cancel" type="reset">Cancel</button></li>
        </ul>
    </div>
</template>

<script>
    import patronDisplayRow from "./patronDisplayRow";
    import embargo from "./embargo";
    import displayModal from "../mixins/displayModal";
    import axios from 'axios';
    import cloneDeep from 'lodash.clonedeep';
    import isEmpty from 'lodash.isempty';

    const STAFF_ONLY_ROLE_TEXT = '\u2014';

    export default {
        name: 'patronRoles',

        components: {patronDisplayRow, embargo},

        mixins: [displayModal],

        props: {
            alertHandler: Object,
            changesCheck: Boolean,
            containerType: String,
            uuid: String
        },

        data() {
            return {
                display_roles: {
                    inherited: { roles: [], embargoed: false, deleted: false },
                    assigned: { roles: [], embargoed: false, deleted: false }
                },
                patron_roles: {
                    inherited: { roles: [], embargoed: false, deleted: false },
                    assigned: { roles: [], embargoed: false, deleted: false }
                },
                submit_roles: [],
                role_history: {},
                object_embargo_info: {},
                parent_embargo_info: {},
                onyen_role: 'none',
                patrons_role: 'none',
                user_type: ''
            }
        },

        computed: {
            possibleRoles() {
                let container = this.containerType.toLowerCase();

                return [
                    { text: STAFF_ONLY_ROLE_TEXT , role: STAFF_ONLY_ROLE_TEXT }, // Only used by the display tables
                    { text: 'No Access', role: 'none' },
                    { text: 'Can Discover', role: 'canDiscover' },
                    { text: 'Metadata Only', role: 'canViewMetadata' },
                    { text: 'Access Copies', role: 'canViewAccessCopies' },
                    { text: 'Can View Originals', role: 'canViewOriginals' },
                    { text: `All of this ${container}`, role: 'canAccess' }
                ]
            },

            hasParentRole() {
                return this.display_roles.inherited.roles.length > 0;
            },

            hasObjectRole() {
                return this.display_roles.assigned.roles.length > 0;
            },

            assignedPatronRoles() {
                return [
                    { principal: 'Public', role: this.patrons_role },
                    { principal: 'Onyen', role: this.onyen_role }
                ];
            },

            shouldDisable() {
                return this.user_type === 'staff' || this.user_type === '' || !isEmpty(this.object_embargo_info);
            }
        },

        methods: {
            defaultPermission(perms) {
                const options = [
                    { field: 'roles', default: [] },
                    { field: 'embargoed', default: false },
                    { field: 'deleted', default: false }
                ];

                options.forEach((option) => {
                    if (perms[option.field] === undefined) {
                        perms[option.field] = option.default;
                    }
                });

                return perms;
            },

            getRoles() {
               axios.get(`/services/api/acl/patron/${this.uuid}`).then((response) => {
                   if (response.data.inherited.roles.length === 0 && response.data.assigned.roles.length === 0) {
                       let set_roles = [
                           { principal: 'Public', role: 'canAccess' },
                           { principal: 'Onyen', role: 'canAccess' }
                       ];
                       this.display_roles.inherited.roles = [{ principal: 'Staff', role: STAFF_ONLY_ROLE_TEXT }];
                       this.display_roles.assigned.roles = set_roles;
                       this.patron_roles.assigned.roles = set_roles;
                       this.submit_roles = set_roles;
                   } else {
                       let default_perms = {
                           inherited: this.defaultPermission(response.data.inherited),
                           assigned: this.defaultPermission(response.data.assigned)
                       };
                       this.patron_roles =  default_perms;
                       this.display_roles = cloneDeep(default_perms);
                       this.submit_roles = cloneDeep(default_perms).assigned.roles;
                   }

                   // Set values for forms from retrieved data
                   this.patrons_role = this.setCurrentObjectRole('Public');
                   this.onyen_role = this.setCurrentObjectRole('Onyen');

                   // Merge principals for display if role values are the same
                   this.display_roles.inherited.roles = this.displayRolesMerge(this.display_roles.inherited.roles);
                   this.display_roles.assigned.roles = this.displayRolesMerge(this.display_roles.assigned.roles);
               }).catch((error) => {
                    let response_msg = `Unable load current patron roles for: ${this.title}`;
                    this.alertHandler.alertHandler('error', response_msg);
                    console.log(error);
                });
            },

            setRoles() {
                this.is_submitting = true;
                this.response_message = 'Saving permissions \u2026';

                axios({
                    method: 'put',
                    url: `/services/api/edit/acl/patron/${this.uuid}`,
                    data: JSON.stringify( { roles: this.submit_roles } ),
                    headers: {'content-type': 'application/json; charset=utf-8'}
                }).then((response) => {
                    let response_msg = `Patron roles successfully updated for: ${this.title}`;
                    this.alertHandler.alertHandler('success', response_msg);
                    this.unsaved_changes = false;
                    this.is_submitting = false;
                    this.response_message = '';
                }).catch((error) => {
                    let response_msg = `Unable to update patron roles for: ${this.title}`;
                    this.is_submitting = false;
                    this.alertHandler.alertHandler('error', response_msg);
                    console.log(error);
                });
            },

            /**
             *
             */
            displayRolesMerge(users) {
                if (users.length === 2 && users[0].role === users[1].role) {
                    return [{ principal: 'Public', role: users[0].role }];
                }

                return users;
            },

            setCurrentObjectRole(principal) {
                let user_index = this.userIndex(principal);
                let role_type = 'none';

                if (this.userIndex('Staff') !== -1) {
                    this.user_type = 'staff';
                } else if (user_index !== -1) {
                    this.user_type = 'patron';
                    role_type = this.display_roles.assigned.roles[user_index].role;
                }

                return role_type;
            },

            updateRoleList(e) {
                this.unsaved_changes = true;
                let type = e.target.id;

                if (type === 'staff') {
                    this.role_history = Object.assign({}, { patron: this.patrons_role, onyen: this.onyen_role });
                    this.patrons_role = 'none';
                    this.onyen_role = 'none';
                } else if (type === 'patron') {
                    if (!isEmpty(this.role_history)) {
                        this.patrons_role = this.role_history.patron;
                        this.onyen_role = this.role_history.onyen;
                    }
                }

                this.updateDisplayRoles(type);
                this.updateSubmitRoles();
            },

            updateRole(principal) {
                let user_index = this.userIndex(principal);

                if (user_index !== -1) {
                    this.display_roles.assigned.roles[user_index].role = this[`${principal}_role`];
                } else {
                    this.display_roles.assigned.roles.push({principal: principal, role: this[`${principal}_role`]})
                }

                this.unsaved_changes = true;
                this.dedupeDisplayRoles();
                this.updateSubmitRoles();
            },

            updateDisplayRoles(type) {
                if (type === 'staff') {
                    this.display_roles.assigned.roles = [{ principal: 'Staff', role: STAFF_ONLY_ROLE_TEXT }]
                } else {
                    this.dedupeDisplayRoles();
                }
            },

            dedupeDisplayRoles() {
                this.display_roles.assigned.roles = this.displayRolesMerge(this.assignedPatronRoles);
            },

            updateSubmitRoles() {
                this.submit_roles = this.assignedPatronRoles;
            },

            userIndex(principal) {
                return this.display_roles.assigned.roles.findIndex((user) => {
                  return user.principal.toLowerCase() === principal.toLowerCase();
                });
            },

            setEmbargo(embargo_info) {
                this.object_embargo_info = embargo_info;
                this.display_roles.assigned.embargoed = !this.display_roles.assigned.embargoed;

                if (!isEmpty(embargo_info)) {
                    this.unsaved_changes = true;
                    this.display_roles.assigned.roles = [{principal: 'Public', role: 'canViewMetadata'}];
                    this.patrons_role = 'canViewMetadata';
                    this.onyen_role = 'canViewMetadata';
                } else {
                    this.dedupeDisplayRoles();
                }

                this.submit_roles = this.assignedPatronRoles();
            },

            /**
             * Wrap so it can share a watcher with patronRoles.vue
             * See mixins/displayModal.js
             */
            showModal() {
                this.displayModal();
            }
        },

        mounted() {
            this.getRoles();
        }
    }
</script>

<style scoped lang="scss">
    #patron-roles {
        text-align: left;
        margin: 5px 25px;

        select {
            margin-left: 5px;
            padding: 5px;
            width: 225px;
        }

        .public-select {
            margin-left: 10px;
        }

        .inherited-permissions, p {
            margin-bottom: 25px;
        }

        .set-patron-roles {
            border-top: 1px dashed gray;
            list-style-type: none;
            padding-top: 25px;
            text-align: left;

            li {
                ul {
                    border-top: none;
                    margin: 10px 15px;
                    text-align: left;

                    li {
                        margin-left: 15px;
                        display: inline-flex;

                        p {
                            margin: auto 20px auto auto;
                        }
                    }
                }

                .public-role {
                    margin-left: 30px;
                }
            }
        }

        .patron {
            margin-left: 35px;
            margin-top: 15px;
            padding: 0;
        }

        .select-wrapper::after {
            right: 5px;
            top: 8px;
        }

        .is-disabled {
            cursor: not-allowed;
            opacity: .5;
        }
    }
</style>