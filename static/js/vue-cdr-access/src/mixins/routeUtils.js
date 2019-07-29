import isEmpty from 'lodash.isempty';

export default {
    data() {
        return {
            rows_per_page: this.$route.query.rows || 20
        }
    },

    methods: {
        /**
         * Put URL parameters into an object
         * Set default params, if none are present, that can be updated
         * @param params_to_update
         * @returns {({} & Dictionary<string | (string | null)[]>) | {start: number, page: number, sort: string, rows: number}}
         */
        urlParams(params_to_update = {}) {
            let defaults = {
                    rows: this.rows_per_page,
                    start: 0,
                    sort: 'title,normal'
                };
            let route_params = Object.assign(defaults, this.$route.query);

            if (!isEmpty(params_to_update)) {
                route_params = Object.assign(route_params, params_to_update);
            }

            return route_params;
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
         * Check to see if a parameter is in the url query
         * @param param
         * @param params
         * @returns {boolean}
         */
        paramExists(param, params) {
            return `${param}` in params;
        },

        /**
         * Check if folders should be added to the types parameter
         * @param field
         * @returns {*|boolean}
         */
        containsFolderType(field) {
            return /Folder/.test(field);
        }
    }
}