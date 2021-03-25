<template>
    <div id="patron-roles">
        <h3>Effective Patron Access</h3>

        <table class="border inherited-permissions">
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

        <h3 class="update-roles">Set Patron Access</h3>
        <ul class="set-patron-roles">
            <li v-if="!isCollection">
                <span id="parent">
                    <input type="radio" v-model="user_type" value="parent" :disabled="isDeleted"> Inherit from parent
                </span>
            </li>
            <li>
                <span id="patron">
                    <input type="radio" v-model="user_type" value="patron" :disabled="isDeleted"> Allow patron access
                </span>
                <ul class="patron">
                    <li id="assigned_principals_editor">
                        <div v-for="(patron_princ, index) in selected_patron_assignments" v-bind:key="index" class="patron-assigned">
                            <p>{{ principalDisplayName(patron_princ.principal, allowed_other_principals) }}</p>
                            <select v-model="patron_princ.role" :disabled="shouldDisable">
                                <template v-for="(role, index) in possibleRoles">
                                    <option v-if="index > 0" :value="role.role">{{ role.text }}</option>
                                </template>
                            </select>
                            <button class="btn-remove"
                                    @click="removeAssignedPrincipal(patron_princ.principal)"
                                    v-if="!patron_princ.protected">Remove</button>
                        </div>
                        <div id="add-new-patron-principal" v-show="shouldShowAddOtherPrincipals">
                            <select id="add-new-patron-principal-id" v-model="add_new_princ_id" :disabled="shouldDisable">
                                <option v-for="princ in allowed_other_principals" :value="princ.id">{{ princ.name }}</option>
                            </select>
                            <select id="add-new-patron-principal-role" v-model="add_new_princ_role" :disabled="shouldDisable">
                                <option v-for="role in possibleRoles" :value="role.role">{{ role.text }}</option>
                            </select>
                        </div>
                        
                        <button @click="addOtherPrincipal" id="add-other-principal" :disabled="shouldDisable">Add Other Group</button>
                    </li>
                </ul>
            </li>
            <li>
                <span id="staff">
                    <input type="radio" v-model="user_type" value="staff" :disabled="isDeleted"> Staff only access
                </span>
            </li>
        </ul>

        <embargo ref="embargoInfo"
                 :current-embargo="embargo"
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
    const EVERYONE_PRINCIPAL = 'everyone';
    const AUTH_PRINCIPAL = 'authenticated';
    // Special display only principals
    const STAFF_PRINCIPAL = 'staff';
    const PATRON_PRINCIPAL = 'patron';
    const PROTECTED_PRINCIPALS = new Set([EVERYONE_PRINCIPAL, AUTH_PRINCIPAL]);

    const STAFF_ONLY_ROLE = 'none';
    const VIEW_METADATA_ROLE = 'canViewMetadata';
    const VIEW_ACCESS_COPIES_ROLE = 'canViewAccessCopies';
    const VIEW_ORIGINAL_ROLE = 'canViewOriginals';
    const DEFAULT_ROLE = VIEW_ORIGINAL_ROLE;

    const ACCESS_TYPE_INHERIT = "parent";
    const ACCESS_TYPE_STAFF_ONLY = "staff";
    const ACCESS_TYPE_DIRECT = "patron";

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
            actionHandler: Object,
            alertHandler: Object,
            changesCheck: Boolean,
            containerType: String,
            resultObject: Object,
            title: String,
            uuid: String
        },

        data() {
            return {
                patron_roles: {
                    inherited: initialRoles(),
                },
                allowed_other_principals: [],
                selected_patron_assignments: [],
                embargo: null,
                deleted: false,
                deleted_inherited: false,
                add_new_princ_role: VIEW_ORIGINAL_ROLE,
                add_new_princ_id: '',
                response_message: '',
                unsaved_changes: false,
                user_type: null,
                shouldShowAddOtherPrincipals: false
            }
        },

        computed: {
            possibleRoles() {
                return this.possibleRoleList(this.containerType);
            },

            dedupedRoles() {
                let assigned = cloneDeep(this.assignedPatronRoles);
                // demote assigned roles if embargoed
                if (this.embargo !== null || this.patron_roles.inherited.embargo !== null) {
                    assigned.forEach(assignment => {
                        if (assignment.role === VIEW_ORIGINAL_ROLE || assignment.role === VIEW_ACCESS_COPIES_ROLE) {
                            assignment.role = VIEW_METADATA_ROLE;
                        }
                    });
                }
                let inherited = cloneDeep(this.patron_roles.inherited.roles);
                this.setRoleType(assigned, 'assigned');
                this.setRoleType(inherited, 'inherited');

                return this.winningRoleList(inherited.concat(assigned));
            },

            /**
             * Returns the current state of roles assigned to patrons
             * @returns {*[]}
             */
            assignedPatronRoles() {
                if (this.user_type === ACCESS_TYPE_INHERIT) {
                    return [];
                } else if (this.user_type === ACCESS_TYPE_STAFF_ONLY) {
                    return Array.from(this.getInheritedPrincipalSet())
                                .map(princ => ({ principal: princ, role: STAFF_ONLY_ROLE, assignedTo: this.uuid }));
                } else {
                    return this.selected_patron_assignments
                               .map(pa => ({ principal: pa.principal, role: pa.role, assignedTo: this.uuid }));
                }
            },

            shouldDisable() {
                return this.user_type === ACCESS_TYPE_STAFF_ONLY || this.user_type === ACCESS_TYPE_INHERIT
                    || this.isDeleted;
            },

            isEmbargoed() {
                return this.embargo !== null;
            },

            isEmbargoedParent() {
                return this.patron_roles.inherited.embargo !== null;
            },

            isDeleted() {
                return this.deleted || this.deleted_inherited;
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
                    this.embargo = response.data.assigned.embargo;
                    this.patron_roles = {
                        inherited: cloneDeep(response.data.inherited)
                    };
                    this.deleted = response.data.assigned.deleted;
                    this.deleted_inherited = response.data.inherited.deleted;
                    this._initializeSelectedAssignments(response.data.assigned.roles);
                    this.allowed_other_principals = response.data.allowedPrincipals;
                }).catch((error) => {
                    let response_msg = `Unable to load current patron roles for: ${this.title}`;
                    this.alertHandler.alertHandler('error', response_msg);
                    console.log(error);
                });
            },

            _initializeSelectedAssignments(assignedRoles) {
                // Set the main patron access setting mode type
                if (assignedRoles.length === 0) {
                    this.user_type = ACCESS_TYPE_INHERIT;
                } else if (assignedRoles.every(r => r.role === STAFF_ONLY_ROLE)) {
                    this.user_type = ACCESS_TYPE_STAFF_ONLY;
                } else {
                    this.user_type = ACCESS_TYPE_DIRECT;
                }

                // Ensure that the basic protected principals are always present
                let principal_set = this.getInheritedPrincipalSet();
                // Add in all inherited and assigned principals
                assignedRoles.forEach(r => principal_set.add(r.principal));
                // Sort the principals so they are in the order: everyone, authenticated, everything else
                let principals = Array.from(principal_set).sort(this.principalComparator);
                // populate the initial selected assignments for the "allow patron access" section
                let defaultRole = this.user_type === ACCESS_TYPE_STAFF_ONLY ? STAFF_ONLY_ROLE : DEFAULT_ROLE;
                for (let principal of principals) {
                    let assignment = assignedRoles.find(r => r.principal === principal);
                    this.selected_patron_assignments.push({
                        principal: principal,
                        role: assignment !== null ? assignment.role : defaultRole,
                        protected: this.isProtectedPrincipal(principal)
                    });
                }
            },

            principalComparator(a, b) {
                if (a === EVERYONE_PRINCIPAL) {
                    return -1;
                }
                if (a === AUTH_PRINCIPAL) {
                    return b === EVERYONE_PRINCIPAL ? 1 : -1;
                }
                return a.localeCompare(b);
            },

            // Returns a set containing all inherited patron principals. Adds the basic principals if not present
            getInheritedPrincipalSet() {
                let principal_set = new Set(PROTECTED_PRINCIPALS);
                this.patron_roles.inherited.roles.forEach(r => principal_set.add(r.principal));
                return principal_set;
            },

            // Returns true if the principal provided is protected, meaning it should always be present
            isProtectedPrincipal(principal) {
                return PROTECTED_PRINCIPALS.has(principal);
            },

            submissionAccessDetails() {
                return {
                    roles: cloneDeep(this.assignedPatronRoles),
                    deleted: this.deleted,
                    embargo: this.embargo,
                    assignedTo: this.uuid
                };
            },

            saveRoles() {
                this.is_submitting = true;
                this.response_message = 'Saving permissions \u2026';

                axios({
                    method: 'put',
                    url: `/services/api/edit/acl/patron/${this.uuid}`,
                    data: JSON.stringify(this.submissionAccessDetails()),
                    headers: {'content-type': 'application/json; charset=utf-8'}
                }).then((response) => {
                    let response_msg = `Patron roles successfully updated for: ${this.title}`;
                    this.alertHandler.alertHandler('success', response_msg);
                    this.unsaved_changes = false;
                    this.is_submitting = false;
                    this.response_message = '';

                    // Update entry in results table
                    this.actionHandler.addEvent({
                        action : 'RefreshResult',
                        target : this.resultObject,
                        waitForUpdate : true
                    });
                }).catch((error) => {
                    let response_msg = `Unable to update patron roles for: ${this.title}`;
                    this.is_submitting = false;
                    this.alertHandler.alertHandler('error', response_msg);
                    console.log(error);
                });
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

            /**
             * Return array of winning roles
             * @param allRoles
             * @returns {array}
             */
            winningRoleList(allRoles) {
                let principalsPresent = Array.from(new Set(allRoles.map(r => r.principal)));
                let winningRoles = principalsPresent.map(p => allRoles.filter(r => r.principal === p))
                                                    .map(roles => this.winningRole(roles));
                let firstRole = winningRoles[0];
                let allSame = false;
                for (let i = 1; i < winningRoles.length; i++) {
                    let current = winningRoles[i];
                    if (current.role === firstRole.role &&
                        ((current.type === 'inherited' && firstRole.type === 'inherited') ||
                            (current.type === 'assigned' && firstRole.type === 'assigned'))) {
                        allSame = true;
                    } else {
                        allSame = false;
                        break;
                    }
                }

                if (allSame) {
                    firstRole.principal = firstRole.role === STAFF_ONLY_ROLE ? ACCESS_TYPE_STAFF_ONLY : ACCESS_TYPE_DIRECT;
                    return [firstRole];
                } else {
                    return winningRoles;
                }
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
                const hasDeletion = type === 'assigned' ? this.deleted : this.deleted_inherited;
                role.embargo = hasEmbargo;
                role.deleted = hasDeletion;

                if (hasDeletion) {
                    role.role = STAFF_ONLY_ROLE;
                }
                if (hasEmbargo && role.role !== STAFF_ONLY_ROLE) {
                    role.role = VIEW_METADATA_ROLE;
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
                this.embargo = embargo_info;

                // if (embargo_info === null) {
                //     this.loadPreviousRole();
                // }

                // this.setUnsavedChanges();
            },

            /**
             * Wrap so it can share a watcher with patronRoles.vue
             * See mixins/displayModal.js
             */
            showModal() {
                this.displayModal();
            },
            
            addOtherPrincipal() {
                if (!this.shouldShowAddOtherPrincipals) {
                    this.shouldShowAddOtherPrincipals = true;
                    return false;
                }
                if (this.add_new_princ_id !== '') {
                    if (this.selected_patron_assignments.some(r => r.principal === this.add_new_princ_id)) {
                        this.$emit('error-msg', "Principal has already been assigned a role");
                        return false;
                    }
                    this.selected_patron_assignments.push({
                        principal: this.add_new_princ_id,
                        role: this.add_new_princ_role,
                        assignedTo: this.uuid
                    });
                    this.add_new_princ_id = '';
                    this.add_new_princ_role = VIEW_ORIGINAL_ROLE;
                }
            },

            removeAssignedPrincipal(principal) {
                let index = this.selected_patron_assignments.findIndex(r => r.principal === principal);
                if (index == -1) {
                    return;
                }
                this.selected_patron_assignments.splice(index, 1);
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