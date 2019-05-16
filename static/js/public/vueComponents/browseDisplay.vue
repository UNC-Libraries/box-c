<template>
    <div class="browse-records-display">
        <div v-if="numberOfRecords > 0" class="columns">
            <div class="column is-10">
                <browse-search :record-id="container_metadata.id"
                               @browse-query-results="browseSearching">
                </browse-search>
            </div>
            <div class="column is-2">
                <browse-sort :records="displayList"
                             @sort-ordering="sortOrdering">
                </browse-sort>
            </div>
        </div>
        <div class="columns">
            <div class="column is-10 spacing">
                <p :class="{ no_results: numberOfRecords === 0}">
                    There are <strong>{{ numberOfRecords }}</strong> {{ childTypeText }} in this level.
                </p>
                <p v-if="numberOfRecords > 0">
                    Displaying <strong>{{ pagination_settings.start + 1}}</strong> to
                    <strong>{{ displayNumber }}</strong> of <strong>{{ numberOfRecords }}</strong>
                </p>
            </div>
            <div class="column is-2">
                <modal-metadata  v-if="numberOfRecords > 0" :metadata="container_metadata"></modal-metadata>
            </div>
        </div>
        <div class="columns">
            <div class="column is-12">
                <ul v-if="numberOfRecords > 0">
                    <li class="column" :class="column_size" v-for="record in displayList"
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
                    column_size: 'is-3',
                    container_metadata: {},
                    pagination_settings: {},
                    record_list: []
                }
            },

            computed: {
                childTypeText: function() {
                    if (this.type === 'AdminUnit') {
                        return 'collections';
                    } else {
                        return 'items';
                    }
                },

                displayList: function() {
                    return this.record_list.slice(
                        this.pagination_settings.start,
                        this.pagination_settings.end
                    );
                },

                displayNumber: function() {
                    let page_display = this.pagination_settings.start + this.recordsPerPage;

                    if (this.numberOfRecords < page_display) {
                        return this.numberOfRecords;
                    }

                    return page_display;
                },

                numberOfRecords: function() {
                    return this.record_list.length;
                }
            },

            methods: {
                recordCountFormat: function(number) {
                    return new Intl.NumberFormat().format(number);
                },

                recordType: function(type) {
                    if (type === 'Collection') {
                        return 'fa-archive';
                    } else if (type === 'Folder') {
                        return 'fa-folder';
                    } else {
                        return 'fa-file';
                    }
                },

                numberOfColumns: function() {
                    let screen_size = window.innerWidth;

                    if (screen_size > 1023) {
                        this.column_size = 'is-3';
                    } else if (screen_size > 768) {
                        this.column_size = 'is-4';
                    } else {
                        this.column_size = 'is-6';
                    }
                },

                /**
                 * Updates list with results of BrowseSearch component custom event
                 * @param search_results
                 */
                browseSearching: function(search_results) {
                    this.container_metadata = search_results.container;
                    this.record_list = search_results.metadata;
                },

                /**
                 * Updates list with results of BrowseSort component custom event
                 * @param sorted_records
                 */
                sortOrdering: function(sorted_records) {
                    this.record_list = sorted_records;
                },

                /**
                 * Updates which page to show from the results of Pagination component custom event
                 * @param pagination_settings
                 */
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

                window.addEventListener('resize', _.debounce(this.numberOfColumns), 300);
            },

            beforeDestroy: function() {
                window.removeEventListener('resize', this.numberOfColumns);
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

    .browse-records-display div,
    .browse-records-display p {
        font-size: 1.4rem;
    }

    .browse-records-display i {
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

    .browse-records-display .no_results {
        margin-top: 25px;
    }

    .browse-records-display .spacing {
        text-align: center;
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

    @media screen and (max-width: 768px) {
        .browse-records-display .spacing p {
            line-height: 20px;
            text-align: left;
        }

        .browse-records-display .spacing p.no_results {
            text-align: center;
        }
    }
</style>