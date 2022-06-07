<template>
    <div class="meta-modal">
        <a href="#" @click.prevent="retrieveResults">More</a>
        <div v-if="show_modal" @close="show_modal = false">
            <transition name="modal">
                <div class="modal-mask">
                    <div class="modal-wrapper">
                        <div class="modal-container">

                            <div class="modal-header columns">
                                <slot name="header">
                                    <div class="column is-12">
                                        <h3>{{ facetName }}</h3>
                                        <button class="button is-small" @click="show_modal = false">{{ $t('modal.close') }}</button>
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
                                        <div class="column field is-grouped" @click.prevent="">
                                            <button @click="getPage(-displayCount)" :disabled="start_row === 0" :class="{no_link: start_row === 0}" class="button">
                                                <span class="icon"><i class="fa fa-backward"></i></span>
                                                <span>Previous</span>
                                            </button>
                                            <button :disabled="disableNextBtn" class="button" @click="getPage(displayCount)">
                                                <span>Next</span>
                                                <span class="icon"><i class="fa fa-forward"></i></span>
                                            </button>
                                        </div>
                                        <div class="column field is-grouped sorting">
                                            <button class="button" @click.prevent="sortFacets('index')">
                                                <span class="icon">
                                                    <i class="fa fa-sort-alpha-down"></i>
                                                </span>
                                                <span>A-Z Sort</span>
                                            </button>
                                            <button class="button" @click.prevent="sortFacets('count')">
                                                <span class="icon">
                                                    <i class="fa fa-sort-numeric-up"></i>
                                                </span>
                                                <span>Numerical Sort</span>
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
import get from 'axios';


export default {
    name: "facetModal",

    props: {
        facetId: String,
        facetName: String
    },

    data() {
        return {
            facet_data: {},
            num_rows: 21, // Request one more than shown, so we can determine if there's another page
            show_modal: false,
            start_row: 0,
            sort_type: 'count' // options 'count' or 'index'
        };
    },

    emits: ['facetsUpdated'],

    computed: {
        facetUrl() {
            let base_url = `/services/api/facet/${this.facetId}/listValues`;
            const query_params = `?facetSort=${this.sort_type}&facetRows=${this.num_rows}&facetStart=${this.start_row}`;

            const collection = this.$route.params.uuid;
            if (collection !== undefined) {
                base_url += `/${collection}`;
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
            get(this.facetUrl).then((response) => {
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

        getPage(start_row) {
            let start = this.start_row + parseInt(start_row);
            if (start < 0) {
                start = 0;
            }
            this.start_row = start;
            this.retrieveResults();
        }
    }
}
</script>

<style scoped lang="scss">
    $grey-border: 1px solid lightgray;

    .modal-container {
        height: auto;
        max-height: 90vh;
    }

    .modal-header {
        border-bottom: $grey-border
    }

    .meta-modal button[disabled]:hover {
        opacity: .6;
    }

    #response-text {
        ul {
            column-count: 2;
            text-align: left;
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

        .no-link {
            pointer-events: none;
        }

        .button[disabled] {
            background-color: #007FAE
        }
    }

    .sorting {
        justify-content: flex-end;
        margin-right: -25px;
    }
</style>