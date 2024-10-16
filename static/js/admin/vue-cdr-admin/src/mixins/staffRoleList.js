export default {
    data() {
        return {
            display_descriptions: false,
            roles: [
                { text: 'can Access', value: 'canAccess' },
                { text: 'can Ingest', value: 'canIngest' },
                { text: 'can Describe', value: 'canDescribe' },
                { text: 'can Process', value: 'canProcess' },
                { text: 'can Manage', value: 'canManage' },
                { text: 'Unit Owner', value: 'unitOwner' },
            ]
        }
    },

    methods: {
        containerRoles(container_type) {
            if (container_type === 'AdminUnit') {
                return this.roles;
            }
            return [...this.roles.slice(0, 5)];
        },

        showDescriptions() {
            this.display_descriptions = !this.display_descriptions;
        }
    }
}