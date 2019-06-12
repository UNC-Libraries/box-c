import isEmpty from 'lodash.isempty';

export default {
    methods: {
        /**
         * Put URL parameters into an object
         * Set default params, if none are present that can be updated
         * @param params_to_update
         * @returns {({} & Dictionary<string | (string | null)[]>) | {start: number, page: number, sort: string, rows: number}}
         */
        urlParams(params_to_update = {}) {
            let route_params = Object.assign({}, this.$route.query);
            let page_params;

            if (isEmpty(this.$route.query)) {
                page_params = {
                    page: 1,
                    rows: 20,
                    start: 0,
                    sort: 'title,normal'
                };
            } else if (!isEmpty(params_to_update)) {
                page_params = Object.assign(route_params, params_to_update);

                if (!this.paramExists('format', params_to_update)) {
                    delete page_params.format;
                }
            } else {
                page_params = route_params;
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
        },

        /**
         * Add format to component params, other than BrowseImage
         * @param update_params
         * @returns {*}
         */
        addFormat(update_params) {
            if (this.paramExists('format', this.$route.query)) {
                update_params.format = this.$route.query.format;
            }

            return update_params;
        }
    }
}