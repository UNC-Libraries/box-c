export default {
    data() {
        return {
            roles: [
                { text: 'can Access', value: 'canAccess' },
                { text: 'can Ingest', value: 'canIngest' },
                { text: 'can Describe', value: 'canDescribe' },
                { text: 'can Manage', value: 'canManage' },
                { text: 'unit owner', value: 'unitOwner' },
                { text: 'administrator', value: 'administrator' }
            ]
        }
    },

    methods: {
        containerRoles(container_type) {
            if (container_type === 'AdminUnit') {
                return this.roles;
            } else {
                return this.roles.slice(0, 4);
            }
        }
    }
}