export default {
    methods: {
        /**
         * Public users can have several value names. Make all are checked
         * @param selected_principal
         * @param user
         * @returns {boolean}
         */
        isPublicEveryone(selected_principal, user) {
            let user_value = user.toLowerCase();
            let public_user_regex = /everyone|patron|public.user/;

            if (public_user_regex.test(user_value)) {
                return public_user_regex.test(selected_principal);
            }

            return selected_principal === user_value;
        }
    }
}