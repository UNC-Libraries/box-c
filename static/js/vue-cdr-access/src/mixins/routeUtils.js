import isEmpty from 'lodash.isempty';

export default {
    methods: {
        /**
         * Put URL parameters into an object
         * Set default params, if none are present that can be updated
         * @returns {{[p: string]: string | (string | null)[]} | {start: number, page: number, sort: string, rows: number}}
         */
        urlParams(params_to_update = {}) {
            let  page_params = {
                page: 1,
                rows: 20,
                start: 0,
                sort: 'title,normal'
            };

            if (!isEmpty(params_to_update)) {
                Object.keys(params_to_update).forEach(key => {
                    page_params[key] = params_to_update[key];
                });

                if ('format' in Object.keys(params_to_update) && params_to_update.format === 'delete') {
                    delete page_params.format;
                }
            } else {
                page_params = this.$route.query;
            }

            return page_params;
        },

        /**
         * Expects an object of parameters. Generally used with urlParams() method above
         * @param params
         * @returns {string}
         */
        formatParamsString(params) {
            let updated_params = '';
            let param_keys = Object.keys(params);

            param_keys.forEach((param_name, i) => {
                if (param_name === undefined) {
                    return;
                }

                if (i === 0) {
                    updated_params += `?${param_name}=${encodeURIComponent(params[param_name])}`;
                } else {
                    updated_params += `&${param_name}=${encodeURIComponent(params[param_name])}`
                }
            });

            return updated_params;
        },

        /**
         * Check to see if a parameter is in the url query
         * @param param
         * @param params
         * @returns {boolean}
         */
        paramExists(param, params) {
            return `${param}` in params;
        }
    }
}