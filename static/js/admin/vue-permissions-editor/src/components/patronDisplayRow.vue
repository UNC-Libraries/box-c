<template>
    <tr>
        <td class="access-display">
            {{ principalDisplayName(user.principal, allowedPrincipals) }}
            <div class="display-note-btn" :class="{hidden: nonPublicRole(user.principal)}">
                <i class="far fa-question-circle" :class="{hidden: nonPublicRole(user.principal)}"></i>
                <div class="arrow" :class="{'arrow-offset': alignTooltip(user.principal)}"></div>
                <div class="browse-tip">
                    <p><strong>Public Users:</strong> Applies to unauthenticated users.</p>
                    <p><strong>Patrons:</strong> Applies to all patron users, whether authenticated or unauthenticated.</p>
                </div>
            </div>
        </td>
        <td>
            {{ formattedRole(user.role) }} {{ fromText(user.type) }}
            <span class="permission-icons">
                <i class="far fa-times-circle" title="object deleted" :class="{hidden: !user.deleted}"></i>
                <div class="circle" title="object embargoed" :class="{hidden: !user.embargo}">
                    <div>e</div>
                </div>
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
            user: Object,
            userType: String,
            allowedPrincipals: Array
        },

        methods: {
            fromText(type) {
                if (this.userType === 'parent' || type === 'assigned') {
                    return '';
                }

                return '(Overridden by parent)';
            },

            formattedRole(role) {
                if (this.user.principal === 'staff') {
                    return 'N/A';
                }

                let roleList = this.possibleRoleList(this.containerType);
                let roleText = roleList.find((d) => d.role === role)
                return roleText.text;
            },

            nonPublicRole(text) {
                return text !== 'everyone' && text !== 'patron';
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