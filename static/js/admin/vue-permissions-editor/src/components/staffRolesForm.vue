<template>
            <tr>
                <td class="border size">
                    <input @focus="clearErrorMessage"
                           @focusout="userNameSet"
                           type="text"
                           placeholder="ONYEN/Group"
                           v-model.trim="user_name">
                </td>
                <td class="border select-box">
                    <div class="select-wrapper">
                        <select v-model="selected_role" @focus="clearErrorMessage">
                            <option v-for="role in containerRoles(containerType)" :value="role.value">{{ role.text }}</option>
                        </select>
                    </div>
                </td>
                <td class="btn">
                    <button class="btn-add" @click.prevent="addUser">Add</button>
                </td>
            </tr>
</template>

<script>
    import staffRoleList from "../mixins/staffRoleList";

    export default {
        name: 'staffRolesForm',

        mixins: [staffRoleList],

        props: {
            containerType: String,
            isCanceling: Boolean,
            isSubmitting: Boolean
        },

        watch: {
            isCanceling(canceling) {
                this.unAddedUser(canceling);
            },

            isSubmitting(submitting) {
                this.unAddedUser(submitting);
            }
        },

        data() {
            return {
                selected_role: 'canAccess',
                user_name: ''
            }
        },

        methods: {
            unAddedUser(should_add) {
                if (should_add && this.user_name !== '') {
                    this.emitEvent();
                } else {
                    this.$emit('username-set', false);
                }
            },

            userNameSet() {
                if (this.user_name !== '') {
                    this.$emit('username-set', true);
                }
            },

            addUser() {
                if (this.user_name !== '') {
                    this.emitEvent();
                } else {
                    this.$emit('form-error', 'Please add a user before submitting')
                }
            },

            emitEvent() {
                this.$emit('add-user', { principal: this.user_name, role: this.selected_role, type: 'new' });
                this.user_name = '';
                this.selected_role = 'canAccess';
                this.$emit('username-set', false);
            },

            clearErrorMessage() {
                this.$emit('form-error', '');
            }
        }
    }
</script>

<style scoped lang="scss">
    input, select {
        padding: 3px;
        width: 100%;
    }

    input {
        box-sizing: border-box;
        display: table-cell;
    }

    #modal-permissions-editor .border {
        padding: 0;
    }

    select {
        padding: 6px 3px;
    }

    .btn-add {
        background-color: limegreen;
    }

    .error {
        color: red;
        height: 1px;
        margin-top: 15px;
    }

    button {
        border: none;
        color: white;
        padding: 5px 10px;
    }
</style>