<template>
    <tr>
        <td>From {{ type }}</td>
        <td class="access-display">
            {{ user.principal }}
            <a href="#" class="display-note-btn" :class="{hidden: nonPublicRole(user.principal)}">
                <i class="far fa-question-circle"></i>
                <div class="arrow"></div>
                <span class="browse-tip">What this means</span>
            </a>

            <span class="permission-icons">
                        <i class="far fa-check-circle"
                           v-if="mostRestrictive(user.principal) === type"></i>
                    </span>
        </td>
        <td>
            {{ displayRole(user.role) }}
            <span class="permission-icons">
                        <i class="far fa-times-circle" :class="{hidden: !deleted}"></i>
                        <i class="far fa-circle" :class="{hidden: !embargoed}">
                            <div :class="{'custom-icon-offset': mostRestrictive(user.principal) !== type}">e</div>
                        </i>
                        <i class="far fa-check-circle"
                           v-if="mostRestrictive(user.principal) === type"></i>
                    </span>
        </td>
    </tr>
</template>

<script>
    export default {
        name: 'patronDisplayRow',
        
        props: {
            deleted: Boolean,
            displayRoles: Object,
            embargoed: Boolean,
            possibleRoles: Array,
            type: String,
            user: Object,
        },
        
        methods: {
            nonPublicRole(text) {
                return text !== 'Public';
            },

            currentUserRoles(user = 'Staff') {
                let inherited = this.displayRoles.inherited.roles.find((u) => u.principal === user);
                let assigned = this.displayRoles.assigned.roles.find((u) => u.principal === user);

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

            displayRole(role) {
                let selected_role = this.possibleRoles.find((r) => r.role === role);
                return selected_role.text;
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
            }
        }
    }
</script>

<style scoped lang="scss">
    td:last-child {
        min-width: 225px;
    }

    .access-display {
        max-width: 100px;
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

    .browse-tip, .arrow {
        display: none;
    }

    .arrow {
        border-left: 5px solid transparent;
        border-right: 5px solid transparent;
        border-bottom: 10px solid darkslategray;
        height: 0;
        margin: 2px 2px 0 60px;
        width: 0;
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
</style>