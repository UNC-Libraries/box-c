<template>
    <tr :class="{onyen: this.user.principal === 'everyone'}">
        <td>{{ fromText }}</td>
        <td class="access-display">
            {{ formattedPrincipal }}
            <a href="#" class="display-note-btn" :class="{hidden: nonPublicRole(user.principal)}">
                <i class="far fa-question-circle" :class="{hidden: nonPublicRole(user.principal)}"></i>
                <div class="arrow" :class="{'arrow-offset': alignTooltip(user.principal)}"></div>
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
                        <i class="far fa-times-circle" :class="{hidden: !hasAction.deleted}"></i>
                        <div class="circle" :class="{hidden: !hasAction.embargo > 0}">
                            <div>e</div>
                        </div>
                        <i class="far fa-check-circle"
                           v-if="mostRestrictive(user.principal) === type"></i>
                    </span>
        </td>
    </tr>
</template>

<script>
    import capitalize from 'lodash.capitalize';

    export default {
        name: 'patronDisplayRow',
        
        props: {
            displayRoles: Object,
            possibleRoles: Array,
            type: String,
            user: Object,
        },

        computed: {
            hasAction() {
                return this.displayRoles[this.type];
            },

            formattedPrincipal() {
                return capitalize(this.user.principal);
            },

            authenticatedUser() {
                return this.user.principal === 'authenticated'
            },

            fromText() {
                if (this.authenticatedUser) {
                    return '';
                } else if (this.type === 'assigned') {
                    return 'From object';
                } else {
                    return 'From Parent';
                }
            }
        },
        
        methods: {
            nonPublicRole(text) {
                return text !== 'everyone' && text !== 'patron';
            },

            currentUserRoles(user = 'Staff') {
                let inherited = this.displayRoles.inherited.roles.find((u) => u.principal === user);
                let assigned = this.displayRoles.assigned.roles.find((u) => u.principal === user);

                return { inherited: inherited , assigned: assigned };
            },

            displayRole(role) {
                let selected_role = this.possibleRoles.find((r) => r.role === role);
                return selected_role.text;
            },

            /**
             * Compares inherited and current object permissions for a user to determine which is more restrictive
             * @param current_users
             * @returns {string}
             */
            hasMultipleRoles(current_users) {
                let inherited_role = this.possibleRoles.findIndex((r) => r.role === current_users.inherited.role);
                let assigned_role = this.possibleRoles.findIndex((r) => r.role === current_users.assigned.role);

                if (assigned_role !== -1 && assigned_role < inherited_role) {
                    return 'assigned';
                } else {
                    return 'inherited';
                }
            },

            /**
             * Determines which permission for a given user is the 'effective' one
             * @param user
             * @returns {string|*|string}
             */
            hasRolesPriority(user) {
                let current_users = this.currentUserRoles(user);

                if (current_users.inherited === undefined && current_users.assigned === undefined) {
                    return 'none';
                } else if (current_users.inherited !== undefined && current_users.assigned === undefined) {
                    return 'inherited';
                } else if (current_users.inherited === undefined && current_users.assigned !== undefined) {
                    return 'assigned';
                } else {
                    return this.hasMultipleRoles(current_users);
                }
            },

            /**
             * Determines most restrictive permissions for icon display
             * @param user
             * @returns {*|string}
             */
            mostRestrictive(user) {
                // Check for staff roles. They supersede all other roles
                let has_staff_only = this._hasStaffOnly();

                if (has_staff_only !== undefined) {
                    return has_staff_only;
                }

                // Check for other users/roles
                return this.hasRolesPriority(user);
            },

            /**
             * Determines if staff roles are present and if so determines 'effective' permission
             * @returns {string|undefined}
             * @private
             */
            _hasStaffOnly() {
                let current_users = this.currentUserRoles();

                if (current_users.inherited !== undefined) {
                    return 'inherited';
                } else if (current_users.assigned !== undefined) {
                    return 'assigned'
                } else {
                    return undefined;
                }
            },

            alignTooltip(text) {
                return /patron/.test(text.toLowerCase());
            }
        }
    }
</script>

<style scoped lang="scss">
    #modal-permissions-editor {
        .border {
            tr.onyen {
                td {
                    border-bottom: none;
                }
            }

            td {
                height: auto;
                padding: 7px 0 7px 15px;
                position: relative;
            }
        }
    }

    td:last-child {
        min-width: 225px;
    }

    .access-display {
        max-width: 100px;

        span {
            width: auto
        }
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

    .circle {
        border: 1px solid #0F1C3F;
        border-radius: 50%;
        height: .8em;
        margin-left: 8px;
        width: .8em;
        text-align: center;

        div {
            font-size: 10px;
            margin-top: -4px;
        }
    }

    .permission-icons {
        display: inline-flex;
        float: right;
        margin-right: 20px;
        padding-top: 2px;
        text-align: right;
    }

    .browse-tip, .arrow {
        display: none;
    }

    .arrow {
        border-left: 5px solid transparent;
        border-right: 5px solid transparent;
        border-bottom: 10px solid darkslategray;
        height: 0;
        left: 88px;
        margin: inherit;
        top: 26px;
        width: 0;
    }



    a.display-note-btn:hover {
        .arrow, .browse-tip {
            display: block;
            position: absolute;
            z-index: 10009;
        }

        .arrow-offset {
            left: 68px;
        }

        .browse-tip {
            background-color: white;
            border: 1px solid darkslategray;
            border-radius: 5px;
            color: black;
            font-weight: normal;
            margin: inherit;
            padding: 10px;
            text-align: left;
            top: 35px;
            width: 240px;
        }
    }
</style>