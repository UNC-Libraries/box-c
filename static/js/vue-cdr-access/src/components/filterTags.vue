<template>
    <div>
        <ul @click="updateQueryUrl">
            <li data-type="anywhere" class="search-text" v-if="$route.query.anywhere">{{ $route.query.anywhere }}
                <i class="fas fa-times" data-type="anywhere"></i></li>
            <template v-for="searchText in searchQueryDisplay">
                <li :data-type="searchText.type" :data-value="searchText.original_value" class="search-text" :title="searchText.value_text">
                    {{ searchText.type_text }} <i class="fas fa-greater-than"></i>
                    {{ truncateText(searchText.value_text) }}
                    <i class="fas fa-times" :data-type="searchText.type" :data-value="searchText.original_value"></i>
                </li>
            </template>
        </ul>
    </div>
</template>

<script>
import cloneDeep from "lodash.clonedeep";
import routeUtils from "../mixins/routeUtils";

const TYPES = {
    added: 'Date Added',
    collection: 'Collection',
    created: 'Date Created',
    contributor: 'Contributor',
    contributorIndex: 'Creator/Contributor',
    creator: 'Creator',
    creatorIndex: 'Creator',
    creatorContributor: 'Creator/Contributor',
    format: 'Format',
    location: 'Location',
    subject: 'Subject',
    subjectIndex: 'Subject',
    titleIndex: 'Title',
    genre: 'Genre'
}

export default {
    name: "filterTags",

    mixins: [routeUtils],

    props: {
        facetList: Array
    },

    computed: {
        searchQueryDisplay() {
            const params = Object.keys(this.$route.query);
            const query_text = params.map((param) => {
                return this.formatSearchValue(this.$route.query[param], param);
            }).flat();
            return query_text.filter(v => v !== '');
        }
    },

    methods: {
        updateQueryUrl(event) {
            const params = this._updateParams(event);
            this.$router.push({ name: 'searchRecords', query: params }).catch((e) => {
                if (this.nonDuplicateNavigationError(e)) {
                    throw e;
                }
            });
        },

        _updateParams(event) {
            const query_param = event.target.getAttribute('data-type');
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
            let display_text = decodeURIComponent(fieldValue);

            // Format time based tags
            if (type === 'added' || type === 'created') {
                display_text = this._formatTime(fieldValue);
            }

            let tag_info = {
                type: type,
                type_text: TYPES[type],
                original_value: fieldValue,
                value_text: display_text
            };

            // Return non multi-value tags
            const MULTI_VALUED_FIELDS = ['format', 'collection', 'creatorContributor'];
            if (!MULTI_VALUED_FIELDS.includes(type)) {
                return tag_info;
            }

            // Multi-value tags
            return this._updateMultiValueTags(display_text, type, tag_info);
        },

        _formatTime(field) {
            if (field.startsWith(',')) {
                return `All dates through ${field.replace(',', '')}`;
            } else if (field.endsWith(',')) {
                return `${field.replace(',', '')} to present date`;
            } else {
                const date_values = field.split(',');
                return `${date_values[0]} to ${date_values[1]}`
            }
        },

        _updateMultiValueTags(display_text, type, tag_info) {
            const tag_values = display_text.split('||');

            // Format collection tags
            if (type === 'collection') {
                const collections = this.facetList.find(f => f.name === 'PARENT_COLLECTION');
                if (collections === undefined) {
                    return tag_info;
                }

                return tag_values.map((f_value) => {
                    let collection_text = f_value;
                    let original_value = f_value;

                    const selected_collection = collections.values.find(c => c.limitToValue === f_value);
                    if (selected_collection !== undefined) {
                        collection_text = selected_collection.displayValue;
                        original_value = selected_collection.limitToValue;
                    }

                    return this._multiValueDisplay(tag_info, collection_text, original_value);
                });
            }

            // All other multi-value tags
            return tag_values.map(f => this._multiValueDisplay(tag_info, f, f));
        },

        _multiValueDisplay(facet_info, display_value, original_value) {
            let current_facet_info = cloneDeep(facet_info);
            current_facet_info['value_text'] = display_value;
            current_facet_info['original_value'] = original_value;
            return current_facet_info;
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
    ul {
        display: flex;
        flex-wrap: wrap;
        margin-top: 10px;

        &:hover {
            cursor: pointer;
        }

        .search-text {
            border: 1px solid lightgray;
            border-radius: 5px;
            font-size: .85rem;
            margin-left: 5px;
            padding-left: 7px;
            text-indent: 0;
        }
    }

    i.fa-times {
        background-color: #1A698C;
        border: 1px solid #1A698C;
        border-bottom-right-radius: 5px;
        border-top-right-radius: 5px;
        color: white;
        margin-left: 5px;
        padding: 7px;
    }

    i.fa-greater-than {
        color: #B0B0B0;
        font-size: .6rem;
    }
</style>