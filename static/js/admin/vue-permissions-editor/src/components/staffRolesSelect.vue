<template>
    <select v-model="selected_role" @change="selectedValue" class="select-css">
        <option value="">--Role--</option>
        <option v-for="role in containerRoles(containerType)" :value="role.value">{{ role.text }}</option>
    </select>
</template>

<script>
    import staffRoleList from "../mixins/staffRoleList";

    export default {
        name: 'staffRolesSelect',

        mixins: [staffRoleList],

        props: {
            containerType: String,
            user: Object
        },

        watch: {
            user(updated_user) {
                this.selected_role = updated_user.role;
            }
        },

        data() {
            return {
                selected_role: this.user.role
            }
        },

        methods: {
           selectedValue() {
               this.$emit('staff-role-update', { principal: this.user.principal, role: this.selected_role });
           }
        }
    }
</script>

<style scoped>
</style>