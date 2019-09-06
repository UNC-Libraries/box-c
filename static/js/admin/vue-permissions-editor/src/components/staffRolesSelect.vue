<template>
    <div class="select-wrapper">
        <select v-model="selected_role" @change="selectedValue" :class="{'marked-for-deletion': deletedUser}">
            <option v-for="role in containerRoles(containerType)" :value="role.value">{{ role.text }}</option>
        </select>
    </div>
</template>

<script>
    import staffRoleList from "../mixins/staffRoleList";

    export default {
        name: 'staffRolesSelect',

        mixins: [staffRoleList],

        props: {
            areDeleted: Array,
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

        computed: {
            deletedUser() {
                return this.areDeleted.find((u) => u.principal === this.user.principal);
            }
        },

        methods: {
           selectedValue() {
               this.$emit('staff-role-update', { principal: this.user.principal, role: this.selected_role });
           }
        }
    }
</script>

<style scoped lang="scss">
</style>