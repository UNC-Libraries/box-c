<!--
Displays tags for currently active filters in a search result, with the option to remove them
-->
<template>
    <div @click="updateQueryUrl" class="field is-grouped">
        <button data-type="anywhere" class="button search-text" v-if="$route.query.anywhere"><span>{{ $route.query.anywhere }}</span>
            <span class="icon"><i class="fas fa-times"></i></span></button>
        <template v-for="searchText in searchQueryDisplay">
            <button :data-type="searchText.type" :data-value="searchText.original_value" class="search-text button" :title="searchText.value_text">
                <span>
                    {{ searchText.type_text }} <i class="fas fa-greater-than"></i>
                    {{ truncateText(searchText.value_text) }}
                </span>
                <span class="icon"><i class="fas fa-times"></i></span>
            </button>
        </template>
    </div>
</template>

<script>
import routeUtils from "../mixins/routeUtils";

const TYPES = {
    added: 'Date Deposited',
    unit: 'Administrative Unit',
    collection: 'Collection',
    created: 'Date Created',
    contributor: 'Contributor',
    contributorIndex: 'Creator/Contributor',
    creator: 'Creator',
    creatorContributor: 'Creator/Contributor',
    createdYear: 'Date Created',
    format: 'Format',
    language: 'Language',
    location: 'Location',
    publisher: 'Publisher',
    subject: 'Subject',
    subjectIndex: 'Subject',
    titleIndex: 'Title',
    genre: 'Genre',
    collectionId: 'Collection Number'
}

export default {
    name: "filterTags",

    mixins: [routeUtils],

    props: {
        filterParameters: Object
    },

    computed: {
        searchQueryDisplay() {
            const query_text = Object.keys(this.filterParameters).map((paramName) => {
                return this.formatSearchValue(this.filterParameters[paramName], paramName);
            }).flat();
            return query_text.filter(v => v !== '');
        }
    },

    methods: {
        updateQueryUrl(event) {
            const params = this._updateParams(event);
            this.routeWithParams(params, this.$route.name, this.routeParams);
        },

        _updateParams(event) {
            // Find the button that was clicked, to deal with the button containing child elements
            const button = event.target.closest('button');
            if (!button) {
                return;
            }

            const query_param = button.getAttribute('data-type');
            const params = Object.assign({}, this.$route.query);
            const tag = decodeURIComponent(params[query_param]).split('||');

            if (tag.length > 1) {
                const deleted_value = event.target.getAttribute('data-value');
                let updated_results = [];

                if (query_param === 'format') {
                    updated_results = tag.filter(t => !t.startsWith(deleted_value));
                } else {
                    updated_results = tag.filter(t => t !== deleted_value);
                }

                if (updated_results.length > 0) {
                    params[query_param] = encodeURIComponent(updated_results.join('||'));
                    return params;
                }
            }

            delete params[query_param];
            return params;
        },

        formatSearchValue(fieldValue, type) {
            if (fieldValue === undefined || TYPES[type] === undefined) {
                return '';
            }
            if (Array.isArray(fieldValue)) {
                // Multivalued fields
                return fieldValue.map((value) => {
                    if (typeof value === 'object' && value !== null) {
                        return this._makeTagInfo(type, value.value, value.displayValue);
                    } else {
                        return this._makeTagInfo(type, value, value);
                    }
                });
            } else {
                // Single valued fields
                if (type === 'added' || type === 'created' || type === 'createdYear') {
                    return this._makeTagInfo(type, fieldValue, this._formatTime(fieldValue));
                } else {
                    return this._makeTagInfo(type, fieldValue, fieldValue);
                }
            }
        },

        _makeTagInfo(type, original_val, display_val) {
            return {
                type: type,
                type_text: TYPES[type],
                original_value: original_val,
                value_text: display_val
            };
        },

        _formatTime(field) {
            if (field === 'unknown') {
                return field;
            } else if (field.startsWith(',')) {
                return `All dates through ${field.replace(',', '')}`;
            } else if (field.endsWith(',')) {
                return `${field.replace(',', '')} to present date`;
            } else {
                const date_values = field.split(',');
                return `${date_values[0]} to ${date_values[1]}`
            }
        },

        truncateText(text) {
            const MAX_LENGTH = 40;
            if (text.length > (MAX_LENGTH + 1)) {
                return `${text.substr(0, MAX_LENGTH)}\u2026`;
            }
            return text;
        }
    }
}
</script>

<style scoped lang="scss">
    i.fa-greater-than {
        font-size: .6rem;
    }

    .delete.is-small {
        margin-left: 6px;
        margin-right: -5px;
    }
</style>