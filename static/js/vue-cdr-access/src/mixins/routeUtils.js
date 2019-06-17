import isEmpty from 'lodash.isempty';

export default {
    methods: {
        /**
         * Put URL parameters into an object
         * Set default params, if none are present, that can be updated
         * @param params_to_update
         * @returns {({} & Dictionary<string | (string | null)[]>) | {start: number, page: number, sort: string, rows: number}}
         */
        urlParams(params_to_update = {}) {
            let defaults = {
                    page: 1,
                    rows: 20,
                    start: 0,
                    sort: 'title,normal'
                };
            let route_params = Object.assign(defaults, this.$route.query);
            let page_params;

            if (!isEmpty(params_to_update)) {
                page_params = Object.assign(route_params, params_to_update);
            }

            return page_params;
        },

        /**
         * Expects an object of parameters. Generally used with urlParams() method above
         * @param params
         * @returns {string}
         */
        formatParamsString(params) {
            let param_keys = Object.keys(params);
            let updated_params = param_keys.map((param) => {
                if (param === undefined) {
                    return;
                }
                return `${encodeURIComponent(param)}=${encodeURIComponent(params[param])}`
            }).join('&');

            return `?${updated_params}`;
        },

        /**
         * Check to see if a parameter is in the $route.query object
         * @param param
         * @param params
         * @returns {boolean}
         */
        paramExists(param, params) {
            return `${param}` in params;
        },

        /**
         * Add format to component params
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