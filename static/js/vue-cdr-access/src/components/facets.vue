<template>
    <div id="facetList" class="contentarea">
        <a v-if="selected_facets.length > 0" id="clear-all" class="button is-link is-small" @click.prevent="clearAll()">
            <span class="icon is-small">
                <i class="fas fa-times"></i>
            </span> {{ $t('facets.clear')}}</a>
        <h2 class="facet-header">{{ $t('facets.filter') }}</h2>
        <div class="facet-display" v-for="facet in this.sortedFacetsList">
            <div v-if="showFacetDisplay(facet)">
                <h3>{{ facetName(facet.name) }}</h3>
                <ul v-if="facet.name !=='DATE_CREATED_YEAR'" >
                    <li v-for="value in facet.values">
                        <a class="is-selected" v-if="isSelected(value.limitToValue)" @click.prevent="updateAll(value, true)">
                            {{ value.displayValue }} ({{ value.count }}) <i class="fas fa-times"></i></a>
                        <a v-else @click.prevent="updateAll(value)">{{ value.displayValue }} ({{ value.count }})</a>
                    </li>
                </ul>
                <form v-if="facet.name === 'DATE_CREATED_YEAR'">
                    <input type="number" v-model="dates.selected_dates.start" name="start_date"
                           min="1" max="3000" aria-label="Start Date" placeholder="Start Date" />
                    &ndash;
                    <input type="number" v-model="dates.selected_dates.end" name="end_date"
                           min="1" max="3000" aria-label="End Date" placeholder="End Date" />
                    <br />
                    <input type="submit" value="Limit" @click.prevent="setDateFacetUrl()" class="button is-small" />
                    <p class="date_error" v-if="dates.invalid_date_range">The start date cannot be after the end date</p>
                </form>
            </div>
        </div>
    </div>
</template>

<script>
    import sortBy from 'lodash.sortby';
    import routeUtils from '../mixins/routeUtils';

    const UUID_REGEX = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i;
    const CURRENT_YEAR = new Date().getFullYear();
    const POSSIBLE_FACET_PARAMS = ['collection', 'createdYear', 'format', 'language', 'subject'];

    export default {
        name: 'facets',

        props: {
            facetList: Array,
            minCreatedYear: Number
        },

        mixins: [routeUtils],

        data() {
            return {
                dates: {
                    selected_dates: {
                        start: this.minCreatedYear,
                        end: CURRENT_YEAR
                    },
                    error_message: '',
                    invalid_date_range: false
                },
                min_year: this.minCreatedYear,
                selected_facets: []
            }
        },

        watch: {
            '$route.query': {
                handler() {
                    this.selected_facets = [];
                    this.setFacetsFromParams();
                },
                deep: true
            },
            minCreatedYear: {
                handler(year) {
                    // Favor the date range in the url for consistencies' sake
                    if (this.urlParams().createdYear === undefined) {
                        this.min_year = year;
                        this.dates.selected_dates.start = year
                    }
                }
            }
        },

        computed: {
            routeHasCollectionId() {
                return UUID_REGEX.test(this.$route.path);
            },

            selectedFacetInfo() {
                const display_list = [];
                this.selected_facets.map((f) => {
                    const parts = f.split(/=(.+)/, 2);
                    const facet_type = parts[0];
                    const facets = parts[1].split("||");
                    facets.forEach((fv) => {
                        display_list.push({
                            type: facet_type,
                            value: fv.toLowerCase()
                        });
                    });
                });
                return display_list;
            },

            sortedFacetsList() {
                return this.facetList.map((facet) => {
                    if (facet.name === 'CONTENT_TYPE') {
                        facet.values = sortBy(facet.values, ['limitToValue', 'count']);
                    }

                    return facet;
                });
            },

            currentYear() {
                return CURRENT_YEAR;
            }
        },

        methods: {
            clearAll() {
                this.selected_facets = [];
                this.selectedFacets();
            },

            updateAll(facet, remove = false) {
                if (remove) {
                    this.facetInfoRemove(facet);
                } else {
                    this.updateSelectedFacet(facet);
                }

                this.selectedFacets();
            },

            updateSelectedFacet(facet) {
                const facet_value = this.facetValue(facet);
                const facet_type = facet_value.split('=');
                const found_facet = this.selected_facets.findIndex((f) => {
                    return f.startsWith(`${facet_type[0]}=`);
                });
                // Remove old facet value, instead of replacing or selected_facet watcher doesn't fire
                if (found_facet !== -1) {
                    this.selected_facets.splice(found_facet, 1, facet_value);
                } else {
                    this.selected_facets.push(facet_value);
                }
            },

            isSelected(facet) {
                return this.selectedFacetInfo.findIndex(uf => uf.value === facet) !== -1;
            },

            /**
             * Used to determine if a collection has been selected. If so, remove collections from facets to select from
             * @param facet
             * @returns {boolean|boolean}
             */
            showFacetDisplay(facet) {
                if (facet.name === 'DATE_CREATED_YEAR' && this.minCreatedYear !== undefined) {
                    return true;
                }
                return facet.values.length > 0 &&
                    (facet.name !== 'PARENT_COLLECTION' || (facet.name === 'PARENT_COLLECTION' && !this.routeHasCollectionId));
            },

            /**
             * Push new url after a facet is selected/deselected
             */
            selectedFacets() {
                let updated_facet_params = this.updateUrl();
                let base_search = {
                    query: this.urlParams({}, true)
                };

                // Unset current facets
                POSSIBLE_FACET_PARAMS.forEach((facet) => delete base_search.query[facet]);
                // Add/Update with new facets
                base_search.query = Object.assign(base_search.query, updated_facet_params.queryFacets);
                this.$router.push(base_search).catch((e) => {
                    if (this.nonDuplicateNavigationError(e)) {
                        throw e;
                    }
                });
            },

            /**
             * Determine parameters to build the new url after a facet is selected/deselected
             * @returns {{path: string, queryFacets: {}}}
             */
            updateUrl() {
                return {
                    path: '/search/',
                    queryFacets: this._formatFacets(this.selected_facets)
                };
            },

            /**
             * Remove full facet info for deselected facets and their children
             * @param facet
             */
            facetInfoRemove(facet) {
               const facet_type = this.facetType(facet);
               const current_index = this.selected_facets.findIndex(sf => sf.startsWith(facet_type));

               if (current_index !== -1) {
                   const facet_parts = this.selected_facets[current_index].split('=');
                   const current_values = facet_parts[1].split('||');

                   let updated_values;
                   if (facet_type.startsWith('format')) {
                       let current_value_regex = new RegExp(facet.limitToValue);
                       updated_values = current_values.filter(f => !current_value_regex.test(f)).join('||');
                   } else {
                       updated_values = current_values.filter(f => f !== facet.limitToValue).join('||');
                   }

                   if (updated_values === '') {
                       this.selected_facets.splice(current_index, 1);
                   } else {
                       this.selected_facets.splice(current_index, 1, `${facet_parts[0]}=${updated_values}`);
                   }
               }
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
                    if (facet_pieces[0] !== 'createdYear') {
                        formatted_facets[facet_pieces[0]] = encodeURIComponent(decodeURIComponent(facet_pieces[1]));
                    } else {
                        formatted_facets[facet_pieces[0]] = facet_pieces[1];
                    }
                });

                return formatted_facets;
            },

            /**
             * Determine facet header text
             * @param value
             * @returns {string|*}
             */
            facetName(value) {
                switch (value) {
                    case 'PARENT_COLLECTION':
                        return 'Collection';
                    case 'CONTENT_TYPE':
                        return 'Format';
                    case 'LANGUAGE':
                        return 'Language';
                    case 'SUBJECT':
                        return 'Subject';
                    case 'GENRE':
                        return 'Genre';
                    case 'DATE_CREATED_YEAR':
                        return 'Date Created';
                    default:
                        return value;
                }
            },

            facetType(value) {
                switch (value.fieldName) {
                    case 'PARENT_COLLECTION':
                        return 'collection=';
                    case 'CONTENT_TYPE':
                        return 'format=';
                    case 'LANGUAGE':
                        return 'language=';
                    case 'SUBJECT':
                        return 'subject=';
                    case 'GENRE':
                        return 'genre=';
                    case 'DATE_CREATED_YEAR':
                        return 'createdYear=';
                    default:
                        return '';
                }
            },

            /**
             * Create base facet value for a selected facet
             * @param value
             * @returns {string}
             */
            facetValue(value) {
                const facet_type = this.facetType(value);
                const current_facet_value = this.selected_facets.filter(f => f.startsWith(facet_type));

                if (current_facet_value.length === 1) {
                    const selected_facet_parts = current_facet_value[0]
                        .replace(facet_type, '')
                        .split('||');
                    const facet_set = new Set(selected_facet_parts);
                    facet_set.add(value.limitToValue);

                    return `${facet_type}${Array.from(facet_set).join('||')}`
                }

                return `${facet_type}${value.limitToValue}`;
            },

            setDateFacetUrl() {
                let start = this.dates.selected_dates.start;
                let end = this.dates.selected_dates.end;
                if (start > end) {
                    this.dates.invalid_date_range = true;
                    return;
                }

                const updated_facet_value = `createdYear=${start},${end}`;
                const current_facet_value = this.selected_facets.findIndex(f => f.startsWith('createdYear'));
                if (current_facet_value !== -1) {
                    this.selected_facets[current_facet_value] = updated_facet_value
                } else {
                    this.selected_facets.push(updated_facet_value);
                }

                this.dates.invalid_date_range = false;
                this.selectedFacets();
            },

            setDateRangeFromParams(facet_value) {
                if (facet_value !== undefined) {
                    const decoded_facet_value = decodeURIComponent(facet_value);
                    const search_years = decoded_facet_value.split(',');
                    const start_year = parseInt(search_years[0]);
                    const end_year = parseInt(search_years[1]);

                    // Ignore invalid date ranges
                    if (Number.isNaN(start_year) || Number.isNaN(end_year) || start_year > end_year) {
                        return;
                    }

                    this.dates.selected_dates.start = start_year;
                    this.min_year = start_year;
                    this.dates.selected_dates.end = end_year;
                } else {
                    // In this case selected start date set by minSearchYear watcher
                    this.dates.selected_dates.end = this.currentYear;
                }
            },

            /**
             * Determine if a facet value is in the url query and add it to selected facets, if so.
             * This method triggers every time a facet is added/removed from the mounted() method
             * as the url change triggers a reload.
             * @param type
             * @param facet_value
             * @private
             */
            _setFacetFromRoute(type, facet_value) {
                if (facet_value !== undefined) {
                    const decoded_facet_value = decodeURIComponent(facet_value);
                    const updated_value = `${type}=${decoded_facet_value}`
                    this.selected_facets.push(updated_value);
                }

                // Set date ranges
                if (type === 'createdYear') {
                    this.setDateRangeFromParams(facet_value);
                }
            },

            /**
             * Set all facets from current url
             */
            setFacetsFromParams() {
                let params = this.urlParams();
                POSSIBLE_FACET_PARAMS.forEach((type) => {
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
    $cdr-blue: #1A698C;
    #facetList {
        h3 {
            font-size: 18px;
            margin-bottom: 5px;
        }

        li {
            margin-left: 15px;
            padding-top: 5px;
        }

        .is-link {
            background-color: $cdr-blue;
            border-radius: 5px;
            font-size: .85rem;
            margin-bottom: 10px;
            margin-top: 12px;
            padding: 1.1em 0.8em 1.1em 1.2em;
        }

        .facet-header {
            padding-top: 18px;
        }

        .facet-display {
            margin-bottom: 25px;
            text-transform: capitalize;

            a, i {
                padding-left: 15px;
            }

            i {
                color: $cdr-blue;
                position: relative;
                vertical-align: text-top;
            }

            .is-selected {
                color: black;
                text-decoration: none;
            }
        }

        a.button {
            width: initial;

            span {
                padding-right: 10px;
            }
        }

        form {
            margin-top: 5px;
            input[type=number] {
                max-width: 100px;
            }
            input[type=submit] {
                margin-top: 10px;
                background-color: $cdr-blue;
                color: white;
                font-weight: bold;
                border-radius: 5px;
            }
        }
        .date_error {
            color: #cc0f35;
            font-size: 15px;
            margin-top: 10px;
        }
    }
</style>