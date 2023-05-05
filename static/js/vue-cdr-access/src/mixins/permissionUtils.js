import isEmpty from 'lodash.isempty';

export default {
    methods: {
        permissionData(recordData) {
            if (recordData.briefObject !== undefined) {
                return recordData.briefObject;
            }
            return recordData;
        },

        hasGroups(recordData) {
            recordData = this.permissionData(recordData);
            const group_roles = recordData.groupRoleMap;
            return !(group_roles === undefined || isEmpty(group_roles));
        },

        hasGroupRole(recordData, role, user_type= 'everyone') {
            recordData = this.permissionData(recordData);
            if (!this.hasGroups(recordData)) {
                return false;
            }

            const group_roles = recordData.groupRoleMap;
            return Object.keys(group_roles).includes(user_type) &&
                group_roles[user_type].includes(role);
        },

        hasPermission(recordData, permission) {
            recordData = this.permissionData(recordData);
            if (recordData.permissions === undefined) {
                return false;
            }
            return recordData.permissions.includes(permission);
        },

        markedForDeletion(record) {
           if (record.status === undefined) return false;
           return /marked.*?deletion/i.test(this.restrictions(record));
        },

        isRestricted(record) {
           if (record.type === 'AdminUnit') return false;
           if (record.status === undefined) return true;
           return !record.status.includes('Public Access');
        },

        restrictions(record) {
            return record.status.join(',').toLowerCase();
        }
    }
}