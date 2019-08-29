<template>
            <tr>
                <td class="border">
                    <input @focus="clearErrorMessage" type="text" placeholder="ONYEN/Group" v-model.trim="user_name">
                </td>
                <td class="border select-box">
                    <select @focus="clearErrorMessage" v-model="selected_role" class="select-css">
                        <option value="">--Role--</option>
                        <option v-for="role in roles" :value="role.value">{{ role.text }}</option>
                    </select>
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

        data() {
            return {
                selected_role: '',
                user_name: ''
            }
        },

        methods: {
            addUser() {
                if (this.user_name !== '' && this.selected_role !== '') {
                    this.$emit('add-user', { principal: this.user_name, role: this.selected_role, type: 'new' });
                    this.user_name = '';
                    this.selected_role = '';
                } else {
                    this.$emit('form-error', 'Please add a user and role before submitting')
                }
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