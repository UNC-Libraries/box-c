const utils = {
    /**
     * Put URL paramaters into an object
     * @returns {{start: number, page: number, sort: string, rows: number}}
     */
    urlParams() {
        // Set default params that can be updated
        let page_params = {
            page: 1,
            rows: 20,
            start: 0,
            sort: 'title,normal'
        };

        let params = window.location.search;
        let params_list = params.split('&');

        if (params_list[0] !== '') {
            params_list.forEach((p) => {
                let param = p.split('=');
                let key = param[0].replace('?', '');

                page_params[key] = decodeURIComponent(param[1])
            });
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
    }
};

export { utils }