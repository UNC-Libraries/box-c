export default {
    methods: {
        possibleRoleList(container) {
            return [
                { text: '', role: '' },
                { text: 'No Access', role: 'none' },
                { text: 'Metadata Only', role: 'canViewMetadata' },
                { text: 'Access Copies', role: 'canViewAccessCopies' },
                { text: `All of this ${container}`, role: 'canViewOriginals' }
            ]
        }
    }
}