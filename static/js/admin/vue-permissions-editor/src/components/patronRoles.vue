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
            <template v-if="hasParentRole" v-for="user in sortedRoles.inherited">
                <patron-display-row
                        :container-type="containerType"
                        :display-roles="display_roles"
                        :possible-roles="possibleRoles"
                        type="inherited"
                        :user="user"></patron-display-row>
            </template>
            <template v-if="hasObjectRole" v-for="user in sortedRoles.assigned">
                <patron-display-row
                        :container-type="containerType"
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
                <span @click="updateRoleList" id="patron">
                    <input type="radio" @click="updateRoleList" v-model="user_type" value="patron" :disabled="isDeleted"> Allow patron access
                </span>
                <ul class="patron">
                    <li class="public-role">
                        <p>Public users</p>
                        <div class="select-wrapper" :class="{'is-disabled': shouldDisable}">
                            <select id="public" @change="updateRole('everyone')" class="public-select" v-model="everyone_role" :disabled="shouldDisable">
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
            <li>
                <span @click="updateRoleList" id="staff">
                    <input type="radio" @click="updateRoleList" v-model="user_type" value="staff" :disabled="isDeleted"> Staff only access
                </span>
            </li>
        </ul>

        <embargo ref="embargoInfo"
                 :current-embargo="display_roles.assigned.embargo"
                 :is-deleted="isDeleted"
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
    import patronDisplayRow from './patronDisplayRow';
    import embargo from './embargo';
    import displayModal from '../mixins/displayModal';
    import patronHelpers from '../mixins/patronHelpers';
    import axios from 'axios';
    import cloneDeep from 'lodash.clonedeep';
    import isEmpty from 'lodash.isempty';

    const STAFF_ONLY_ROLE_TEXT = '\u2014';
    let staffOnlyRoles = () => {
        return [
            { principal: 'everyone', role: 'none', principal_display: 'staff' },
            { principal: 'authenticated', role: 'none', principal_display: 'staff' }
        ];
    };
    let initialRoles = () => cloneDeep({ roles: [], embargo: null, deleted: false });

    export default {
        name: 'patronRoles',

        components: {patronDisplayRow, embargo},

        mixins: [displayModal, patronHelpers],

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
                    inherited: initialRoles(),
                    assigned: initialRoles()
                },
                patron_roles: {
                    inherited: initialRoles(),
                    assigned: initialRoles()
                },
                submit_roles: initialRoles(),
                role_history: {},
                authenticated_role: 'none',
                everyone_role: 'none',
                history_set: false,
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

            sortedRoles() {
                let assigned = this.sortedUsers(this.display_roles.assigned.roles);
                let inherited = this.sortedUsers(this.display_roles.inherited.roles);

                if (assigned.length > 0  && (this.sameRolesAll || this.sameRolesNoInherited || [0].principal_display === 'staff')) {
                    assigned = [this.display_roles.assigned.roles[0]];
                }

                if (this.sameRolesAll) {
                    inherited =  [this.display_roles.inherited.roles[0]];
                }

                return { inherited: inherited, assigned: assigned };
            },

            sameRolesAll() {
              return this.hasSameAssignedRoles && this.hasSameInheritedRoles;
            },

            sameRolesNoInherited() {
                let inherited = this.display_roles.inherited.roles;
                return inherited.length === 0 && this.hasSameAssignedRoles;
            },

            compareRoles() {
                return {
                    inherited: this.patron_roles.inherited,
                    assigned: this.submit_roles
                };
            },

            /**
             * Returns the current state of non-staff users
             * @returns {*[]}
             */
            assignedPatronRoles() {
                return [
                    { principal: 'everyone', role: this.everyone_role, principal_display: 'everyone' },
                    { principal: 'authenticated', role: this.authenticated_role, principal_display: 'authenticated' }
                ];
            },

            shouldDisable() {
                return this.user_type === 'staff' || this.user_type === '';
            },

            embargoed() {
                return this.display_roles.assigned.embargo !== null;
            },

            isDeleted() {
                return this.display_roles.assigned.deleted;
            },

            isCollection() {
                return this.containerType.toLowerCase() === 'collection';
            },

            hasSameInheritedRoles() {
                let inherited = this.display_roles.inherited.roles;
                return inherited.length > 1 && inherited[0].role === inherited[1].role;
            },

            hasSameOrEmptyInheritedRoles() {
                let inherited = this.display_roles.inherited.roles;
                let roleSize = inherited.length;
                return roleSize === 0 || (roleSize > 1 && inherited[0].role === inherited[1].role);
            },

            hasSameAssignedRoles() {
                let assigned = this.display_roles.assigned.roles;
                return assigned.length > 1 && assigned[0].role === assigned[1].role;
            }
        },

        methods: {
            sortedUsers(users) {
                return users.sort((a, b) => b.principal.localeCompare(a.principal));
            },

            defaultRoles(perms, type) {
                if (perms.roles === null) {
                    perms.roles = [];
                } else if (perms.roles.length === 0 && type === 'assigned' && this.isCollection) {
                    this.authenticated_role = 'none';
                    this.everyone_role = 'none';
                    perms.roles = this.assignedPatronRoles;
                } else if (perms.roles.length === 0 && type === 'assigned') {
                    this.authenticated_role = 'canViewOriginals';
                    this.everyone_role = 'canViewOriginals';
                    perms.roles = this.assignedPatronRoles;
                }

                return perms;
            },

            defaultPublicDisplayRoles(perms) {
                if (perms.inherited.roles.length === 1 && perms.inherited.roles[0].principal === 'authenticated') {
                    perms.inherited.roles.push({ principal: 'everyone', role: 'none', principal_display: 'everyone' });
                }

                return perms;
            },

            getRoles() {
                axios.get(`/services/api/acl/patron/${this.uuid}`).then((response) => {
                    if ((response.data.inherited.roles === null || response.data.inherited.roles.length === 0) &&
                        response.data.assigned.roles.length === 0) {
                        let assigned_defaults;
                        response.data.inherited.roles = this._defaultInherited();

                        if (this.isCollection) {
                            assigned_defaults = staffOnlyRoles();
                        } else {
                            assigned_defaults = [
                                { principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' },
                                { principal: 'authenticated', role: 'canViewOriginals', principal_display: 'authenticated' }
                            ]
                        }

                        response.data.assigned.roles = assigned_defaults;

                        this.display_roles.inherited = response.data.inherited;
                        this.display_roles.assigned = response.data.assigned;
                        this.patron_roles.assigned = cloneDeep(response.data.assigned);
                        this.submit_roles = cloneDeep(response.data.assigned);
                    } else {
                        let default_perms = {
                            inherited: this.defaultRoles(response.data.inherited, 'inherited'),
                            assigned: this.defaultRoles(response.data.assigned, 'assigned')
                        };

                        this.patron_roles =  cloneDeep(default_perms);
                        this.submit_roles = cloneDeep(default_perms.assigned);

                        // Add in staff user for display if no users returned
                        if (default_perms.inherited.roles.length === 0) {
                            default_perms.inherited.roles = this._defaultInherited();
                        }

                        this.display_roles = cloneDeep(this.defaultPublicDisplayRoles(default_perms));
                    }

                    // Set values for forms from retrieved data
                    this.everyone_role = this.setCurrentObjectRole('everyone');
                    this.authenticated_role = this.setCurrentObjectRole('authenticated');

                    /* Format display values
                    * Set public display of user names
                    * Merge principals for display if role values are the same and update public user name
                    * Reset effective display roles if embargoes present
                    */
                    this.display_roles.inherited.roles = this.displayRolesMerge(this.display_roles.inherited.roles);

                    if (!this.isDeleted) {
                        this.display_roles.assigned.roles = this.embargoedRoles('loading');
                    } else {
                        this.display_roles.assigned.roles = [
                            { principal: 'everyone', role: 'none', principal_display: 'staff' },
                            { principal: 'authenticated', role: 'none', principal_display: 'staff' }
                        ];
                    }
                }).catch((error) => {
                    let response_msg = `Unable to load current patron roles for: ${this.title}`;
                    this.alertHandler.alertHandler('error', response_msg);
                    console.log(error);
                });
            },

            setRoles() {
                this.is_submitting = true;
                this.response_message = 'Saving permissions \u2026';
                // Remove display role before submitting
                this.submit_roles.roles.forEach((d) => {
                    delete d.principal_display;
                });

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
                    this.patron_roles.assigned = cloneDeep(this.submit_roles);
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
                if (!this.isCollection) {
                    default_inherited = staffOnlyRoles();
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
                let type;

                if (users.length === 0) {
                    return users;
                } else if (users[0].role === 'none' && users[1].role === 'none'
                    && (this.hasSameOrEmptyInheritedRoles)) {
                    type = 'staff';
                } else if (this.sameRolesAll || this.sameRolesNoInherited) {
                    type = 'patron';
                }

                users.forEach((u) => {
                    u.principal_display = type || u.principal;
                });

                return users;
            },

            /**
             * Set the form display value for the given user
             * @param principal
             * @returns {string}
             */
            setCurrentObjectRole(principal) {
                let user_index = this.userIndex(principal);
                let is_staff = this.userIndex('staff') !== -1;
                let role_type = 'none';

                if (this.isDeleted || is_staff || this._allRolesNone()) {
                    this.user_type = 'staff';
                } else if (user_index !== -1) {
                    this.user_type = 'patron';
                }

                if (user_index !== -1) {
                    role_type = this.display_roles.assigned.roles[user_index].role;
                } else if (this.isDeleted) {
                    role_type = 'canViewOriginals';
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

                // Use wrapper id if radio button clicked
                if (type === '') {
                    type = e.target.parentElement.id;
                }

                this.user_type = type;

                if (type === 'staff') {
                    this.setRoleHistory();
                    this.history_set = true;
                    this.everyone_role = 'none';
                    this.authenticated_role = 'none';
                } else if (type === 'patron') {
                    this.loadPreviousRole();
                    this.history_set = false;
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
                    this.display_roles.assigned.roles[user_index].role = this.embargoReduceRole(this[`${principal}_role`]);
                } else {
                    this.display_roles.assigned.roles.push({
                        principal: principal,
                        role: this.embargoReduceRole(this[`${principal}_role`]),
                        principal_display: principal
                    });
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
                    this.display_roles.assigned.roles = staffOnlyRoles();
                } else {
                    if (this.display_roles.assigned.embargo !== null) {
                        let everyone_display = 'everyone';
                        let authenticated_display = 'authenticated';

                        if (this.hasSameInheritedRoles) {
                            everyone_display = 'patron';
                            authenticated_display = 'patron';
                        }
                        this.display_roles.assigned.roles = [
                            { principal: 'everyone', role: 'canViewMetadata', principal_display: everyone_display },
                            { principal: 'authenticated', role: 'canViewMetadata', principal_display: authenticated_display }
                        ];
                    }
                }

                this.dedupeDisplayRoles();
            },

            /**
             * Resets display roles if an embargo is present
             * @param role_list
             * @returns {*}
             */
            embargoedRoles(role_list = 'updated') {
                let updated_display = (role_list === 'updated') ? this.assignedPatronRoles : this.display_roles.assigned.roles;
                let updated = updated_display.map((u) => {
                    let display_text = (u.principal_display === undefined) ? u.principal : u.principal_display;
                    return { principal: u.principal, role: this.embargoReduceRole(u.role), principal_display: display_text }
                });

                return this.displayRolesMerge(updated);
            },

            /**
             * Reduces effective role if an embargo is present and role is more permissive than canViewMetadata
             * @param role
             * @returns {string|*}
             */
            embargoReduceRole(role) {
                if (this.embargoed && this.possibleRoles.findIndex((r) => r.role === role) > 2) {
                    return 'canViewMetadata';
                }

                return role;
            },

            setRoleHistory() {
                if (!this.history_set) {
                    this.role_history = {
                        patron: this.everyone_role,
                        authenticated: this.authenticated_role
                    };
                }
            },

            loadPreviousRole() {
                if (!isEmpty(this.role_history)) {
                    this.everyone_role = this.role_history.patron;
                    this.authenticated_role = this.role_history.authenticated;
                }
            },

            /**
             * Merge assigned roles for display
             */
            dedupeDisplayRoles() {
                if (this.embargoed) {
                    this.display_roles.assigned.roles = this.displayRolesMerge(this.embargoedRoles());
                } else {
                    this.display_roles.assigned.roles = this.displayRolesMerge(this.assignedPatronRoles);
                }

                this.display_roles.inherited.roles = this.displayRolesMerge(this.display_roles.inherited.roles);
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

                if (initial_role === undefined) {
                    return true;
                }
                return initial_role.role !== current_role.role;
            },

            userIndex(principal, is_display = true) {
                let roles = is_display ? this.display_roles.assigned.roles : this.patron_roles.assigned.roles;
                return roles.findIndex((user) => {
                    return this.isPublicEveryone(principal, user.principal);
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
                this.display_roles.assigned.embargo = embargo_info;
                this.submit_roles.embargo = embargo_info;

                if (embargo_info !== null) {
                    let roles = this.display_roles.assigned.roles;
                    if (roles.length > 0 && roles[0].principal !== 'staff') {
                        this.display_roles.assigned.roles = this.embargoedRoles();
                    }
                } else {
                    this.loadPreviousRole();
                    this.dedupeDisplayRoles();
                }

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

        span:hover {
            cursor: default;
        }

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

        .btn-disabled {
            &:hover {
                cursor: not-allowed;
                opacity: .5
            }
        }

        p.error {
            color: red;
        }
    }
</style>