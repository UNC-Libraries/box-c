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
            <tr v-if="hasParentRole" v-for="inherited_role in display_roles.inherited">
                <td>From parent</td>
                <td>
                    {{ inherited_role.principal }}
                    <i class="far fa-question-circle" :class="{hidden: nonPublicRole(inherited_role.principal)}"></i>
                    <span class="permission-icons">
                        <i class="far fa-check-circle"
                           :class="{hidden: mostRestrictive(inherited_role.principal) === 'current_object'}"></i>
                    </span>
                </td>
                <td>
                    {{ displayRole(inherited_role.role) }}
                    <span class="permission-icons">
                        <i class="far fa-circle"><span>E</span></i>
                        <i class="far fa-check-circle"
                           :class="{hidden: mostRestrictive(inherited_role.principal) === 'current_object'}"></i>
                    </span>
                </td>
            </tr>
            <tr v-if="hasObjectRole" v-for="object_role in display_roles.current_object">
                <td>This object</td>
                <td>{{ object_role.principal }}
                    <i class="far fa-question-circle" :class="{hidden: nonPublicRole(object_role.principal)}"></i>
                    <span class="permission-icons">
                        <i class="far fa-check-circle"
                           :class="{hidden: mostRestrictive(object_role.principal) === 'parent'}"></i>
                    </span>
                </td>
                <td>{{ displayRole(object_role.role) }}
                    <span class="permission-icons">
                        <i class="far fa-circle"><span>E</span></i>
                        <i class="far fa-check-circle"
                           :class="{hidden: mostRestrictive(object_role.principal)=== 'parent'}"></i>
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

        <embargo :uuid="uuid"></embargo>
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
                display_roles: { inherited: [], current_object: [] },
                patron_roles: { inherited: [], current_object: [] },
                onyen_role: 'none',
                patrons_role: 'none',
                staff_only: false,
                user_type: ''
            }
        },

        computed: {
            possibleRoles() {
                let container;
                if (this.containerType === 'adminUnit') {
                    container = 'admin unit';
                } else {
                    container = this.containerType.toLowerCase();
                }

                return [
                    { text: STAFF_ONLY_ROLE_TEXT , role: STAFF_ONLY_ROLE_TEXT }, // Only used by the display tables
                    { text: 'No Access', role: 'none' },
                    { text: 'Metadata Only', role: 'metadataOnly' },
                    { text: 'Access Copies', role: 'accessCopies' },
                    { text: `All of this ${container}`, role: 'allAccess' }
                ]
            },

            hasParentRole() {
                return this.display_roles.inherited.length > 0;
            },

            hasObjectRole() {
                return this.display_roles.current_object.length > 0;
            },

            assignedPatronRoles() {
                return [
                    { principal: 'Patrons', role: this.patrons_role },
                    { principal: 'Onyen', role: this.onyen_role }
                ];
            },

            shouldDisable() {
                return this.staff_only || this.user_type === '';
            }
        },

        methods: {
            getRoles() {

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

            displayRole(role) {
                let selected_role = this.possibleRoles.find((r) => r.role === role);
                return selected_role.text;
            },

            updateRoleList(e) {
                this.unsaved_changes = true;
                let type = e.target.id;

                if (type === 'staff') {
                    this.patrons_role = 'none';
                    this.onyen_role = 'none';
                }

                this.updateDisplayRoles(type);
                this.updatePatronRoles();
            },

            updateRole(principal) {
                let user_index = this.userIndex(principal);

                if (user_index !== -1) {
                    this.display_roles.current_object[user_index].role = this[`${principal}_role`];
                }

                this.updatePatronRoles();
            },

            updateDisplayRoles(type) {
                if (type === 'staff') {
                    this.display_roles.current_object = [{ principal: 'staff', role: STAFF_ONLY_ROLE_TEXT }]
                } else {
                    this.display_roles.current_object = this.assignedPatronRoles;
                }
            },

            updatePatronRoles() {
                this.patron_roles.current_object = this.assignedPatronRoles;
            },

            currentUserRoles(user = 'staff') {
                let inherited = this.display_roles.inherited.find((u) => u.principal === user);
                let current_object = this.display_roles.current_object.find((u) => u.principal === user);

                return { inherited: inherited, current_object: current_object };
            },

            hasStaffOnly() {
                let current_users = this.currentUserRoles();

                if (current_users.inherited !== undefined) {
                    return 'parent';
                } else if (current_users.current_object !== undefined) {
                    return 'current_object'
                } else {
                    return undefined;
                }
            },

            hasMultipleRoles(current_users) {
                let inherited_role = this.possibleRoles.findIndex((r) => r.role === current_users.inherited.role);
                let current_object_role = this.possibleRoles.findIndex((r) => r.role === current_users.current_object.role);

                if (current_object_role !== -1 && current_object_role < inherited_role) {
                    return 'current_object';
                } else {
                    return 'parent';
                }
            },

            hasRolesPriority(user) {
                let current_users = this.currentUserRoles(user);

                if (current_users.inherited === undefined && current_users.current_object === undefined) {
                    return 'none';
                } else if (current_users.inherited !== undefined && current_users.current_object === undefined) {
                    return 'parent';
                } else if (current_users.inherited === undefined && current_users.current_object !== undefined) {
                    return 'current_object';
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

            userIndex(principal) {
                return this.display_roles.current_object.findIndex((user) => user.principal.toLowerCase() === principal);
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

        .fa-question-circle {
            color: gray;

            &:hover {
                cursor: pointer;
            }
        }

        .fa-check-circle {
            color: limegreen;
            margin-left: 10px;
        }

        .fa-circle {
            color: red;

            span {
                font-size: 12px;
                margin-left: -11px;
            }
        }

        .permission-icons {
            float: right;
            margin-right: 20px;
        }

        .is-disabled {
            cursor: not-allowed;
            opacity: .5;
        }
    }
</style>