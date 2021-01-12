<template>
    <div id="patron-roles">
        <h3>Effective Patron Access</h3>

        <table v-if="hasParentRole || hasObjectRole" class="border inherited-permissions">
            <thead>
            <tr>
                <th class="access-display">Who can access</th>
                <th>What can be viewed</th>
            </tr>
            </thead>
            <tbody>
            <template v-for="user in dedupedRoles">
                <patron-display-row :user="user" :user-type="user_type" :container-type="containerType"></patron-display-row>
            </template>
            </tbody>
        </table>
        <p v-else>There are no current permissions assigned</p>

        <h3 class="update-roles">Set Patron Access</h3>
        <ul class="set-patron-roles">
            <li v-if="!isCollection">
                <span @click="updateRoleList" id="parent">
                    <input type="radio" @click="updateRoleList" v-model="user_type" value="parent" :disabled="isDeleted"> Inherit from parent
                </span>
            </li>
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
                        @click="saveRoles"
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

    const METADATA_ONLY_ROLES = [
        { principal: 'everyone', role: 'canViewMetadata' },
        { principal: 'authenticated', role: 'canViewMetadata' }
    ];
    const STAFF_ONLY_ROLES = [
        { principal: 'everyone', role: 'none' },
        { principal: 'authenticated', role: 'none' }
    ];
    const VIEW_ORIGINAL_ROLES = [
        { principal: 'everyone', role: 'canViewOriginals' },
        { principal: 'authenticated', role: 'canViewOriginals' }
    ];
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
                last_clicked_access: '',
                response_message: '',
                unsaved_changes: false,
                user_type: ''
            }
        },

        computed: {
            possibleRoles() {
                return this.possibleRoleList(this.containerType);
            },

            hasParentRole() {
                return this.display_roles.inherited.roles.length > 0;
            },

            hasObjectRole() {
                return this.display_roles.assigned.roles.length > 0;
            },

            dedupedRoles() {
                let assigned = cloneDeep(this.display_roles.assigned.roles);
                let inherited = cloneDeep(this.display_roles.inherited.roles);
                this.setRoleType(assigned, 'assigned');
                this.setRoleType(inherited, 'inherited');

                return this.winningRoleList(inherited.concat(assigned));
            },

            /**
             * Returns the current state of users
             * @returns {*[]}
             */
            assignedPatronRoles() {
                return [
                    { principal: 'everyone', role: this.everyone_role },
                    { principal: 'authenticated', role: this.authenticated_role }
                ];
            },

            shouldDisable() {
                return this.user_type === 'staff' || this.user_type === 'parent';
            },

            isEmbargoed() {
                return this.display_roles.assigned.embargo !== null;
            },

            isEmbargoedParent() {
                return this.patron_roles.inherited.embargo !== null;
            },

            isDeleted() {
                return this.display_roles.assigned.deleted;
            },

            isDeletedParent() {
                return this.patron_roles.inherited.deleted;
            },

            isCollection() {
                return this.containerType.toLowerCase() === 'collection';
            }
        },

        methods: {
            setRoleType(roles, type) {
                if (roles.length > 0) {
                    roles.forEach((d) => {
                        d.type = type;
                    });
                }
            },

            getRoles() {
                axios.get(`/services/api/acl/patron/${this.uuid}`).then((response) => {
                    // Set display roles
                    this.display_roles.inherited = this._setInitialInherited(response.data.inherited);
                    this.display_roles.assigned = this._setInitialAssigned(response.data.assigned);
                    // Set roles from server
                    this.patron_roles = {
                        inherited: cloneDeep(response.data.inherited), // Pick up default roles for comparing roles
                        assigned: cloneDeep(response.data.assigned)
                    };
                    // Set submit roles
                    this.submit_roles = cloneDeep(response.data.assigned);
                    // Set form variables
                    this._setInitialForm(response.data.assigned.roles.length);
                    // Set form history
                    this.setRoleHistory();
                }).catch((error) => {
                    let response_msg = `Unable to load current patron roles for: ${this.title}`;
                    this.alertHandler.alertHandler('error', response_msg);
                    console.log(error);
                });
            },

            _setInitialInherited(responseInherited) {
                let inherited = cloneDeep(responseInherited);

                if (inherited.roles === null || inherited.roles.length === 0) {
                    inherited.roles = (!this.isCollection) ? STAFF_ONLY_ROLES : [];
                } else if (inherited.roles.length === 1) {
                    const hasEveryone = inherited.roles.find((d) => d.principal === 'everyone') !== undefined;
                    let princText = hasEveryone ? 'authenticated' : 'everyone';
                    inherited.roles.push({ principal: princText, role: 'none' });
                }

                return inherited;
            },

            _setInitialAssigned(responseAssigned) {
                let assigned = cloneDeep(responseAssigned);

                if (assigned.roles.length === 0)  {
                    assigned.roles = this.isCollection ? STAFF_ONLY_ROLES : VIEW_ORIGINAL_ROLES;
                }

                return assigned;
            },

            // Note: Collections always have roles
            _setInitialForm(noAssigned) {
                if (!this.isCollection && noAssigned === 0) {
                    this.authenticated_role = 'canViewOriginals';
                    this.everyone_role = 'canViewOriginals';
                    this.user_type = 'parent';
                } else {
                    this.authenticated_role = this.display_roles.assigned.roles[this.userIndex('authenticated')].role;
                    this.everyone_role = this.display_roles.assigned.roles[this.userIndex('everyone')].role;
                    let isStaffRole = this.authenticated_role === 'none' && this.everyone_role === 'none';
                    this.user_type = (isStaffRole) ? 'staff' : 'patron';
                }
            },

            saveRoles() {
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
                    this.patron_roles.assigned = cloneDeep(this.submit_roles);
                }).catch((error) => {
                    let response_msg = `Unable to update patron roles for: ${this.title}`;
                    this.is_submitting = false;
                    this.alertHandler.alertHandler('error', response_msg);
                    console.log(error);
                });
            },

            setParentRole() {
                this.everyone_role = 'canViewOriginals';
                this.authenticated_role = 'canViewOriginals';
                this.submit_roles.roles = [];
                this.display_roles.assigned.roles = [];
            },

            setPatronRole() {
                this.display_roles.assigned.roles = this.assignedPatronRoles;
                this.submit_roles.roles = [
                    { principal: 'everyone', role: this.everyone_role, assignedTo: this.uuid },
                    { principal: 'authenticated', role: this.authenticated_role, assignedTo: this.uuid }
                ]
            },

            setStaffRole() {
                this.everyone_role = 'none';
                this.authenticated_role = 'none';
                this.display_roles.assigned.roles = STAFF_ONLY_ROLES;
                this.submit_roles.roles = [
                    { principal: 'everyone', role: 'none', assignedTo: this.uuid },
                    { principal: 'authenticated', role: 'none', assignedTo: this.uuid }
                ];
            },

            selectedRole(type) {
                this.user_type = type;
                if (type === 'parent') {
                    this.setParentRole();
                } else if (type === 'patron') {
                    this.setPatronRole();
                } else {
                    this.setStaffRole();
                }
            },

            /**
             * Update the role type and current roles when switching between parent/patron/staff options
             * Pull from role history if any, for patron roles
             * @param e
             */
            updateRoleList(e) {
                if (!this.isDeleted) {
                    let type = e.target.id;

                    // Use wrapper id if radio button clicked
                    if (type === '') {
                        type = e.target.parentElement.id;
                    }

                    if ((type === 'staff' || type === 'parent') && this.last_clicked_access === 'patron') {
                        this.setRoleHistory();
                    }

                    if (type === 'patron') {
                        this.loadPreviousRole(type);
                    }

                    this.last_clicked_access = type;
                    this.selectedRole(type);
                    this.setUnsavedChanges();
                }
            },

            /**
             * Update roles if one of the patron select boxes is updated
             * @param principal
             */
            updateRole(principal) {
                this.setPatronRole();
                this.setUnsavedChanges();
            },

            setRoleHistory() {
                if (!this.history_set) {
                    this.role_history = {
                        patron: this.everyone_role,
                        authenticated: this.authenticated_role
                    };
                    this.history_set = true;
                }
            },

            /**
             * Load previously set patron role if patron option is checked
             * @param type
             */
            loadPreviousRole(type) {
                if (!isEmpty(this.role_history)) {
                    this.everyone_role = this.role_history.patron;
                    this.authenticated_role = this.role_history.authenticated;

                    if (type === 'patron') {
                        this.history_set = false;
                    }
                }
            },

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
                let initial_role_exists = initial_role !== undefined;
                let current_role_exists = current_role !== undefined;

                if (!initial_role_exists && !current_role_exists) {
                    return false;
                } else if ((!initial_role_exists && current_role_exists) ||
                    (initial_role_exists && !current_role_exists)) {
                    return true;
                } else {
                    return initial_role.role !== current_role.role;
                }
            },

            userIndex(principal, is_display = true) {
                let roles = is_display ? this.display_roles.assigned.roles : this.patron_roles.assigned.roles;
                return roles.findIndex((user) => principal === user.principal);
            },

            /**
             * Return array of winning roles
             * @param allRoles
             * @returns {array}
             */
            winningRoleList(allRoles) {
                let everyone = allRoles.filter((d) => d.principal === 'everyone');
                let authenticated = allRoles.filter((d) => d.principal === 'authenticated');

                let everyoneRole = this.winningRole(everyone);
                let authenticatedRole = this.winningRole(authenticated);

                // Compact roles if they have the same role and are of the same type
                if (everyoneRole.role === authenticatedRole.role) {
                    if ((everyoneRole.type === 'inherited' && authenticatedRole.type === 'inherited') ||
                        (everyoneRole.type === 'assigned' && authenticatedRole.type === 'assigned')) {
                        everyoneRole.principal = everyoneRole.role  === 'none' ? 'staff' : 'patron';
                        return [everyoneRole];
                    }
                }

                return [everyoneRole, authenticatedRole];
            },

            /**
             * Return winning role for a type of role, e.g. everyone or authenticated
             * @param roles
             * @returns {object}
             */
            winningRole(roles) {
                let assignedRole = roles.find((d) => d.type === 'assigned');
                if (assignedRole !== undefined) {
                    assignedRole = this.addRoleInfo(assignedRole, 'assigned');
                }
                let inheritedRole = roles.find((d) => d.type === 'inherited');
                if (inheritedRole !== undefined) {
                    inheritedRole = this.addRoleInfo(inheritedRole, 'inherited');
                }

                if (assignedRole === undefined) {
                    return inheritedRole;
                } else if (inheritedRole === undefined) {
                    return assignedRole;
                }

                let assignedRolePriority = this.possibleRoles.findIndex((r) => r.role === assignedRole.role);
                let inheritedRolePriority = this.possibleRoles.findIndex((r) => r.role === inheritedRole.role);

                if (assignedRolePriority !== -1 && assignedRolePriority < inheritedRolePriority) {
                    return assignedRole;
                } else {
                    return inheritedRole;
                }
            },

            addRoleInfo(role, type) {
                const hasEmbargo = type === 'assigned' ? this.isEmbargoed : this.isEmbargoedParent;
                const hasDeletion = type === 'assigned' ? this.isDeleted : this.isDeletedParent;
                role.embargo = hasEmbargo;
                role.deleted = hasDeletion;

                if (hasDeletion) {
                    role.role = 'none';
                }
                if (hasEmbargo && role.role !== 'none') {
                    role.role = 'canViewMetadata';
                }

                return role;
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

                if (embargo_info === null) {
                    this.loadPreviousRole();
                }

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

        h3 {
            color: black;
            padding-top: 25px;

            &:first-child {
                padding-top: 0;
            }
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
            margin-top: 15px;
        }

        .update-roles {
            border-top: 1px dashed gray;
        }

        .set-patron-roles {
            list-style-type: none;
            text-align: left;

            li {
                display: block;
                margin-left: 15px;

                &:first-child {
                    margin-bottom: 15px;
                }

                ul {
                    border-top: none;
                    margin: 10px 27px;
                    text-align: left;

                    li {
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

    #modal-permissions-editor {
        ul {
            border-top: none;
            margin-top: 15px;
            padding-top: 0;
        }
    }
</style>