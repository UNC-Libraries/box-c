<template>
    <tr :class="{onyen: this.user.principal === 'everyone'}">
        <td>{{ fromText }}</td>
        <td class="access-display">
            {{ formattedPrincipal(user.principal_display) }}
            <div class="display-note-btn" :class="{hidden: nonPublicRole(user.principal_display)}">
                <i class="far fa-question-circle" :class="{hidden: nonPublicRole(user.principal_display)}"></i>
                <div class="arrow" :class="{'arrow-offset': alignTooltip(user.principal_display)}"></div>
                <div class="browse-tip">
                    <p><strong>Public Users:</strong> Applies to unauthenticated users.</p>
                    <p><strong>Patrons:</strong> Applies to all patron users, whether authenticated or unauthenticated.</p>
                </div>
            </div>

            <span class="permission-icons">
                <i class="far fa-check-circle" title="effective permission" v-if="mostRestrictive(user.principal) === type"></i>
            </span>
        </td>
        <td>
            {{ displayRole(user.role) }}
            <span class="permission-icons">
                <i class="far fa-times-circle" title="object deleted" :class="{hidden: !hasAction.deleted}"></i>
                <div class="circle" title="object embargoed" :class="{hidden: !hasAction.embargo > 0}">
                    <div>e</div>
                </div>
                <i class="far fa-check-circle" title="effective permission" v-if="mostRestrictive(user.principal) === type"></i>
            </span>
        </td>
    </tr>
</template>

<script>
    import patronHelpers from '../mixins/patronHelpers';

    export default {
        name: 'patronDisplayRow',

        mixins: [patronHelpers],

        props: {
            containerType: String,
            displayRoles: Object,
            possibleRoles: Array,
            type: String,
            user: Object,
        },

        computed: {
            hasAction() {
                return this.displayRoles[this.type];
            },

            authenticatedUser() {
                return this.user.principal_display === 'authenticated'
            },

            fromText() {
                if (this.authenticatedUser) {
                    return '';
                } else if (this.type === 'assigned') {
                    return 'From Object';
                } else {
                    return 'From Parent';
                }
            }
        },

        methods: {
            formattedPrincipal(user) {
                if (user === 'everyone') {
                    user = 'Public Users';
                }

                return user;
            },

            nonPublicRole(text) {
                return text !== 'everyone' && text !== 'patron';
            },

            currentUserRoles(user = 'staff') {
                // Since we only care about the returned role, checking for 'everyone' if user is 'patron'
                // is fine since 'everyone' and 'authenticated' will have the same role
                if (user === 'patron') {
                    user = 'everyone';
                }

                let inherited = this.displayRoles.inherited.roles.find((u) => u.principal === user);
                let assigned = this.displayRoles.assigned.roles.find((u) => u.principal === user);
                let inherited_staff = this.displayRoles.inherited.roles.find((u) => u.principal === 'staff');
                let assigned_staff = this.displayRoles.assigned.roles.find((u) => u.principal === 'staff');

                return {
                    inherited: inherited,
                    assigned: assigned,
                    inherited_staff: inherited_staff,
                    assigned_staff: assigned_staff
                };
            },

            displayRole(role) {
                if (this.user.principal_display === 'staff') {
                    return this.possibleRoles[0].text;
                }
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

                if (this.containerType === 'Collection') {
                    return 'assigned';
                } else if (current_users.inherited_staff !== undefined) {
                    return 'inherited';
                } else if (current_users.assigned_staff !== undefined) {
                    return 'assigned';
                } else if (current_users.assigned === undefined) {
                    return 'inherited';
                } else if (current_users.inherited === undefined) {
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
                return this.hasRolesPriority(user);
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
        text-transform: capitalize;

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
        left: 105px;
        margin: inherit;
        top: 26px;
        width: 0;
    }

    div.display-note-btn {
        display: inline-flex;
        width: 15px;
    }

    div.display-note-btn:hover {
        cursor: grab;

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
            left: 15px;
            margin: inherit;
            padding: 10px;
            text-align: left;
            top: 35px;
            width: 240px;

            p {
                font-size: 14px;
            }
        }
    }
</style>