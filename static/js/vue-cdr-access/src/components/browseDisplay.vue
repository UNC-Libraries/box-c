<template>
    <div class="browse-records-display">
        <div v-if="record_list.length > 0" class="columns">
            <div class="column is-10">
                <browse-search></browse-search>
            </div>
            <div class="column is-2">
                <browse-sort></browse-sort>
            </div>
        </div>
        <div class="columns">
            <div class="column is-10 spacing">
                <p :class="{ no_results: record_count === 0}">
                    There {{ noteWording('are') }} <strong>{{ record_count }}</strong> {{ noteWording(childTypeText) }} in this level.
                    <browse-filters v-if="!is_admin_unit" :browse-type="browse_type" :container-type="container_metadata.type"></browse-filters>
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
    import browseFilters from "./browseFilters";
    import browseSearch from './browseSearch.vue';
    import browseSort from './browseSort.vue';
    import modalMetadata from './modalMetadata.vue';
    import pagination from './pagination.vue';
    import debounce from 'lodash.debounce';
    import chunk from 'lodash.chunk';
    import get from 'axios';
    import routeUtils from '../mixins/routeUtils';
    import browseOptionUtils from "../mixins/browseOptionUtils";

    export default {
        name: 'browseDisplay',

        components: {
            browseFilters,
            browseSearch,
            browseSort,
            modalMetadata,
            pagination
        },

        watch: {
            '$route.query'(d) {
                this.browseTypeFromUrl(this.is_admin_unit);
                if (!this.is_page_loading) {
                    this.retrieveData();
                }
            }
        },

        mixins: [routeUtils, browseOptionUtils],

        data() {
            return {
                column_size: 'is-3',
                container_name: '',
                container_metadata: {},
                default_work_type: 'Work',
                is_admin_unit: false,
                is_collection: false,
                is_folder: false,
                is_page_loading: true,
                record_count: 0,
                record_list: [],
                search_method: 'searchJson',
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
                let params = this.urlParams();

                if (this.is_admin_unit) {
                    this.default_work_type = 'Collection';
                    delete params.format;
                    delete params.browse_type;
                } else if (this.containsFolderType(this.$route.query.types)) {
                    this.default_work_type = 'Work,Folder'
                }

                params.types = this.default_work_type;
                this.$router.push({ name: 'browseDisplay', query: params });
            },

            updateParams() {
                let params = this.urlParams();

                if (this.is_collection || this.is_folder) {
                    params.types = 'Work';
                }

                if (this.is_admin_unit) {
                    params.types = 'Collection';

                    delete params.format;
                    delete params.browse_type;
                    localStorage.removeItem('dcr-browse-type');

                    this.browse_type = null;
                    this.search_method = 'listJson';
                } else if (this.browse_type === 'structure-display') {
                    params.types = 'Work,Folder';
                    delete params.format;
                    this.search_method = 'listJson';
                } else {
                    this.search_method = 'searchJson';
                }

                if (this.containsFolderType(this.$route.query.types)) {
                    params.types = 'Folder';
                    this.search_method = 'listJson';
                }

                this.default_work_type = params.types;
                return params;
            },

            retrieveData() {
                let param_string = this.formatParamsString(this.updateParams());
                this.uuid = location.pathname.split('/')[2];

                get(`${this.search_method}/${this.uuid}${param_string}`).then((response) => {
                    this.record_count = response.data.resultCount;
                    this.record_list = response.data.metadata;
                    this.container_name = response.data.container.title;
                    this.container_metadata = response.data.container;
                    this.is_page_loading = false;
                }).catch(function (error) {
                    console.log(error);
                });
            },

            findPageType() {
                // Small hack to check outside of Vue controlled DOM to see what page type we're on
                this.is_admin_unit = document.getElementById('is-admin-unit') !== null;
                this.is_collection = document.getElementById('is-collection') !== null;
                this.is_folder = document.getElementById('is-folder') !== null;

                /*
                 * Show browse options for non admin unit pages
                 * The options live outside Vue controlled DOM
                 * See access/src/main/webapp/WEB-INF/jsp/fullRecord.jsp
                 */
                if (!this.is_admin_unit) {
                    this.displayBrowseButtons();
                }

                if (this.is_admin_unit || this.is_collection || this.is_folder) {
                    this.updateUrl();
                }
            }
        },

        mounted() {
            this.findPageType();
            this.browseTypeFromUrl(this.is_admin_unit);
            this.retrieveData();

            window.addEventListener('resize', debounce(this.numberOfColumns, 300));
            this.setBrowseEvents();
            window.addEventListener('load', this.setButtonColor);
        },

        beforeDestroy() {
            window.removeEventListener('resize', this.numberOfColumns);
            this.clearBrowseEvents();
            window.removeEventListener('load', this.setButtonColor);
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