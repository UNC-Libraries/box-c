const PRINCIPAL_DISPLAY_NAMES = {
    everyone: 'Public users',
    authenticated: 'Authenticated users',
    staff: 'No Patron Access',
    patron: 'Patron'
};

export default {
    methods: {
        principalDisplayName(principal, allowed_other_principals) {
            // For reserved principals
            let displayName = PRINCIPAL_DISPLAY_NAMES[principal];
            if (displayName !== undefined) {
                return displayName;
            }
            // For custom defined principals
            let mapping = allowed_other_principals.find(e => e.id === principal);
            if (mapping === null) {
                // no name available, fall back to principal
                return principal;
            }
            return mapping.name;
        },

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