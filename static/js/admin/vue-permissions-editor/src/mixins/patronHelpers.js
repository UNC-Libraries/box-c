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
            let mapping = allowed_other_principals.find(e => e.principal === principal);
            if (mapping === null) {
                // no name available, fall back to principal
                return principal;
            }
            return mapping.name;
        },

        possibleRoleList(container) {
            let displayType = container === null ? "object" : container;
            return [
                { text: '', role: '' },
                { text: 'No Access', role: 'none' },
                { text: 'Metadata Only', role: 'canViewMetadata' },
                { text: 'Access Copies', role: 'canViewAccessCopies' },
                { text: 'Access Copies + Low Res Downloads', role: 'canViewReducedQuality' },
                { text: `All of this ${displayType}`, role: 'canViewOriginals' }
            ]
        }
    }
}