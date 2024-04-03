define('SearchTags', ['jquery'], function($) {
    const SEARCH_PARAMETERS = [
        { key: 'anywhere', display_value: 'anywhere' },
        { key: 'titleIndex', display_value: 'title' },
        { key: 'contributorIndex', display_value: 'contributor' },
        { key: 'subjectIndex', display_value: 'subject' }
    ];

    class SearchTags {
        constructor() {
            this.search_tags = [];
        }

        setupEvents() {
            $('#clear-results').on('click', (e) => this.#clearAll(e));
            $('.clear-all-facets').on('click', (e) => this.#clearFilters(e));
            $('#search-tags-list').on('click', (e) => this.#clearSearchParameter(e));
        }

        /**
         * Updates tag display in the UI
         * @param data
         */
        updateTags(data) {
            this.#clearTags();

            const search_tags = this.#buildTags(data);
            // Only add container if it's from the filter list
            if (data.container !== undefined && data.container.type !== 'ContentRoot' && /search/.test(location.pathname)) {
                this.search_tags.push({ type: data.container.type, text: data.container.title, uuid: data.container.id });
            }

            if (search_tags.length > 0) {
                search_tags.forEach(d => this.search_tags.push(d));
            }

            this.#displayButtons();
            this.#writeTags();
        }

        /**
         * Builds the list of tags to be displayed
         * @param data
         * @returns {unknown[]|*[]}
         */
        #buildTags(data) {
            const search = data.searchQueryUrl;

            if (search !== undefined && search !== '') {
                let tags = search.split('&');
                return tags.map((d) => {
                    const tag = d.split('=');
                    if (tag.length === 2) {
                        const search_tag = SEARCH_PARAMETERS.find((d) => d.key === tag[0]);
                        const tag_text = (search_tag !== undefined) ? search_tag.display_value : tag[0];
                        return { type: tag_text, text: tag[1], uuid: undefined }
                    }
                });
            }

            return [];
        }

        /**
         * Writes tags to the UI
         */
        #writeTags() {
            let tags = '';
            this.search_tags.forEach((d) => {
                tags += `<li id="${d.type}">${d.type} <i class="fas fa-greater-than"></i> ${d.text} <i class="fas fa-times"></i></li>`;
            });

            $('#search-tags-list').html(tags);
        }

        #clearTags() {
            this.search_tags = [];
        }

        /**
         * Clear a single search parameter
         * Container ID is not included the search parameters, so update the url accordingly
         * @param e
         */
        #clearSearchParameter(e) {
            const facet_index = this.search_tags.findIndex((d) => d.type === e.target.id);

            // Save container id, if present, for reuse in updated url if no tags are present
            let container = '';
            if (facet_index !== -1 && this.search_tags[facet_index].uuid !== undefined) {
                container = `/${this.search_tags[facet_index].uuid}`;
            }

            // Remove selected filter
            if (facet_index !== -1) {
                this.search_tags.splice(facet_index, 1);
            }

            if (this.search_tags.length === 0) {
                location.assign(`/admin/list${container}`);
                return;
            }

            // Update url if filters are still present
            const query_string = this.#setQueryString();
            const collection_search = this.search_tags.find((d) => d.uuid !== undefined);

            if (collection_search === undefined) {
                location.assign(`/admin/search/collections?${query_string}`);
            } else if (query_string !== '') {
                location.assign(`/admin/search/${collection_search.uuid}?${query_string}`);
            } else {
                location.assign(`/admin/search/${collection_search.uuid}`);
            }
        }

        /**
         * Create search string
         * @returns {string}
         */
        #setQueryString() {
            let query_string = '';
            this.search_tags.forEach((d, index) => {
                if (d.uuid === undefined) {
                    if (index > 0) {
                        query_string += '&';
                    }
                    query_string += `${d.type}=${d.text}`;
                }
            });
            return query_string;
        }

        #searchQueryKeys() {
            return SEARCH_PARAMETERS.map(d => d.display_value);
        }

        #displayButtons() {
            // Clear all button
            const clear_all_btn = $('#clear-results');
            if (this.search_tags.length > 0) {
                clear_all_btn.removeClass('disabled');
            } else {
                clear_all_btn.addClass('disabled');
            }

            // Clear filters button
            const search_query_keys = this.#searchQueryKeys();
            const filter_tags = this.search_tags.filter((d) => {
                return d.uuid === undefined && !search_query_keys.includes(d.type);
            });

            const filter_btn = $('#search-tags .facets-button');
            if (filter_tags.length > 0) {
                filter_btn.removeClass('hide');
            } else {
                filter_btn.addClass('hide');
            }
        }

        /**
         * Clear filters, leaving search query intact and container id
         * @param e
         */
        #clearFilters(e) {
            e.preventDefault();
            const search_query_keys = this.#searchQueryKeys();
            this.search_tags = this.search_tags.filter((d) => {
                return d.uuid !== undefined || search_query_keys.includes(d.type);
            });
            location.assign(this.#resetUrl(this.#setQueryString()));
        }

        /**
         * Clear filters and search query
         * @param e
         */
        #clearAll(e) {
            e.preventDefault();
            const uuid = this.#resetUrl();
            this.#clearTags();
            location.assign(uuid);
        }

        /**
         * Reset url when starting over or clearing all filters
         * @param query_string
         * @returns {string}
         */
        #resetUrl(query_string = '') {
            const container = this.search_tags.find((d) => d.uuid !== undefined);
            const uuid = (container === undefined) ? '' : `/${container.uuid}`;
            if (query_string === '') {
                return `/admin/list${uuid}`;
            }
            return `/admin/search${uuid}?${query_string}`;
        }
    }

    return SearchTags;
});