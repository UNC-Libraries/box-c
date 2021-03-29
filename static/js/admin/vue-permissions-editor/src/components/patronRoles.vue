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
            <template v-for="user in displayAssignments">
                <patron-display-row :user="user"
                                    :user-type="user_type"
                                    :container-type="containerType"
                                    :allowed-principals="allowed_principals"></patron-display-row>
            </template>
            </tbody>
        </table>

        <h3 class="update-roles">Set Patron Access</h3>
        <ul class="set-patron-roles">
            <li v-if="!isCollection">
                <input type="radio" v-model="user_type" value="parent" :disabled="isDeleted"
                        id="user_type_parent"><label for="user_type_parent"> Inherit from parent</label>
            </li>
            <li>
                <input type="radio" v-model="user_type" value="patron" :disabled="isDeleted"
                       id="user_type_patron"><label for="user_type_patron"> Allow patron access</label>
                <ul id="assigned_principals_editor" class="patron">
                    <li v-for="(patron_princ, index) in selected_patron_assignments" v-bind:key="index" class="patron-assigned">
                        <p>{{ principalDisplayName(patron_princ.principal, allowed_principals) }}</p>
                        <div class="select-wrapper" :class="{'is-disabled': shouldDisable}">
                            <select v-model="patron_princ.role" :disabled="shouldDisable">
                                <template v-for="(role, index) in possibleRoles">
                                    <option v-if="index > 0" :value="role.role">{{ role.text }}</option>
                                </template>
                            </select>
                        </div>
                        <button class="btn-remove"
                                @click="removeAssignedPrincipal(patron_princ.principal)"
                                :disabled="shouldDisable"
                                v-if="!patron_princ.protected">Remove</button>
                    </li>
                    <li id="add-new-patron-principal" v-show="shouldShowAddPrincipal">
                        <p class="select-wrapper" :class="{'is-disabled': shouldDisable}">
                            <select id="add-new-patron-principal-id" v-model="new_assignment_principal" :disabled="shouldDisable">
                                <option v-for="princ in allowed_principals" :value="princ.principal">{{ princ.name }}</option>
                            </select>
                        </p>
                        <div class="select-wrapper" :class="{'is-disabled': shouldDisable}">
                            <select id="add-new-patron-principal-role" v-model="new_assignment_role" :disabled="shouldDisable">
                                <option v-for="role in possibleRoles" :value="role.role">{{ role.text }}</option>
                            </select>
                        </div>
                    </li>
                    <li>
                        <button @click="addPrincipal" id="add-principal" :disabled="shouldDisable">Add Group</button>
                    </li>
                </ul>
            </li>
            <li>
                <input type="radio" v-model="user_type" value="staff" :disabled="isDeleted"
                        id="user_type_staff"> <label for="user_type_staff"> Staff only access</label>
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
                        :class="{'btn-disabled': !hasUnsavedChanges}"
                        :disabled="!hasUnsavedChanges">Save Changes</button>
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

    const EVERYONE_PRINCIPAL = 'everyone';
    const AUTH_PRINCIPAL = 'authenticated';
    // Special display only principals
    const STAFF_PRINCIPAL = 'staff';
    const PATRON_PRINCIPAL = 'patron';
    const PROTECTED_PRINCIPALS = [EVERYONE_PRINCIPAL, AUTH_PRINCIPAL];
    const PROTECTED_PRINCIPALS_SET = new Set(PROTECTED_PRINCIPALS);

    const STAFF_ONLY_ROLE = 'none';
    const VIEW_METADATA_ROLE = 'canViewMetadata';
    const VIEW_ACCESS_COPIES_ROLE = 'canViewAccessCopies';
    const VIEW_ORIGINAL_ROLE = 'canViewOriginals';
    const DEFAULT_ROLE = VIEW_ORIGINAL_ROLE;

    const ACCESS_TYPE_INHERIT = "parent";
    const ACCESS_TYPE_STAFF_ONLY = "staff";
    const ACCESS_TYPE_DIRECT = "patron";

    let initialRoles = () => ({ roles: [], embargo: null, deleted: false });
    const DEFAULT_DISPLAY_COLLECTION = [
        { principal: EVERYONE_PRINCIPAL, role: STAFF_ONLY_ROLE, assignedTo: null},
        { principal: AUTH_PRINCIPAL, role: STAFF_ONLY_ROLE, assignedTo: null},
    ];

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

        created() {
            // Initialize non-reactive variables
            this.inherited = initialRoles();
        },

        data() {
            return {
                allowed_principals: [],
                selected_patron_assignments: [],
                embargo: null,
                deleted: false,
                new_assignment_role: VIEW_ORIGINAL_ROLE,
                new_assignment_principal: '',
                response_message: '',
                user_type: null,
                shouldShowAddPrincipal: false,
                saved_details: null
            }
        },

        computed: {
            possibleRoles() {
                return this.possibleRoleList(this.containerType);
            },

            displayAssignments() {
                let assigned = cloneDeep(this.assignedPatronRoles);
                // Display the new assignment before it is committed, if valid
                if (this.shouldShowAddPrincipal) {
                    try {
                        assigned.push(this.getNewAssignment());
                    } catch (e) {
                        // ignore invalid assignments
                    }
                }
                let inherited = cloneDeep(this.inherited.roles);
                // If no roles available for a collection, revert to defaults
                if (inherited.length == 0 && assigned.length == 0 && this.isCollection) {
                    assigned = cloneDeep(DEFAULT_DISPLAY_COLLECTION);
                }
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
                    return this.getInheritedPrincipals()
                        .map(princ => ({ principal: princ, role: STAFF_ONLY_ROLE, assignedTo: this.uuid }));
                } else {
                    return this.selected_patron_assignments
                               .map(pa => ({ principal: pa.principal, role: pa.role, assignedTo: this.uuid }));
                }
            },

            shouldDisable() {
                return this.user_type === ACCESS_TYPE_STAFF_ONLY || this.user_type === ACCESS_TYPE_INHERIT;
            },

            isEmbargoed() {
                return this.embargo !== null;
            },

            isEmbargoedParent() {
                return this.inherited.embargo !== null;
            },

            isDeleted() {
                return this.deleted || this.inherited.deleted;
            },

            isCollection() {
                return this.containerType.toLowerCase() === 'collection';
            },

            hasUnsavedChanges() {
                // return false if the saved state hasn't been loaded yet
                if (this.saved_details === null) {
                    return false;
                }
                if (this.embargo !== this.saved_details.embargo) {
                    return true;
                }
                if (this.new_assignment_principal !== '') {
                    return true;
                }
                let assignedPatrons = this.assignedPatronRoles;
                if (assignedPatrons.length != this.saved_details.roles.length) {
                    return true;
                }
                for (let i = 0; i < assignedPatrons.length; i++) {
                    let saved = this.saved_details.roles[i];
                    let assigned = assignedPatrons[i];
                    if (saved.principal != assigned.principal) {
                        return true;
                    }
                    if (saved.role != assigned.role) {
                        return true;
                    }
                }
                return false;
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
                    this._initializeInherited(response.data.inherited);
                    this.deleted = response.data.assigned.deleted;
                    this._initializeSelectedAssignments(response.data.assigned.roles);
                    this.allowed_principals = response.data.allowedPrincipals;
                    this.saved_details = this.submissionAccessDetails();
                }).catch((error) => {
                    let response_msg = `Unable to load current patron roles for: ${this.title}`;
                    this.alertHandler.alertHandler('error', response_msg);
                    console.log(error);
                });
            },

            _initializeInherited(inherited) {
                if (this.isCollection) {
                    return;
                }
                // Merge the incoming non-null inherited details into the defaults
                if (inherited != null) {
                    Object.keys(inherited).forEach(key => {
                        if (inherited[key] !== null) {
                            this.inherited[key] = inherited[key];
                        }
                    })
                }
                // Add any missing protected principals for non-collection objects
                for (let princ of PROTECTED_PRINCIPALS) {
                    if (!this.inherited.roles.some(r => r.principal === princ)) {
                        this.inherited.roles.push({
                            principal: princ,
                            role: STAFF_ONLY_ROLE,
                            assignedTo: null
                        });
                    }
                }
            },

            _initializeSelectedAssignments(assignedRoles) {
                // Set the main patron access setting mode type
                if (assignedRoles.length === 0) {
                    if (this.isCollection) {
                        this.user_type = ACCESS_TYPE_STAFF_ONLY;
                    } else {
                        this.user_type = ACCESS_TYPE_INHERIT;
                    }
                } else if (assignedRoles.every(r => r.role === STAFF_ONLY_ROLE)) {
                    this.user_type = ACCESS_TYPE_STAFF_ONLY;
                } else {
                    this.user_type = ACCESS_TYPE_DIRECT;
                }

                // Ensure that the basic protected principals are always present
                let principal_set = new Set(this.getInheritedPrincipals());
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
                        role: assignment !== undefined ? assignment.role : defaultRole,
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
            getInheritedPrincipals() {
                let inherited = new Set(PROTECTED_PRINCIPALS);
                this.inherited.roles.forEach(r => inherited.add(r.principal));
                return Array.from(inherited);
            },

            // Returns true if the principal provided is protected, meaning it should always be present
            isProtectedPrincipal(principal) {
                return PROTECTED_PRINCIPALS_SET.has(principal);
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
                // Commit uncommitted new group assignment if one was input
                if (this.shouldShowAddPrincipal && this.new_assignment_principal !== '') {
                    this.addPrincipal();
                }
                let submissionDetails = this.submissionAccessDetails();

                axios({
                    method: 'put',
                    url: `/services/api/edit/acl/patron/${this.uuid}`,
                    data: JSON.stringify(submissionDetails),
                    headers: {'content-type': 'application/json; charset=utf-8'}
                }).then((response) => {
                    let response_msg = `Patron roles successfully updated for: ${this.title}`;
                    this.alertHandler.alertHandler('success', response_msg);
                    this.is_submitting = false;
                    this.response_message = '';
                    this.saved_details = submissionDetails;

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

            /**
             * Return array of winning roles
             * @param allRoles
             * @returns {array}
             */
            winningRoleList(allRoles) {
                if (allRoles.length == 0) {
                    return [];
                }
                let principalsPresent = Array.from(new Set(allRoles.map(r => r.principal)));
                let winningRoles = principalsPresent.map(p => allRoles.filter(r => r.principal === p))
                                                    .map(roles => this.winningRole(roles));
                let firstRole = winningRoles[0];
                let allSame = true;
                for (let i = 1; i < winningRoles.length; i++) {
                    let current = winningRoles[i];
                    if (!(current.role === firstRole.role &&
                            ((current.type === 'inherited' && firstRole.type === 'inherited') ||
                            (current.type === 'assigned' && firstRole.type === 'assigned')))) {
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
                    // if a custom patron principal is not inherited, default to staff only unless this is a collection
                    if (this.isProtectedPrincipal(assignedRole.principal) || this.isCollection) {
                        return assignedRole;
                    } else {
                        return {
                            principal: assignedRole.principal,
                            role: STAFF_ONLY_ROLE,
                            deleted: false,
                            embargo: false,
                            assignedTo: null,
                            type: 'inherited'
                        };
                    }
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
                const hasDeletion = type === 'assigned' ? this.deleted : this.inherited.deleted;
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
            },

            /**
             * Wrap so it can share a watcher with patronRoles.vue
             * See mixins/displayModal.js
             */
            showModal() {
                this.displayModal();
            },
            
            addPrincipal() {
                if (!this.shouldShowAddPrincipal) {
                    this.shouldShowAddPrincipal = true;
                    return false;
                }
                try {
                    let new_assignment = this.getNewAssignment();
                    this.selected_patron_assignments.push(new_assignment);
                    this.new_assignment_principal = '';
                    this.new_assignment_role = VIEW_ORIGINAL_ROLE;
                } catch (e) {
                    this.alertHandler.alertHandler('error', e);
                    return false;
                }
            },

            getNewAssignment() {
                if (this.new_assignment_principal === '' || this.new_assignment_role === '') {
                    throw 'Must select both a group and a role when adding a new patron role assignment';
                }
                if (this.new_assignment_principal !== '') {
                    if (this.selected_patron_assignments.some(r => r.principal === this.new_assignment_principal)) {
                        throw 'Principal has already been assigned a role';
                    }
                }
                return {
                    principal: this.new_assignment_principal,
                    role: this.new_assignment_role,
                    assignedTo: this.uuid
                };
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
                margin-bottom: 15px;

                ul {
                    border-top: none;
                    margin: 10px 27px;
                    text-align: left;

                    li {
                        display: flex;
                        text-indent: 0;
                        margin-left: 0;
                        margin-bottom: 8px;

                        p {
                            min-width: 170px;
                            margin: auto 20px auto 0;

                            select {
                                margin-left: 0;
                            }
                        }
                    }
                }

                .patron-assigned {
                    button {
                        position: relative;
                    }
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

        button:disabled,
        button[disabled] {
            opacity: .3;
            &:hover {
                cursor: not-allowed;
                opacity: .5
            }
        }

        p.error {
            color: red;
        }

        #add-new-patron-principal-id {
            min-width: 170px;
            width: auto;
        }
    }

    #add-principal {
        margin-left: 0;
    }

    #modal-permissions-editor {
        ul {
            border-top: none;
            margin-top: 15px;
            padding-top: 0;
        }
    }
</style>