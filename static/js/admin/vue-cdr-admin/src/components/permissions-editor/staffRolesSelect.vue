<template>
    <div class="select-wrapper">
        <select v-model="selected_role" @change="selectedValue" :disabled="deletedUser" :class="{'marked-for-deletion': deletedUser}">
            <option v-for="role in containerRoles(containerType)" :value="role.value">{{ role.text }}</option>
        </select>
    </div>
</template>

<script>
    import { mapActions } from 'pinia';
    import { usePermissionsStore } from '../../stores/permissions';
    import staffRoleList from "../../mixins/staffRoleList";

    export default {
        name: 'staffRolesSelect',

        mixins: [staffRoleList],

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
            ...mapActions(usePermissionsStore, ['setStaffRole']),

            selectedValue() {
               this.setStaffRole({ principal: this.user.principal, role: this.selected_role });
           }
        }
    }
</script>
