<template>
    <div id="facetList" class="contentarea">
        <h2 class="facet-header">Filter results by...</h2>
        <div class="selected_facets" v-if="facet_info.length > 0">
            <ul>
                <li v-for="value in facet_info">
                    <label :aria-label="value.displayValue" @click="facetInfo(value)">
                        <input type="checkbox" v-model="selected_facets" :value="facetValue(value)">
                        {{ value.displayValue }}
                    </label>
                </li>
            </ul>
        </div>
        <div class="facet-display" v-if="facet.values.length > 0" v-for="facet in this.facetList">
            <h3>{{ facetName(facet.name) }}</h3>
            <ul>
                <li v-for="value in facet.values">
                    <label :aria-label="value.displayValue" @click="facetInfo(value)">
                        <input type="checkbox" v-model="selected_facets" :value="facetValue(value)">
                        {{ value.displayValue }} ({{ value.count }})
                    </label>
                </li>
            </ul>
        </div>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';

    const UUID_REGEX = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i;

    export default {
        name: 'facets',

        props: {
            facetList: Array
        },

        mixins: [routeUtils],

        data() {
            return {
                facet_info: [],
                selected_facets: [],
            }
        },

        watch: {
            selected_facets() {
                this.selectedFacets();
            }
        },

        methods: {
            selectedFacets() {
                const POSSIBLE_FACETS = ['format', 'language', 'subject'];
                let updated_facet_params = this.updateUrl();

                let base_search = {
                    query: this.urlParams({}, true)
                };
                // Unset current facets
                POSSIBLE_FACETS.forEach((facet) => delete base_search.query[facet]);
                // Add/Update with new facets
                base_search.query = Object.assign(base_search.query, updated_facet_params.queryFacets);

                if (updated_facet_params.collection !== '') {
                    base_search.path = updated_facet_params.path
                } else {
                    base_search.name = 'searchRecords';
                }

                this.$router.push(base_search);
            },

            updateUrl() {
                let updated_facets = this.selected_facets;
                let path = '/search/';
                let collection_name = '';
                let collection = this.selected_facets.findIndex((facet) => {
                    return UUID_REGEX.test(facet);
                });

                if (collection !== -1) {
                    collection_name = this.selected_facets[collection];
                    path += `${this.selected_facets[collection]}`;

                    // Remove collection from facets array without removing it from this.selected_facets
                    updated_facets = [
                        ...this.selected_facets.slice(0, collection),
                        ...this.selected_facets.slice(collection + 1)
                    ];
                }

                return {
                    collection: collection_name,
                    path: path,
                    queryFacets: this._formatFacets(updated_facets)
                };
            },

            facetInfo(facet) {
                let facet_index = this.facet_info.findIndex((f) => {
                   return f.value === facet.value;
                });

                if (facet_index === -1) {
                    this.facet_info.push(facet);
                } else {
                    this.facet_info = [
                        ...this.facet_info.slice(0, facet_index),
                        ...this.facet_info.slice(facet_index + 1)
                    ];
                }
            },

            _formatFacets(updated_facets) {
                let formatted_facets = {};
                updated_facets.forEach((facet) => {
                    let facet_pieces = facet.split('=');
                    formatted_facets[facet_pieces[0]] = encodeURIComponent(facet_pieces[1]);
                });

                return formatted_facets;
            },

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

            facetValue(value) {
                let facet_type;

                if (value.fieldName  === 'CONTENT_TYPE') {
                    facet_type = 'format=';
                } else if (value.fieldName  === 'LANGUAGE') {
                    facet_type = 'language=';
                } else if (value.fieldName === 'SUBJECT') {
                    facet_type = 'subject=';
                } else {
                    facet_type = '';
                }

                return `${facet_type}${value.limitToValue}`;
            },

            addCollection() {
                let collection = this.$route.path.match(UUID_REGEX);
                if (collection !== null) {
                    this.selected_facets.push(collection[0]);

                    let collections = this.facetList.find((f) => f.name === 'PARENT_COLLECTION');
                    if (collections !== undefined) {
                        this.facetInfo(collections.values.find((f) => f.value === collection[0]));
                    }
                }
            }
        },

        mounted() {
            this.addCollection();
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