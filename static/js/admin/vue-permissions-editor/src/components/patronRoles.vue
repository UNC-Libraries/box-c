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
                            <select id="public" @change="updateRole('patrons')" class="public-select" v-model="everyone_role" :disabled="shouldDisable">
                                <template v-for="(role, index) in possibleRoles">
                                    <option v-if="index > 0" :value="role.role">{{ role.text }}</option>
                                </template>
                            </select>
                        </div>
                    </li>
                    <li>
                        <p>Authenticated users</p>
                        <div class="select-wrapper" :class="{'is-disabled': shouldDisable}">
                            <select id="authenticated" @change="updateRole('authenticated')" v-model="authenticated_role" :disabled="shouldDisable">
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

        <embargo ref="embargoInfo"
                 :current-embargo="timestampEmbargo"
                 @embargo-info="setEmbargo"
                 @error-msg="embargoError">
        </embargo>
        <p class="message" :class="{error: !/Saving/.test(response_message)}">{{ response_message }}</p>
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
    let initial_roles = () => cloneDeep({ roles: [], embargo: null, deleted: false });

    export default {
        name: 'patronRoles',

        components: {patronDisplayRow, embargo},

        mixins: [displayModal],

        props: {
            alertHandler: Object,
            changesCheck: Boolean,
            containerType: String,
            title: String,
            uuid: String
        },

        data() {
            return {
                display_roles: {
                    inherited: initial_roles(),
                    assigned: initial_roles()
                },
                patron_roles: {
                    inherited: initial_roles(),
                    assigned: initial_roles()
                },
                submit_roles: initial_roles(),
                role_history: {},
                authenticated_role: 'none',
                everyone_role: 'none',
                response_message: '',
                unsaved_changes: false,
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
                    { text: `All of this ${container}`, role: 'canViewOriginals' }
                ]
            },

            hasParentRole() {
                return this.display_roles.inherited.roles.length > 0;
            },

            hasObjectRole() {
                return this.display_roles.assigned.roles.length > 0;
            },

            /**
             * Returns the current state non-staff users
             * @returns {*[]}
             */
            assignedPatronRoles() {
                return [
                    { principal: 'everyone', role: this.everyone_role },
                    { principal: 'authenticated', role: this.authenticated_role }
                ];
            },

            shouldDisable() {
                return this.user_type === 'staff' || this.user_type === '';
            },

            /**
             * Pass current embargo as a timestamp
             * @return {null | number}
             */
            timestampEmbargo() {
                if (this.patron_roles.assigned.embargo === null) {
                    return 0;
                } else {
                    return this.patron_roles.assigned.embargo;
                }
            }
        },

        methods: {
            defaultRoles(perms) {
                if (perms.roles === null) {
                    perms.roles = [];
                }

                return perms;
            },

            getRoles() {
                axios.get(`/services/api/acl/patron/${this.uuid}`).then((response) => {
                    if ((response.data.inherited.roles === null || response.data.inherited.roles.length === 0) &&
                        response.data.assigned.roles.length === 0) {
                        let set_roles = [
                            { principal: 'everyone', role: 'canViewOriginals' },
                            { principal: 'authenticated', role: 'canViewOriginals' }
                        ];

                        this.display_roles.inherited.roles = this._defaultInherited();
                        this.display_roles.assigned.roles = set_roles;
                        this.patron_roles.assigned.roles = set_roles;
                        this.submit_roles.roles = set_roles;
                    } else {
                        let default_perms = {
                            inherited: this.defaultRoles(response.data.inherited),
                            assigned: this.defaultRoles(response.data.assigned)
                        };
                        this.patron_roles =  default_perms;
                        this.display_roles = cloneDeep(default_perms);
                        this.submit_roles = cloneDeep(default_perms.assigned);
                    }

                    // Set values for forms from retrieved data
                    this.everyone_role = this.setCurrentObjectRole('everyone');
                    this.authenticated_role = this.setCurrentObjectRole('authenticated');

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
                    data: JSON.stringify(this.submit_roles),
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
             * Show inherited roles for object other than collections
             * @returns {Array}
             * @private
             */
            _defaultInherited() {
                let default_inherited;
                if (this.containerType.toLowerCase() !== 'collection') {
                    default_inherited = [{ principal: 'Staff', role: STAFF_ONLY_ROLE_TEXT }]
                } else {
                    default_inherited = [];
                }
                return default_inherited;
            },

            /**
             * Merge display if everyone and authenticated roles are the same
             * @param users
             * @return {{principal: string, role: *}[] | *}
             */
            displayRolesMerge(users) {
                if (users.length === 2 && users[0].role === users[1].role) {
                    if (users[0].role === 'none') {
                        return [{ principal: 'Staff', role: STAFF_ONLY_ROLE_TEXT }];
                    } else {
                        return [{ principal: 'patron', role: users[0].role }];
                    }
                }

                return users;
            },

            /**
             * Set the form display value for the given user
             * @param principal
             * @returns {string}
             */
            setCurrentObjectRole(principal) {
                let user_index = this.userIndex(principal);
                let role_type = 'none';

                if (this.userIndex('Staff') !== -1 || this._allRolesNone()) {
                    this.user_type = 'staff';
                } else if (user_index !== -1) {
                    this.user_type = 'patron';
                    role_type = this.display_roles.assigned.roles[user_index].role;
                }

                return role_type;
            },

            /**
             * Determines if all assigned roles are set to 'none'
             * If so staff permissions radio button should be checked
             * @returns {boolean}
             */
            _allRolesNone() {
                let roles = this.display_roles.assigned.roles.map((r) => r.role);
                let dedupe = [...new Set(roles)];

                return dedupe.length === 1 && dedupe[0] === 'none';
            },

            /**
             * Update the role type and current roles
             * Pull from role history if any, for non-staff roles
             * @param e
             */
            updateRoleList(e) {
                let type = e.target.id;

                if (type === 'staff') {
                    this.setRoleHistory();
                    this.everyone_role = 'none';
                    this.authenticated_role = 'none';
                } else if (type === 'patron') {
                    this.loadPreviousRole();
                }

                this.updateDisplayRoles(type);
                this.updateSubmitRoles();
                this.setUnsavedChanges();
            },

            /**
             * Update a users role or add the user and role if they don't exist
             * @param principal
             */
            updateRole(principal) {
                let user_index = this.userIndex(principal);

                if (user_index !== -1) {
                    this.display_roles.assigned.roles[user_index].role = this[`${principal}_role`];
                } else {
                    this.display_roles.assigned.roles.push({principal: principal, role: this[`${principal}_role`]})
                }

                this.dedupeDisplayRoles();
                this.updateSubmitRoles();
                this.setUnsavedChanges();
            },

            /**
             * Set roles based on user type
             * @param type
             */
            updateDisplayRoles(type) {
                if (type === 'staff') {
                    this.display_roles.assigned.roles = [{ principal: 'Staff', role: STAFF_ONLY_ROLE_TEXT }]
                } else {
                    this.dedupeDisplayRoles();
                }
            },

            setRoleHistory() {
                this.role_history = Object.assign({}, {
                    patron: this.everyone_role,
                    authenticated: this.authenticated_role
                });
            },

            loadPreviousRole() {
                if (!isEmpty(this.role_history)) {
                    this.everyone_role = this.role_history.patron;
                    this.authenticated_role = this.role_history.authenticated;
                }
            },

            /**
             * Merge assigned roles and set the result to current roles list
             */
            dedupeDisplayRoles() {
                this.display_roles.assigned.roles = this.displayRolesMerge(this.assignedPatronRoles);
            },

            updateSubmitRoles() {
                this.submit_roles.roles = this.assignedPatronRoles;
            },

            /**
             * Determine if there are unsaved changes
             */
            setUnsavedChanges() {
                let loaded_roles = this.patron_roles.assigned;
                this.unsaved_changes = this._hasRoleChange('everyone') || this._hasRoleChange('authenticated') ||
                    loaded_roles.embargo !== this.submit_roles.embargo || loaded_roles.deleted !== this.submit_roles.deleted;
            },

            /**
             * Determine if a user's role has changed
             * @param type
             * @returns {boolean}
             * @private
             */
            _hasRoleChange(type) {
                let initial_role = this.patron_roles.assigned.roles.find(user => user.principal === type);
                let current_role = this.submit_roles.roles.find(user => user.principal === type);

                if (initial_role === undefined || current_role === undefined) {
                    return false;
                } else {
                    return initial_role.role !== current_role.role;
                }
            },

            userIndex(principal) {
                return this.display_roles.assigned.roles.findIndex((user) => {
                    return user.principal.toLowerCase() === principal.toLowerCase();
                });
            },

            /**
             * Updates display message based on emitted event from embargo component
             * @param error_msg
             */
            embargoError(error_msg) {
                this.response_message = error_msg;
            },

            /**
             * Updates embargo for display and submit roles based on emitted event from embargo component
             * @param embargo_info
             */
            setEmbargo(embargo_info) {
                if (embargo_info !== null) {
                    this.setRoleHistory();
                    this.display_roles.assigned.roles = [{principal: 'patron', role: 'canViewMetadata'}];
                } else {
                    this.loadPreviousRole();
                    this.dedupeDisplayRoles();
                }

                this.display_roles.assigned.embargo = embargo_info;
                this.submit_roles.embargo = embargo_info;
                this.updateSubmitRoles();
                this.setUnsavedChanges();
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
                    margin: 10px 27px;
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

        p.error {
            color: red;
        }
    }
</style>