<template>
    <div>
        <form>
            <input @focus="clearErrorMessage" type="text" placeholder="ONYEN/Group" v-model.trim="user_name">
            <select @focus="clearErrorMessage" v-model="selected_role">
                <option value="">--Role--</option>
                <option v-for="role in roles" :value="role.value">{{ role.text }}</option>
            </select>
            <button class="btn-add" @click.prevent="addUser">Add</button>
        </form>
        <p class="error">{{ error_message }}</p>
    </div>
</template>

<script>
    import staffRoleList from "../mixins/staffRoleList";

    export default {
        name: 'staffRolesForm',

        mixins: [staffRoleList],

        data() {
            return {
                error_message: '',
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
                    this.error_message = 'Please add a user and role before submitting'
                }
            },

            clearErrorMessage() {
                this.error_message = '';
            }
        }
    }
</script>

<style scoped lang="scss">
    input, select {
        margin-right: 10px;
        padding: 3px;
    }

    select {
        padding: 6px 3px;
    }

    .btn-add {
        background-color: limegreen;
    }

    .error {
        color: red;
        margin-top: 10px;
    }

    button {
        border: none;
        color: white;
        padding: 5px 10px;
    }
</style>