<!--
Facet list component, used to display all the values of facets and provide links for applying those values as filters on the current search.
-->
<template>
    <div id="facetList">
        <h2 class="subtitle">{{ $t('facets.filter') }}</h2>
        <div class="facet-display" :id="'facet-display-' + facetType(facet.name, false)" v-for="facet in this.sortedFacetsList">
            <h3 class="title is-5 mb-2">{{ facetName(facet.name) }}</h3>
            <slider v-if="showDateSelectors(facet) && hasValidDateRangeValues(dates.selected_dates)" ref="sliderInfo"
                    :start-range="[dates.selected_dates.start, dates.selected_dates.end]"
                    :range-values="{min: dates.selected_dates.start, max: currentYear}" @sliderUpdated="sliderUpdated"></slider>
            <form v-if="showDateSelectors(facet) && hasValidDateRangeValues(dates.selected_dates)">
                <input type="number" v-model="dates.selected_dates.start" name="start_date"
                       aria-label="Start Date" placeholder="Start Date" />
                &ndash;
                <input type="number" v-model="dates.selected_dates.end" name="end_date"
                       aria-label="End Date" placeholder="End Date" />
                <input type="submit" value="Limit" @click.prevent="setDateFacetUrl()" class="button is-small" />
                <p class="date_error" v-if="dates.invalid_date_range">The start date cannot be after the end date</p>
            </form>

            <div v-for="value in facet.values" class="columns is-mobile facet-entry">
                <div class="column is-four-fifths">
                    <a class="is-selected" v-if="isSelected(value)" @click.prevent="updateAll(value, true)">
                            {{ value.displayValue }} <i class="fas fa-times"></i></a>
                        <a v-else @click.prevent="updateAll(value)">{{ value.displayValue }}</a>
                </div>
                <div class="column has-text-right facet-count">
                    {{ value.count }}
                </div>
            </div>

            <div class="mt-2">
                <facet-modal v-if="showMoreResults(facet)" :facet-id="facetType(facet.name, false)"
                    :facet-name="facetName(facet.name)" @facetValueAdded="modalFacetValueAdded" ></facet-modal>
            </div>
        </div>
    </div>
</template>

<script>
    import facetModal from "@/components/facetModal.vue";
    import slider from "@/components/slider.vue";
    import routeUtils from '../mixins/routeUtils';

    const CURRENT_YEAR = new Date().getFullYear();
    const FACET_RESULT_COUNT = 6;

    export default {
        name: 'facets',

        components: {facetModal, slider},

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
            minCreatedYear(newValue, oldValue) {
                if (oldValue === undefined || newValue < oldValue) {
                    this.dates.selected_dates.start = newValue;
                }
            }
        },

        computed: {
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
                return this.facetList.filter((facet) => this.showFacetDisplay(facet));
            },

            currentYear() {
                return CURRENT_YEAR;
            }
        },

        methods: {
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
                const facet_type = this.facetType(facet.fieldName, false);
                const facet_value = facet.value.toLowerCase();
                return this.selectedFacetInfo.findIndex(uf => uf.type === facet_type
                        && uf.value.toLowerCase() === facet_value) !== -1;
            },

            /**
             * Determine if a facet should be displayed or not
             * @param facet
             * @returns {boolean|boolean}
             */
            showFacetDisplay(facet) {
                if (facet.name === 'DATE_CREATED_YEAR' && this.minCreatedYear !== undefined) {
                    return true;
                }
                return facet.values.length > 0;
            },

            showMoreResults(facet) {
                return facet.name !== 'DATE_CREATED_YEAR' && facet.values.length >= FACET_RESULT_COUNT;
            },

            showDateSelectors(facet) {
                if (facet.name !== 'DATE_CREATED_YEAR') {
                    return false;
                }
                const facet_type = this.facetType(facet.name);
                const current_facet_value = this.selected_facets.filter(f => f.startsWith(facet_type));
                return current_facet_value.length === 0 || current_facet_value[0] !== (facet_type + "unknown");
            },

            hasValidDateRangeValues(date_values) {
                return date_values.start && isFinite(date_values.start)
                    && date_values.end && isFinite(date_values.end);
            },

            /**
             * Push new url after a facet is selected/deselected
             * Reset start row of search to 0
             */
            selectedFacets() {
                let updated_facet_params = this.updateUrl();
                let base_search = {
                    query: this.urlParams({ start: 0 }, true)
                };

                // Unset current facets
                this.possibleFacetFields.forEach((facet) => delete base_search.query[facet]);
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
               const facet_type = this.facetType(facet.fieldName);
               const current_index = this.selected_facets.findIndex(sf => sf.startsWith(facet_type));

               if (current_index !== -1) {
                   const facet_parts = this.selected_facets[current_index].split('=');
                   const current_values = facet_parts[1].split('||');

                   let updated_values = current_values.filter(f => f !== facet.limitToValue).join('||');

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
                    case 'PARENT_UNIT':
                        return 'Collecting Unit';
                    case 'FILE_FORMAT_CATEGORY':
                        return 'Format';
                    case 'LANGUAGE':
                        return 'Language';
                    case 'LOCATION':
                        return 'Location';
                    case 'SUBJECT':
                        return 'Subject';
                    case 'GENRE':
                        return 'Genre';
                    case 'DATE_CREATED_YEAR':
                        return 'Date Created';
                    case 'PUBLISHER':
                        return 'Publisher';
                    case 'CREATOR_CONTRIBUTOR':
                        return 'Creator/Contributor';
                    default:
                        return value;
                }
            },

            facetType(value, query_value = true) {
                let type = ''

                switch (value) {
                    case 'PARENT_COLLECTION':
                        type = 'collection';
                        break;
                    case 'PARENT_UNIT':
                        type = 'unit';
                        break;
                    case 'FILE_FORMAT_CATEGORY':
                        type = 'format';
                        break;
                    case 'LANGUAGE':
                        type = 'language';
                        break;
                    case 'LOCATION':
                        type = 'location';
                        break;
                    case 'SUBJECT':
                        type = 'subject';
                        break;
                    case 'GENRE':
                        type = 'genre';
                        break;
                    case 'DATE_CREATED_YEAR':
                        type = 'createdYear';
                        break;
                    case 'PUBLISHER':
                        type = 'publisher';
                        break;
                    case 'CREATOR_CONTRIBUTOR':
                        type = 'creatorContributor';
                        break;
                    default:
                        break;
                }

                return query_value ? `${type}=`: type;
            },

            /**
             * Create base facet value for a selected facet
             * @param value
             * @returns {string}
             */
            facetValue(value) {
                const facet_type = this.facetType(value.fieldName);
                const current_facet_value = this.selected_facets.filter(f => f.startsWith(facet_type));

                if (current_facet_value.length === 1 && value.fieldName !== 'DATE_CREATED_YEAR') {
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
                    this.dates.selected_dates.end = end_year;
                } else {
                    this.dates.selected_dates.start = this.minCreatedYear;
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
                this.possibleFacetFields.forEach((type) => {
                    this._setFacetFromRoute(type, params[type]);
                });
            },

            sliderUpdated(values) {
                this.dates.selected_dates.start = values[0];
                this.dates.selected_dates.end = values[1];
            },

            modalFacetValueAdded(facet) {
                this.updateAll(facet);
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
        .columns {
            margin: 0;
            padding: 0 .75rem 0 .1rem;
            line-height: 1.25rem;
        }

        .column {
            padding: .25rem 0;
            text-indent: 1rem hanging;
        }

        .facet-display {
            margin-bottom: 1rem;
            text-transform: capitalize;

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

        form {
            float: none;
            margin-bottom: 15px;
            margin-top: 5px;
            margin-left: 15px;
            input[type=number] {
                max-width: 100px;
                padding: 3px;
            }
            input[type=submit] {
                margin-left: 8px;
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