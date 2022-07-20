import isEmpty from 'lodash.isempty';

const UUID_REGEX = /([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/i;
export default {
    data() {
        return {
            rows_per_page: this.$route.query.rows || 20,
            min_created_year: undefined
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
            let defaults = {
                start: 0,
                rows: this.rows_per_page,
                sort: 'default,normal',
                facetSelect: this.possibleFacetFields.join(',')
            };

            if (!is_search) {
                defaults.works_only = false;
                defaults.browse_type = 'list-display';
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
                params.types = 'Work,Folder,Collection,File';
            } else {
                params.types = 'Work,File';
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
         * Push updated url to history, using the provided query parameters
         * @param params query parameters to push to url
         * @param route_name optional name for pushed route
         * @param route_params path parameters for formatting into route
         */
        routeWithParams(params, route_name = undefined, route_params = undefined) {
            this.$router.push({ query: params, name: route_name, params: route_params }).catch((e) => {
                if (this.nonDuplicateNavigationError(e)) {
                    throw e;
                }
            });
        },

        /**
         * Resets start row, so searches end up on the first page of results
         * @param query_params
         * @returns {*}
         */
        resetStartRow(query_params) {
            if (query_params.start !== undefined) {
                query_params.start = 0;
            }
            return query_params;
        },

        /**
         * Vite really, really wants all images to be in the project being built and referenced via imports.
         * This doesn't make sense for some of our images. The project won't build unless an import or url is given.
         * So just return the image url for images that are external to the Vue project.
         * @param image
         */
        nonVueStaticImageUrl(image) {
            return `https://${window.location.host}/static/images/${image}`;
        }
    },

    computed: {
        possibleFacetFields() {
            return this.$store.state.possibleFacetFields;
        },
        minimumCreatedYear() {
            if (this.min_created_year !== undefined) {
                return parseInt(this.min_created_year)
            }
            return undefined;
        },
        allPossibleSearchParameters() {
            return this.possibleFacetFields.concat(['anywhere']);
        },
        routeParams() {
            let params = {};
            let match = UUID_REGEX.exec(this.$route.path);
            if (match != null) {
                params.uuid = match[1];
            }
            return params;
        },
        routeHasPathId() {
            return UUID_REGEX.test(this.$route.path);
        }
    }
}