<template>
    <div id="patron-roles">
        <h1>Set Patron Access</h1>
        <h3 v-if="hasParentRole || hasObjectRole">Current Settings</h3>

        <table v-if="hasParentRole || hasObjectRole" class="border inherited-permissions">
            <thead>
                <tr>
                    <th></th>
                    <th>Who can access</th>
                    <th>What can be viewed</th>
                </tr>
            </thead>
            <tbody>
            <tr v-if="hasParentRole" v-for="inherited_role in display_roles.inherited.roles">
                <td>From parent</td>
                <td>
                    {{ inherited_role.principal }}
                    <a href="#" class="display-note-btn" :class="{hidden: nonPublicRole(inherited_role.principal)}">
                        <i class="far fa-question-circle"></i>
                        <div class="arrow"></div>
                        <span class="browse-tip">What this means</span>
                    </a>

                    <span class="permission-icons">
                        <i class="far fa-check-circle"
                           v-if="mostRestrictive(inherited_role.principal) === 'parent'"></i>
                    </span>
                </td>
                <td>
                    {{ displayRole(inherited_role.role) }}
                    <span class="permission-icons">
                        <i class="far fa-times-circle" :class="{hidden: !display_roles.inherited.deleted}"></i>
                        <i class="far fa-circle" :class="{hidden: hasEmbargo('parent')}">
                            <div :class="{'custom-icon-offset': mostRestrictive(inherited_role.principal) === 'assigned'}">e</div>
                        </i>
                        <i class="far fa-check-circle"
                           v-if="mostRestrictive(inherited_role.principal) === 'parent'"></i>
                    </span>
                </td>
            </tr>
            <tr v-if="hasObjectRole" v-for="object_role in display_roles.assigned.roles">
                <td>This object</td>
                <td>{{ object_role.principal }}
                    <a href="#" class="display-note-btn" :class="{hidden: nonPublicRole(object_role.principal)}">
                        <i class="far fa-question-circle"></i>
                        <div class="arrow"></div>
                        <span class="browse-tip">What this means</span>
                    </a>
                    <span class="permission-icons">
                        <i class="far fa-check-circle"
                           v-if="mostRestrictive(object_role.principal) === 'assigned'"></i>
                    </span>
                </td>
                <td>{{ displayRole(object_role.role) }}
                    <span class="permission-icons">
                        <i class="far fa-times-circle" :class="{hidden: !display_roles.assigned.deleted}"></i>
                        <i class="far fa-circle" :class="{hidden: hasEmbargo('object')}">
                            <div :class="{'custom-icon-offset': mostRestrictive(object_role.principal) === 'parent'}">e</div>
                        </i>
                        <i class="far fa-check-circle"
                           v-if="mostRestrictive(object_role.principal) === 'assigned'"></i>
                    </span>
                </td>
            </tr>
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

        <embargo :uuid="uuid" @embargo-info="setEmbargo"></embargo>
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
    import embargo from "./embargo";
    import displayModal from "../mixins/displayModal";
    import axios from 'axios';
    import cloneDeep from 'lodash.clonedeep';
    import isEmpty from 'lodash.isempty';

    const STAFF_ONLY_ROLE_TEXT = '\u2014';

    export default {
        name: 'patronRoles',

        components: {embargo},

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
                    { principal: 'Patrons', role: this.patrons_role },
                    { principal: 'Onyen', role: this.onyen_role }
                ];
            },

            shouldDisable() {
                return this.user_type === 'Staff' || this.user_type === '';
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

            setRoles() {
                this.is_submitting = true;
                this.response_message = 'Saving permissions \u2026';

                setTimeout(() => {
                    this.is_submitting = false;
                    this.unsaved_changes = false;
                    this.response_message = '';
                }, 3000);
            },

            getRoles() {
               axios.get(`/services/api/acl/patron/${this.uuid}`).then((response) => {
                   if (response.data.inherited.roles.length === 0 && response.data.assigned.roles.length === 0) {
                       let set_roles = [
                           { principal: 'Patrons', role: 'canAccess' },
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
                   }

                   // Set values for forms from retrieved data
                   this.patrons_role = this.setCurrentObjectRole('Patrons');
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

            /**
             *
             */
            displayRolesMerge(users) {
                if (users.length === 2 && users[0].role === users[1].role) {
                    return [{ principal: 'Patrons', role: users[0].role }];
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

            displayRole(role) {
                let selected_role = this.possibleRoles.find((r) => r.role === role);
                return selected_role.text;
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

                this.display_roles.assigned.roles = this.displayRolesMerge(this.assignedPatronRoles);
                this.updateSubmitRoles();
            },

            updateDisplayRoles(type) {
                if (type === 'staff') {
                    this.display_roles.assigned.roles = [{ principal: 'Staff', role: STAFF_ONLY_ROLE_TEXT }]
                } else {
                    this.display_roles.assigned.roles = this.displayRolesMerge(this.assignedPatronRoles);
                }
            },

            updateSubmitRoles() {
                this.submit_roles = this.assignedPatronRoles;
            },

            currentUserRoles(user = 'Staff') {
                let inherited = this.display_roles.inherited.roles.find((u) => u.principal === user);
                let assigned = this.display_roles.assigned.roles.find((u) => u.principal === user);

                return { inherited: inherited , assigned: assigned };
            },

            hasStaffOnly() {
                let current_users = this.currentUserRoles();

                if (current_users.inherited !== undefined) {
                    return 'parent';
                } else if (current_users.assigned !== undefined) {
                    return 'assigned'
                } else {
                    return undefined;
                }
            },

            hasMultipleRoles(current_users) {
                let inherited_role = this.possibleRoles.findIndex((r) => r.role === current_users.inherited.role);
                let assigned_role = this.possibleRoles.findIndex((r) => r.role === current_users.assigned.role);

                if (assigned_role !== -1 && assigned_role < inherited_role) {
                    return 'assigned';
                } else {
                    return 'parent';
                }
            },

            hasRolesPriority(user) {
                let current_users = this.currentUserRoles(user);

                if (current_users.inherited === undefined && current_users.assigned === undefined) {
                    return 'none';
                } else if (current_users.inherited !== undefined && current_users.assigned === undefined) {
                    return 'parent';
                } else if (current_users.inherited === undefined && current_users.assigned !== undefined) {
                    return 'assigned';
                } else {
                    return this.hasMultipleRoles(current_users);
                }
            },

            mostRestrictive(user) {
                // Check for staff roles. They supersede all other roles
                let has_staff_only = this.hasStaffOnly();

                if (has_staff_only !== undefined) {
                    return has_staff_only;
                }

                // Check for other users/roles
                return this.hasRolesPriority(user);
            },

            nonPublicRole(text) {
                return text !== 'Patrons';
            },

            hasEmbargo(type) {
                return isEmpty(this[`${type}_embargo_info`]);
            },

            userIndex(principal) {
                return this.display_roles.assigned.roles.findIndex((user) => {
                  return user.principal.toLowerCase() === principal.toLowerCase();
                });
            },

            setEmbargo(embargo_info) {
                this.object_embargo_info = embargo_info;

                if (!isEmpty(embargo_info)) {
                    this.display_roles.assigned.roles = [{principal: 'Public', role: 'canViewMetadata'}];
                } else {
                    this.display_roles.assigned.roles = this.assignedPatronRoles;
                }
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

        td:last-child {
            min-width: 225px;
        }

        .patron {
            margin-left: 15px;
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

        .fa-times-circle {
            color: red;
        }

        .fa-question-circle {
            color: gray;

            &:hover {
                cursor: pointer;
            }
        }

        .fa-check-circle {
            color: limegreen;
            margin-left: 8px;
        }

        .fa-circle {
            margin-left: 4px;

            div {
                display: inline-block;
                font-weight: bold;
                margin-left: -10px;
                position: relative;
                top: -2px;
            }
        }

        .permission-icons {
            float: right;
            margin-right: 20px;
            text-align: right;
            width: 55px;
        }

        .custom-icon-offset {
            margin-right: 4px;
        }

        .is-disabled {
            cursor: not-allowed;
            opacity: .5;
        }

        .arrow {
            border-left: 5px solid transparent;
            border-right: 5px solid transparent;
            border-bottom: 10px solid darkslategray;
            height: 0;
            margin: 2px 2px 0 60px;
            width: 0;
        }

        .browse-tip, .arrow {
            display: none;
        }

        a.display-note-btn:hover {
            .arrow, .browse-tip {
                display: block;
                position: absolute;
                z-index: 10009;
            }

            .browse-tip {
                background-color: white;
                border: 1px solid darkslategray;
                border-radius: 5px;
                color: black;
                font-weight: normal;
                margin: 10px 0 0 -65px;
                padding: 10px;
                text-align: left;
                width: 240px;
            }
        }
    }
</style>