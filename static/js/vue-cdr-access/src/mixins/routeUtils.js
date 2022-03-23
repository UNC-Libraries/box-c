import isEmpty from 'lodash.isempty';

export default {
    data() {
        return {
            rows_per_page: this.$route.query.rows || 20,
            possible_facet_fields: ['collection', 'createdYear', 'format', 'language', 'subject', 'location']
        }
    },

    methods: {
        /**
         *  Put URL parameters into an object
         * Set default params, if none are present, that can be updated
         * @param params_to_update
         * @param is_search
         * @returns {any}
         */
        urlParams(params_to_update = {}, is_search = false) {
            let defaults;

            if (is_search) {
                defaults = {
                    'a.setStartRow': 0,
                    rows: this.rows_per_page,
                    sort: 'default,normal',
                    facetSelect: this.possible_facet_fields.join(',')
                };
            } else {
                defaults = {
                    rows: this.rows_per_page,
                    start: 0,
                    sort: 'default,normal',
                    browse_type: 'list-display',
                    works_only: false
                };
            }

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
         * Set work types to display
         * @param works_only
         * @returns {*|({start: number, sort: string, rows: (*|number), browse_type: string}&Dictionary<string|(string|null)[]>)}
         */
        updateWorkType(works_only) {
            let params = this.urlParams();

            if (!this.coerceWorksOnly(works_only)) {
                params.types = 'Work,Folder,Collection';
            } else {
                params.types = 'Work';
            }

            return params;
        },

        /**
         * Set value to boolean if it comes in as string from the url parameters
         * @param works_only
         * @returns {boolean}
         */
        coerceWorksOnly(works_only) {
            if (typeof works_only === 'string') {
                works_only = works_only === 'true';
            }

            return works_only;
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
         * Checks the current URLs query parameters against the provided list of parameter names,
         * returning true if at least one parameter is present and not empty.
         * @param param_names
         * @returns {boolean}
         */
        anyParamsPopulated(param_names) {
            const params = Object.assign({}, this.$route.query);
            for (const name of param_names) {
                if (name in params && params[name]) {
                    return true;
                }
            }
            return false;
        },

        /**
         * Vue router doesn't seem to like dynamically adding params to current route
         * So catch any duplicate navigation errors and ignore
         * @param error
         * @returns {boolean|boolean}
         */
        nonDuplicateNavigationError(error) {
            return (error.name !== 'NavigationDuplicated' && !/^avoided\s+redundant\s+navigation/i.test(error.message));
        },

        /**
         * Returns a list of query parameters from the current URLs minus the specified set of parameters
         * @param param_names names of query parameters to remove
         * @returns {LocationQuery} list of query parameters with specified parameters removed
         */
        removeQueryParameters(param_names) {
            const params = Object.assign({}, this.$route.query);
            for (const remove_param of param_names) {
                delete params[remove_param];
            }
            return params;
        },

        /**
         * Push updated url to history, using the provided query parambers
         * @param params query parameters to push to url
         * @param route_name optional name for pushed route
         */
        routeWithParams(params, route_name = undefined) {
            this.$router.push({ query: params, name: route_name }).catch((e) => {
                if (this.nonDuplicateNavigationError(e)) {
                    throw e;
                }
            });
        }
    }
}