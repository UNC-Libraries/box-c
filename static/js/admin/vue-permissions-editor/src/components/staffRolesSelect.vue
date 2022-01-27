<template>
    <div class="select-wrapper">
        <select v-model="selected_role" @change="selectedValue" :disabled="deletedUser" :class="{'marked-for-deletion': deletedUser}">
            <option v-for="role in containerRoles(containerType)" :value="role.value">{{ role.text }}</option>
        </select>
    </div>
</template>

<script>
    import staffRoleList from "../mixins/staffRoleList";

    export default {
        name: 'staffRolesSelect',

        mixins: [staffRoleList],

        emits: ['staff-role-update'],

        props: {
            areDeleted: {
                type: Array,
                default: () => {
                    return [];
                }

            },
            containerType: String,
            user: Object
        },

        watch: {
            user: {
                handler(updated_user) {
                    this.selected_role = updated_user.role;
                },
                deep: true
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