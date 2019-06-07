<template>
    <div class="browse-records-display">
        <div v-if="numberOfRecords > 0" class="columns">
            <div class="column is-10">
                <browse-search :record-id="container_metadata.id"
                               @browse-query-results="browseSearching">
                </browse-search>
            </div>
            <div class="column is-2">
                <browse-sort :page-base-url="container_metadata.uri">
                </browse-sort>
            </div>
        </div>
        <div class="columns">
            <div class="column is-10 spacing">
                <p :class="{ no_results: record_count === 0}">
                    There {{ noteWording('are') }} <strong>{{ record_count }}</strong> {{ noteWording(childTypeText) }} in this level.
                </p>
            </div>
            <div class="column is-2">
                <modal-metadata :uuid="uuid" :title="container_name"></modal-metadata>
            </div>
        </div>
        <div class="columns">
            <div class="column is-12">
                <ul v-if="numberOfRecords > 0">
                    <li class="column" :class="column_size" v-for="record in record_list"
                        :key="record.id">
                        <a :href="record.uri">
                            <i class="fa" :class="recordType(record.type)"></i>
                            <div class="record-count">{{ recordCountFormat(record.counts.child) }}</div>
                            <div class="record-title">{{ record.title }}</div>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
        <pagination :number-of-records="record_count"
                    :page-base-url="container_metadata.uri">
        </pagination>
    </div>
</template>

<script>
    import browseSearch from './browseSearch.vue';
    import browseSort from './browseSort.vue';
    import modalMetadata from './modalMetadata.vue';
    import pagination from './pagination.vue';
    import debounce from 'lodash.debounce';
    import {utils} from "../utils/helper_methods";

    export default {
        name: 'browseDisplay',

        components: {
            browseSearch,
            browseSort,
            modalMetadata,
            pagination
        },

        watch: {
            '$route.query'(d) {
                this.retrieveData();
            }
        },

        data() {
            return {
                column_size: 'is-3',
                container_name: '',
                container_metadata: '',
                record_count: 0,
                record_list: [],
                uuid: ''
            }
        },

        computed: {
            childTypeText() {
                if (this.container_metadata.type === 'AdminUnit') {
                    return 'collection';
                } else {
                    return 'item';
                }
            },

            numberOfRecords() {
                return this.record_list.length;
            }
        },

        methods: {
            recordCountFormat(number) {
                return new Intl.NumberFormat().format(number);
            },

            recordType(type) {
                if (type === 'Collection') {
                    return 'fa-archive';
                } else if (type === 'Folder') {
                    return 'fa-folder';
                } else {
                    return 'fa-file';
                }
            },

            numberOfColumns() {
                let screen_size = window.innerWidth;

                if (screen_size > 1023) {
                    this.column_size = 'is-3';
                } else if (screen_size > 768) {
                    this.column_size = 'is-4';
                } else {
                    this.column_size = 'is-6';
                }
            },

            noteWording(word) {
                if (this.record_count === 1) {
                    if (word === 'are') {
                        return 'is';
                    }

                    return word;
                }

                return `${word}s`;
            },

            /**
             * Updates list with results of BrowseSearch component custom event
             * @param search_results
             */
            browseSearching(search_results) {
                this.container_name = search_results.title;
                this.record_list = search_results.metadata;
            },

            retrieveData() {
                let self = this;
                let params = utils.urlParams();
                let param_string = utils.formatParamsString(params);
                this.uuid = location.pathname.split('/')[2];

                fetch(`listJson/${this.uuid}${param_string}`)
                    .then(function(response) {
                        return response.json();
                    }).then(function(data) {
                        self.record_count = data.resultCount;
                        self.record_list = data.metadata;
                        self.container_name = data.container.title;
                });
            }
        },

        mounted() {
            this.retrieveData();
            window.addEventListener('resize', debounce(this.numberOfColumns), 300);
        },

        beforeDestroy() {
            window.removeEventListener('resize', this.numberOfColumns);
        }
    };
</script>

<style scoped lang="scss">
    .browse-records-display {
        .columns {
            display: inline-flex;
            width: 100%;
        }

        ul {
            display: inline-flex;
            text-align: center;
            width: 100%;
        }

        div, p {
            font-size: 1.4rem;
        }

        i {
            font-size: 10rem;
        }

        button {
            background-color: #007FAE;
            color: white;

            &:hover {
                color: white;
                opacity: .9;
            }
        }

        .no_results {
            margin-top: 25px;
        }

        .spacing {
            text-align: center;
        }

        .record-count {
            color: white;
            font-size: 40px;
            margin-bottom: 35px;
            margin-left: -15px;
            margin-top: -45px;
        }

        .record-title {
            margin-left: -15px;
        }

        @media screen and (max-width: 768px) {
            .spacing {
                p {
                    line-height: 20px;
                    text-align: left;

                    &.no_results {
                        text-align: center;
                    }
                }
            }
        }
    }
</style>