<template>
    <div class="browse-records-display">
        <div v-if="numberOfRecords > 0" class="columns">
            <div class="column is-10">
                <browse-search :records="record_list"
                               :record-type="type"
                               @browse-query-results="browseSearching">
                </browse-search>
            </div>
            <div class="column is-2">
                <browse-sort :records="record_list"
                             @sort-ordering="sortOrdering">
                </browse-sort>
            </div>
        </div>
        <div class="columns">
            <div class="column is-7 is-offset-3 spacing">
                <p>There are <strong>{{ numberOfRecords }}</strong> {{ childTypeText }} in this {{ typeText }}.</p>
                <p v-if="numberOfRecords > 0">Displaying {{ pagination_settings.start + 1}} to {{ pagination_settings.start + recordsPerPage}}</p>
            </div>
            <div class="column is-2">
                <modal-metadata  v-if="numberOfRecords > 0" :metadata="container_metadata"></modal-metadata>
            </div>
        </div>
        <div class="columns">
            <div class="column is-12">
                <ul v-if="numberOfRecords > 0">
                    <li class="column is-3" v-for="record in displayList"
                        :key="record.id">
                        <a :href="record.uri">
                            <div>
                                <i class="fa fa-archive"></i>
                                <div class="record-count">{{ recordCount(record.counts.child) }}</div>
                            </div>
                            <div class="record-title">{{ record.title }}</div>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
        <pagination :per-page="recordsPerPage"
                    :number-of-records="numberOfRecords"
                    :page-base-url="container_metadata.uri"
                    @pagination-records-to-display="pageToDisplay">
        </pagination>
    </div>
</template>

<script>
    const COMPONENT_PATH = 'vue!public/vueComponents';
    const MODULES = [
        'Vue',
        `${COMPONENT_PATH}/modalMetadata`,
        `${COMPONENT_PATH}/browseSearch`,
        `${COMPONENT_PATH}/browseSort`,
        `${COMPONENT_PATH}/pagination`
    ];

    define(MODULES, function(Vue) {
        Vue.component('browseDisplay', {
            props: {
                path: String,

                recordsPerPage: {
                    type: Number,
                    default: 20
                },

                type: String
            },

            template: template,

            data: function() {
                return {
                    container_metadata: {},
                    pagination_settings: {},
                    record_list: []
                }
            },

            computed: {
                childTypeText: function() {
                    if (this.type === 'AdminUnit') {
                        return 'collections';
                    } else if (this.type === 'Collection') {
                        return 'folders and files';
                    } else {
                        return 'files';
                    }
                },

                typeText: function() {
                    if (this.type === 'AdminUnit') {
                        return 'administrative unit';
                    } else {
                        return this.type.toLowerCase();
                    }
                },

                displayList: function() {
                    return this.record_list.slice(this.pagination_settings.start, this.pagination_settings.end);
                },

                numberOfRecords: function() {
                    return this.record_list.length;
                }
            },

            methods: {
                recordCount: function(number) {
                    return new Intl.NumberFormat().format(number);
                },

                /**
                 * Updates list with results of BrowseSearch component custom event
                 * @param search_results
                 */
                browseSearching: function(search_results) {
                    this.record_list = search_results;
                },

                /**
                 * Updates list with results of BrowseSort component custom event
                 * @param sorted_records
                 */
                sortOrdering: function(sorted_records) {
                    this.record_list = sorted_records;
                },

                pageToDisplay: function(pagination_settings) {
                    this.pagination_settings = pagination_settings;
                }
            },

            mounted: function() {
                let self = this;
                fetch(this.path)
                    .then(function(response) {
                        return response.json();
                    }).then(function(data) {
                        self.container_metadata = data.container;
                        self.record_list = data.metadata;
                    });
            }
        });
    });
</script>

<style>
    .browse-records-display .columns {
        display: inline-flex;
        width: 100%;
    }

    .browse-records-display ul {
        display: inline-flex;
        text-align: center;
        width: 100%;
    }

    .browse-records-display div {
        font-size: 1.4rem;
    }

    .browse-records-display i.fa-archive {
        font-size: 10rem;
    }

    .browse-records-display button {
        background-color: #007FAE;
        color: white;
    }

    .browse-records-display button:hover {
        color: white;
        opacity: .9;
    }

    .browse-records-display .spacing {
        margin-top: 25px;
    }

    .browse-records-display .record-count {
        color: white;
        font-size: 40px;
        margin-bottom: 35px;
        margin-left: -15px;
        margin-top: -45px;
    }

    .browse-records-display .record-title {
        margin-left: -15px;
    }
</style>