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
                    <browse-images :container_type="container_metadata.type"></browse-images>
                </p>
            </div>
            <div class="column is-2">
                <modal-metadata :uuid="uuid" :title="container_name"></modal-metadata>
            </div>
        </div>
        <div class="columns">
            <div class="column is-12" >
                <ul class="column is-12" v-for="records in chunkedRecords">
                    <li v-for="record in records" class="column" :class="column_size">
                        <a :href="record.uri">
                            <img v-if="hasThumbnail(record.thumbnail_url)" :src="record.thumbnail_url">
                            <i v-else class="fa" :class="recordType(record.type)"></i>
                            <div class="record-count" :class="{ thumbnail: hasThumbnail(record.thumbnail_url) }">
                                <div>{{ recordCountFormat(record.counts.child) }}</div>
                            </div>
                            <div class="record-title">{{ record.title }}</div>
                        </a>
                    </li>
                </ul>
                <p class="spacing" v-if="chunkedRecords.length === 0">No records were found.</p>
            </div>
        </div>
        <pagination :number-of-records="record_count"
                    :page-base-url="container_metadata.uri">
        </pagination>
    </div>
</template>

<script>
    import browseImages from "./browseImages";
    import browseSearch from './browseSearch.vue';
    import browseSort from './browseSort.vue';
    import modalMetadata from './modalMetadata.vue';
    import pagination from './pagination.vue';
    import debounce from 'lodash.debounce';
    import chunk from 'lodash.chunk';
    import {utils} from "../utils/helper_methods";

    export default {
        name: 'browseDisplay',

        components: {
            browseImages,
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
                is_collection: false,
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

            chunkedRecords() {
                if (this.column_size === 'is-4') {
                    return chunk(this.record_list, 3);
                } else if (this.column_size === 'is-6') {
                    return chunk(this.record_list, 2);
                } else {
                    return chunk(this.record_list, 4);
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
                if (this.record_count === 1 && word === 'are') {
                    return 'is';
                } else if (word === 'are') {
                    return word;
                }

                return `${word}s`;
            },

            hasThumbnail(thumb) {
              return thumb !== undefined && thumb !== '';
            },

            updateUrl() {
                let params = utils.urlParams();
                params.types = 'Work';
                this.$router.push({ name: 'browseDisplay', query: params });
            },

            /**
             * Updates list with results of BrowseSearch component custom event
             * @param search_results
             */
            browseSearching(search_results) {
                this.container_name = search_results.title;
                this.record_list = search_results.metadata;
                this.container_metadata = search_results.container;
            },

            retrieveData() {
                let self = this;
                let params = utils.urlParams();

                if (this.is_collection) {
                    params.types = 'Work';
                }

                let param_string = utils.formatParamsString(params);
                this.uuid = location.pathname.split('/')[2];

                fetch(`listJson/${this.uuid}${param_string}`)
                    .then(function(response) {
                        return response.json();
                    }).then(function(data) {
                        self.record_count = data.resultCount;
                        self.record_list = data.metadata;
                        self.container_name = data.container.title;
                        self.container_metadata = data.container;
                });
            }
        },

        mounted() {
            // Small hack to check outside of Vue controlled DOM to see if we're on the collections browse page
            this.is_collection = document.getElementById('is-collection') !== null;

            if (this.is_collection) {
                this.updateUrl();
            }

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

        p.spacing {
            margin-bottom: 50px;
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
            margin-bottom: 35px;
            margin-left: -15px;
            margin-top: -65px;

            div {
                font-size: 40px;
            }
        }

        .record-title {
            margin-left: -15px;
            margin-top: 65px;
        }

        .thumbnail {
            background-color: rgba(192, 192, 192, 0.4);
            height: 100px;
            float: none;
            margin: -145px auto 0 auto;
            width: 200px;

            div {
                margin-top: 40px;
                padding-top: 45px;
            }
        }

        .thumbnail + .record-title {
            margin-left: 10px;
            margin-top: 75px
        }

        img {
            height: 100px;
            margin-left: 15px;
            width: 200px;
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