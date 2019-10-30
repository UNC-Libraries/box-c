export default {
    methods: {
        /**
         * Public users can have several value names. Make sure both are checked
         * @param selected_principal
         * @param user
         * @returns {boolean}
         */
        isPublicEveryone(selected_principal, user) {
            let user_value = user.toLowerCase();

            if (user_value === 'everyone' || user_value === 'patron') {
                return selected_principal === 'everyone' || selected_principal === 'patron';
            }

            return selected_principal === user_value;
        }
    }
}