<!--
Modal facet component, used to display all the values of a particular facet in a paginated form and provide links for applying those values as filters on the current search.
-->
<template>
    <div class="meta-modal">
        <a href="#" class="button more" @click.prevent="retrieveResults">View more</a>
        <div v-if="show_modal">
            <transition name="modal">
                <div class="modal-mask">
                    <div class="modal-wrapper">
                        <div class="modal-container">

                            <div class="modal-header columns">
                                <slot name="header">
                                    <div class="column is-12">
                                        <h3>{{ facetName }}</h3>
                                        <button title="Close" class="button" aria-label="Close" @click="closeModal">&times;</button>
                                    </div>
                                </slot>
                            </div>

                            <div class="modal-body" id="facet_data_display">
                                <slot name="body">
                                    <div id="response-text">
                                        <ul>
                                            <template v-for="(facet_value, index) in facet_data.values">
                                                <li v-if="index < displayCount">
                                                    <a @click.prevent="selectedFacet(facet_value)" href="/">{{ facet_value.displayValue }} ({{ facet_value.count }})</a>
                                                </li>
                                            </template>
                                        </ul>
                                    </div>
                                </slot>
                            </div>
                            <div class="modal-footer">
                                <slot name="footer">
                                    <div class="columns">
                                        <div class="column field is-grouped paging">
                                            <button :disabled="start_row === 0" class="button" @click="changePage(-displayCount)">
                                                <span class="icon"><i class="fa fa-backward"></i></span>
                                                <span>Previous</span>
                                            </button>
                                            <button :disabled="disableNextBtn" class="button" @click="changePage(displayCount)">
                                                <span>Next</span>
                                                <span class="icon"><i class="fa fa-forward"></i></span>
                                            </button>
                                            <span class="current-page">Viewing Page: {{ current_page }}&hellip;</span>
                                        </div>
                                        <div class="column field sorting buttons has-addons">
                                            <button class="button" :aria-pressed="hasSort('count')"
                                                    :class="{active: hasSort('count')}" @click.prevent="sortFacets('count')">
                                                <span class="icon">
                                                    <i class="fa fa-sort-numeric-up"></i>
                                                </span>
                                                <span>Numerical Sort</span>
                                            </button>
                                            <button class="button" :aria-pressed="hasSort('index')"
                                                    :class="{active: hasSort('index')}" @click.prevent="sortFacets('index')">
                                                <span class="icon">
                                                    <i class="fa fa-sort-alpha-down"></i>
                                                </span>
                                                <span>A-Z Sort</span>
                                            </button>
                                        </div>
                                    </div>
                                </slot>
                            </div>
                        </div>
                    </div>
                </div>
            </transition>
        </div>
    </div>
</template>

<script>
import axios from 'axios';
import cloneDeep from "lodash.clonedeep";

export default {
    name: "facetModal",

    props: {
        facetId: String,
        facetName: String
    },

    data() {
        return {
            current_page: 1,
            facet_data: {},
            num_rows: 21, // Request one more than shown, so we can determine if there's another page
            show_modal: false,
            start_row: 0,
            sort_type: 'count' // options 'count' or 'index'
        };
    },

    emits: ['facetValueAdded'],

    computed: {
        facetQuery() {
            const current_query = cloneDeep(this.$route.query);
            const skip = ['facetSelect', 'rows', 'sort', 'start', 'browse_type'];
            let updated_query = '';
            Object.keys(current_query).forEach((key) => {
                if (!skip.includes(key)) {
                    updated_query += `&${key}=${current_query[key]}`;
                }
            })
            return updated_query;
        },

        facetUrl() {
            let base_url = `/services/api/facet/${this.facetId}/listValues`;
            let query_params = `?facetSort=${this.sort_type}&facetRows=${this.num_rows}&facetStart=${this.start_row}`;
            query_params += `${this.facetQuery}`;

            const uuid = this.$route.params.id;
            if (uuid !== undefined && uuid !== '') {
                base_url += `/${uuid}`;
            }

            return base_url + query_params;
        },

        numberOfResults() {
            const facet_values = this.facet_data.values;
            return facet_values !== undefined ? facet_values.length : 0;
        },

        disableNextBtn() {
            return this.numberOfResults < this.num_rows;
        },

        displayCount() {
            return this.num_rows - 1;
        }
    },

    methods: {
        retrieveResults() {
            axios.get(this.facetUrl).then((response) => {
                this.facet_data = response.data;
                this.show_modal = true;
            }).catch(function (error) {
                console.log(error);
            });
        },

        selectedFacet(facetValue) {
            this.show_modal = false;
            this.$emit('facetValueAdded', facetValue);
        },

        sortFacets(sort) {
            this.sort_type = sort;
            this.start_row = 0;
            this.retrieveResults();
        },

        hasSort(sort_type) {
            return this.sort_type === sort_type;
        },

        changePage(row_offset) {
            let start = this.start_row + parseInt(row_offset);
            if (start < 0) {
                start = 0;
            }
            this.start_row = start;
            const new_page = (row_offset < 0) ? -1 : 1;
            this.current_page += new_page;
            this.retrieveResults();
        },

        closeModal() {
            this.show_modal = false;
        },

        closeEsc(e) {
            if (e.keyCode === 27) {
                this.closeModal();
            }
        }
    },

    mounted() {
        document.addEventListener("keydown", this.closeEsc);
    },

    beforeDestroy() {
        document.removeEventListener("keydown", this.closeEsc);
    }
}
</script>

<style scoped lang="scss">
    $grey-border: 1px solid lightgray;

    .more.button.is-focused,
    .more.button:focus {
        color: black;
    }

    .column {
        padding: .25rem .75rem;
    }

    .modal-container {
        height: initial;
        padding: 15px 0;
    }

    .modal-header {
        border-bottom: $grey-border;
        padding: 0 15px;
        margin: 0;

        button {
            font-size: 24px;
            font-weight: bold;
            height: 32px;
            padding: 0 4px 4px;
            width: 32px;
        }

        h3 {
            text-align: left;
        }

        div {
            padding-left: 0;
            padding-right: 0;
        }
    }

    .modal-body,
    .modal-footer {
        padding: 0 15px;
    }

    .meta-modal button[disabled]:hover {
        opacity: .6;
    }

    #response-text {
        height: 45vh;
        overflow-y: auto;

        ul {
            column-count: 2;
            text-align: left;
        }

        li {
            break-inside: avoid;
            margin-bottom: 8px;
        }
    }

    .modal-footer {
        border-top: $grey-border;
        margin-top: 5px;

        .columns {
            margin-top: 10px;
            a:first-child {
                margin-right: 8px;
            }
        }

        .current-page {
            font-size: 1rem;
            font-weight: bold;
            margin: auto;
            padding-left: 5px;
        }

        .button[disabled] {
            background-color: #007FAE
        }

        .sorting {
            margin-bottom: 12px;
            padding-bottom: 0;
            padding-top: 4px;

            .button {
                background-color: white;
                color: black;
            }

            .active {
                box-shadow: inset 0 3px 5px rgba(0,0,0,0.125);
                color: #333;
                background-color: #e6e6e6 !important;
                border-color: #adadad;
                pointer-events: none;
                cursor: pointer;
            }
        }
    }

    .sorting {
        justify-content: flex-end;
        margin-right: -25px;
    }

    @media screen and (max-width: 768px) {
        .sorting {
            justify-content: flex-start;
            margin-right: 0;
        }
    }

    @media screen and (max-width: 600px) {
        a.button {
            width: initial;
        }
    }
</style>