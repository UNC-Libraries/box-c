export default {
    data() {
        return {
            show_404: false,
            show_503: false,
        }
    },

    methods: {
        /**
         * An empty response with a 200 return code signals that the uuid used in a query is valid, but not in the DCR.
         * So throw an error that can be caught to display a 404 message.
         * @param response
         */
        emptyJsonResponseCheck(response) {
            if (response.data === '') {
               throw new Error('ResponseEmpty');
            }
        },

        setErrorResponse(error) {
            if (/ResponseEmpty/.test(error) || this.errorStatus(error, 4)) {
                this.show_404 = true;
            } else if (this.errorStatus(error, 5)) {
                this.show_503 = true;
            }
        },

        errorStatus(error, error_code) {
            const status_regex =  new RegExp(`^${error_code}`)
            return error.response !== undefined && status_regex.test(error.response.status);
        }
    }
}