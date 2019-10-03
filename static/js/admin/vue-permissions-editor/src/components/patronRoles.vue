<template>
    <div id="patron-roles">
        <h1>Set Patron Access</h1>
        <h3>Current Settings</h3>

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
                    <i class="far fa-question-circle" :class="{hidden: nonPublicRole(inherited_role.principal )}"></i>
                    <i class="far fa-check-circle" :class="{hidden: mostRestrictive(inherited_role.principal) === 'current_object'}"></i>
                </td>
                <td>
                    {{ displayRole(inherited_role.role) }}
                    <i class="far fa-check-circle" :class="{hidden: mostRestrictive(inherited_role.principal) === 'current_object'}"></i>
                </td>
            </tr>
            <tr v-if="hasObjectRole" v-for="object_role in display_roles.current_object">
                <td>This object</td>
                <td>{{ object_role.principal }}
                    <i class="far fa-question-circle" :class="{hidden: nonPublicRole(object_role.principal)}"></i>
                    <i class="far fa-check-circle" :class="{hidden: mostRestrictive(object_role.principal) === 'parent'}"></i>
                </td>
                <td>{{ displayRole(object_role.role) }} <i class="far fa-check-circle" :class="{hidden: mostRestrictive(object_role.principal)=== 'parent'}"></i></td>
            </tr>
            </tbody>
        </table>
        <p v-else>There are no current permissions assigned</p>

        <ul class="set-patron-roles">
            <li>
                <input @click="updateRoleList" id="patron" type="radio" v-model="user_type" value="patron"> Allow patron access
                <ul>
                    <li>
                        <p class="patron">Public users</p>
                        <div class="select-wrapper" :class="{'is-disabled': staff_only}">
                            <select id="public" @change="updateRole" class="public-select" v-model="public_role" :disabled="staff_only">
                                <option v-for="role in possibleRoles" :value="role.role">{{ role.text }}</option>
                            </select>
                        </div>
                    </li>
                    <li>
                        <p>Onyen users</p>
                        <div class="select-wrapper" :class="{'is-disabled': staff_only}">
                            <select id="onyen" @change="updateRole" v-model="onyen_role" :disabled="staff_only">
                                <template v-for="(role, index) in possibleRoles">
                                    <option v-if="index > 0"  :value="role.role">{{ role.text }}</option>
                                </template>
                            </select>
                        </div>
                    </li>
                </ul>
            </li>
            <li><input @click="updateRoleList" id="staff" type="radio" v-model="user_type" value="staff"> Staff only access</li>
        </ul>
        <embargo :uuid="uuid"></embargo>
    </div>
</template>

<script>
    import embargo from "./embargo";
    import axios from 'axios';

    export default {
        name: 'patronRoles',
        components: {embargo},

        props: {
            alertHandler: Object,
            containerType: String,
            uuid: String
        },

        data() {
            return {
                display_roles: { inherited: [], current_object: [] },
                patron_roles: { inherited: [], current_object: [] },
                onyen_role: 'none',
                public_role: 'none',
                staff_only: false,
                staff_only_role_text: '\u2014',
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
                    { text: this.staff_only_role_text , role: this.staff_only_role_text }, // Only used by the display tables
                    { text: 'have no access', role: 'none' },
                    { text: 'can view metadata only', role: 'metadataOnly' },
                    { text: 'can view access copies', role: 'accessCopies' },
                    { text: `can view all of this ${container}`, role: 'allAccess' }
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
                    { principal: 'public', role: this.public_role },
                    { principal: 'onyen', role: this.onyen_role }
                ];
            }
        },

        methods: {
            displayRole(role) {
                let selected_role = this.possibleRoles.find((r) => r.role === role);
                return selected_role.text;
            },

            updateRole(e) {
                let role_type = e.target.id;
                let user_index = this.userIndex(role_type);

                if (user_index === -1) {
                    this.updatePatronRoles();
                } else {
                    this.display_roles.current_object[user_index].role = this[`${role_type}_role`];
                }
            },

            updateRoleList(e) {
                let type = e.target.id;

                if (type === 'staff') {
                    this.public_role = 'none';
                    this.onyen_role = 'none';
                }

                this.updateDisplayRoles(type);
                this.updatePatronRoles();
            },

            updateDisplayRoles(type) {
                if (type === 'staff') {
                    this.display_roles.current_object = [{ principal: 'staff', role: this.staff_only_role_text }]
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
                return text !== 'public';
            },

            userIndex(principal) {
                return this.display_roles.current_object.findIndex((role) => role.principal === principal);
            },
        }
    }
</script>

<style scoped lang="scss">
    #patron-roles {
        text-align: left;
        margin: 5px 25px;

        li {
            ul {
                margin: 10px 15px;

                li {
                    display: inline-flex;

                    p {
                        margin: auto 20px auto auto;
                    }
                }
            }
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
        }

        .select-wrapper::after {
            right: 5px;
            top: 8px;
        }

        .fa-question-circle {
            color: gray;
        }

        .fa-check-circle {
            color: limegreen;
            float: right;
            margin-right: 25px;
        }

        .is-disabled {
            opacity: .5;
        }
    }
</style>