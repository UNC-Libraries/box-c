<template>
    <div id="facetList" class="contentarea">
        <h2 class="facet-header">Filter results by...</h2>
        <div class="selected_facets" v-if="facet_info.length > 0">
            <ul>
                <li v-for="value in parsedFacetInfo">
                    <label :aria-label="value.displayValue" @click="updateAll(value, true)">
                        <input type="checkbox" v-model="facet_info" :value="JSON.stringify(value)">
                        {{ value.displayValue }}
                    </label>
                </li>
            </ul>
        </div>
        <div class="facet-display" v-if="facet.values.length" v-for="facet in this.facetList">
            <div v-if="showFacetDisplay(facet)">
                <h3>{{ facetName(facet.name) }}</h3>
                <ul>
                    <li v-for="value in facet.values">
                        <label :aria-label="value.displayValue" @click="updateAll(value)">
                            <input type="checkbox" v-model="selected_facets" :value="facetValue(value)">
                            {{ value.displayValue }} ({{ value.count }})
                        </label>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';

    const UUID_REGEX = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i;
    const POSSIBLE_FACETS = ['format', 'language', 'subject'];

    export default {
        name: 'facets',

        props: {
            facetList: Array
        },

        mixins: [routeUtils],

        data() {
            return {
                facet_info: [],
                selected_facets: []
            }
        },

        watch: {
            selected_facets() {
                this.selectedFacets();
            }
        },

        computed: {
            routeHasCollectionId() {
                return UUID_REGEX.test(this.$route.path);
            },

            parsedFacetInfo() {
                return this.facet_info.map((facet) => JSON.parse(facet));
            }
        },

        methods: {
            updateAll(facet, remove = false) {
                if (remove) {
                    this.facetInfoRemove(facet);
                } else {
                    this.facet_info.push(JSON.stringify(facet));
                }
            },

            /**
             * Used to determine if a collection has been selected. If so, remove collections from facets to select from
             * @param facet
             * @returns {boolean|boolean}
             */
            showFacetDisplay(facet) {
                return facet.name !== 'PARENT_COLLECTION' || (facet.name === 'PARENT_COLLECTION' && !this.routeHasCollectionId);
            },

            /**
             * Push new url after a facet is selected/deselected
             */
            selectedFacets() {
                let updated_facet_params = this.updateUrl();
                let base_search = {
                    query: this.urlParams({}, true)
                };

                if (updated_facet_params.collection !== '') {
                    base_search.path = updated_facet_params.path;
                    base_search.query.collection_name = updated_facet_params.collection_name;
                } else {
                    base_search.name = 'searchRecords';
                    delete base_search.query.collection_name;
                }

                // Unset current facets
                POSSIBLE_FACETS.forEach((facet) => delete base_search.query[facet]);
                // Add/Update with new facets
                base_search.query = Object.assign(base_search.query, updated_facet_params.queryFacets);

                this.$router.push(base_search);
            },

            /**
             * Determine parameters to build the new url after a facet is selected/deselected
             * @returns {{queryFacets: {}, path: string, collection: string}}
             */
            updateUrl() {
                let updated_facets = this.selected_facets;
                let path = '/search/';
                let collection_id = '';
                let collection_name = '';
                let collection = this.selected_facets.findIndex((facet) => {
                    return UUID_REGEX.test(facet);
                });

                if (collection !== -1) {
                    let current_collection = this.parsedFacetInfo.find((f) => f.fieldName === 'ANCESTOR_PATH');
                    if (current_collection !== undefined) {
                        collection_name = current_collection.displayValue;
                    }

                    collection_id = this.selected_facets[collection];
                    path += `${collection_id}`;

                    // Remove collection from facets array without removing it from this.selected_facets
                    updated_facets = [
                        ...this.selected_facets.slice(0, collection),
                        ...this.selected_facets.slice(collection + 1)
                    ];
                }

                return {
                    collection: collection_id,
                    collection_name: collection_name,
                    path: path,
                    queryFacets: this._formatFacets(updated_facets)
                };
            },

           /**
             * Remove full facet info for deselected facets
             * @param facet
             */
            facetInfoRemove(facet) {
                this._updateSelectedFacets(facet);

                if (!UUID_REGEX.test(facet.value)) {
                    this._removeSubFacets(facet);
                }
            },

            /**
             * Create regular expression to find facet value
             * @param facet
             * @returns {RegExp}
             * @private
             */
            _buildRegex(facet) {
                let search_value = facet.limitToValue;
                return new RegExp(search_value);
            },

            /**
             * Add/remove item from facet info list
             * @param facet
             * @private
             */
            _updateFacetInfo(facet) {
                this.facet_info = this.facet_info.filter((f) => {
                    return f.fieldName !== facet.fieldName;
                });
            },

            /**
             * Add/remove item from selected facet list
             * @param facet
             * @private
             */
            _updateSelectedFacets(facet) {
                let regex = this._buildRegex(facet);
                this.selected_facets = this.selected_facets.filter((sf) => {
                    return !regex.test(sf);
                });
            },

            /**
             * Remove children of base facets
             * @param facet
             * @private
             */
            _removeSubFacets(facet) {
                this._updateSelectedFacets(facet);
                this._updateFacetInfo(facet);
            },

            /**
             * Format facets for url
             * Avoid double encoding by decoding first
             * @param updated_facets
             * @returns {{}}
             * @private
             */
            _formatFacets(updated_facets) {
                let formatted_facets = {};
                updated_facets.forEach((facet) => {
                    let facet_pieces = facet.split('=');
                    formatted_facets[facet_pieces[0]] = encodeURIComponent(decodeURIComponent(facet_pieces[1]));
                });

                return formatted_facets;
            },

            /**
             * Determine facet header text
             * @param value
             * @returns {string|*}
             */
            facetName(value) {
                if (value === 'PARENT_COLLECTION') {
                    return 'Collection'
                } else if (value === 'CONTENT_TYPE') {
                    return 'Format';
                } else if (value === 'LANGUAGE') {
                    return 'Language';
                } else if (value === 'SUBJECT') {
                    return 'Subject';
                } else {
                    return value;
                }
            },

            /**
             * Create base facet value for a selected facet
             * @param value
             * @returns {string}
             */
            facetValue(value) {
                let facet_type;

                if (value.fieldName === 'CONTENT_TYPE') {
                    facet_type = 'format=';
                } else if (value.fieldName === 'LANGUAGE') {
                    facet_type = 'language=';
                } else if (value.fieldName === 'SUBJECT') {
                    facet_type = 'subject=';
                } else {
                    facet_type = '';
                }

                return `${facet_type}${value.limitToValue}`;
            },

            /**
             * Determine if a collection id is in the url and add it to selected facets, if so
             * @private
             */
            _setCollectionFromRoute() {
                let collection = this.$route.path.match(UUID_REGEX);
                if (collection !== null) {
                    this.selected_facets.push(collection[0]);

                    let facet = {
                        displayValue: this.$route.query.collection_name,
                        fieldName: 'PARENT_COLLECTION',
                        limitToValue: collection[0],
                        value: collection[0]
                    };

                    this.facet_info.push(facet);
                }
            },

            /**
             * Determine if a facet value is in the url query and add it to selected facets, if so
             * @param type
             * @param facet_value
             * @private
             */
            _setFacetFromRoute(type, facet_value) {
                if (facet_value !== undefined) {
                    let decoded_facet_value = decodeURIComponent(facet_value);
                    let grouped_facet = decoded_facet_value.split('/');

                    grouped_facet.forEach((f, index) => {
                        let limit_value = decoded_facet_value;
                        let facet_string = f;
                        let value = `/${grouped_facet.join('^')},${f}`;

                        if (index === 0) {
                            limit_value = f;
                            facet_string = this._formatFacetValue(f);
                            value = `^${f},${facet_string}`;
                        }

                        let facet = { displayValue: facet_string, limitToValue: limit_value, value: value };
                        facet.fieldName = (/format/.test(type)) ? 'CONTENT_TYPE' : type.toUpperCase();

                        this.selected_facets.push(`${type}=${limit_value}`);
                        this.facet_info.push(facet);
                    });
                }
            },

            /**
             * Format display text for facets pulled from url
             * @param facet
             * @returns {string}
             * @private
             */
            _formatFacetValue(facet) {
                let first_letter = facet.substr(0, 1).toUpperCase();
                return `${first_letter}${facet.substr(1)}`
            },

            /**
             * Set all facets from current url
             */
            setFacetsFromParams() {
                this._setCollectionFromRoute();

                let params = this.urlParams();
                POSSIBLE_FACETS.forEach((type) => {
                    this._setFacetFromRoute(type, params[type]);
                });
            }
        },

        mounted() {
            this.setFacetsFromParams();
        }
    }
</script>

<style scoped lang="scss">
    #facetList {
        h3 {
            margin-bottom: 5px;
        }

        input {
            height: 25px;
            position: relative;
            top: 5px;
            width: 25px;
        }

        .facet-display {
            margin-bottom: 25px;
        }

        label {
            float: none;
            width: 100%;
        }

        .selected_facets {
            margin-bottom: 50px;
            margin-top: -20px;
        }
    }
</style>