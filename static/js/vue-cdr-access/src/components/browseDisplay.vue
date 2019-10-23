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
            <div class="column is-11 container-note">
                <p :class="{ hidden: record_count === 0}">
                    <view-type></view-type>
                </p>
            </div>
            <div class="column is-1">
                <modal-metadata :uuid="uuid" :title="container_name"></modal-metadata>
            </div>
        </div>
        <div class="columns">
            <div class="column is-12" >
                <ul class="column is-12" v-for="records in chunkedRecords">
                    <li v-for="record in records" class="column" :class="column_size">
                        <a :href="recordUrl(record.id)">
                            <img v-if="hasThumbnail(record.thumbnail_url)" :src="record.thumbnail_url" class="thumbnail thumbnail-size-large">
                            <i v-else class="fa" :class="recordType(record.type)"></i>
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
    import browseSearch from './browseSearch.vue';
    import browseSort from './browseSort.vue';
    import modalMetadata from './modalMetadata.vue';
    import pagination from './pagination.vue';
    import viewType from './viewType';
    import debounce from 'lodash.debounce';
    import chunk from 'lodash.chunk';
    import get from 'axios';
    import routeUtils from '../mixins/routeUtils';

    export default {
        name: 'browseDisplay',

        components: {
            browseSearch,
            browseSort,
            modalMetadata,
            pagination,
            viewType,
        },

        watch: {
            '$route.query'(d) {
                if (!this.is_page_loading) {
                    this.retrieveData();
                }
            }
        },

        mixins: [routeUtils],

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
            recordUrl(id) {
                return `/record/${id}`;
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

            hasThumbnail(thumb) {
              return thumb !== undefined && thumb !== '';
            },

            updateUrl() {
                let params = this.urlParams();

                if (this.is_admin_unit) {
                    this.default_work_type = 'Collection';
                } else if (params.browse_type === 'structure-display') {
                    this.default_work_type = 'Work,Folder';
                } else {
                    this.default_work_type = 'Work';
                }

                params.types = this.default_work_type;
                this.$router.push({ name: 'browseDisplay', query: params });
            },

            updateParams() {
                let params = this.urlParams();

                if (this.is_collection || this.is_folder) {
                    params.types = 'Work';
                }

                if (params.browse_type === 'structure-display') {
                    params.types = (this.is_admin_unit) ? 'Collection' : 'Work,Folder';
                    this.search_method = 'listJson';
                } else {
                    this.search_method = 'searchJson';
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
                this.updateUrl();
            }
        },

        mounted() {
            this.findPageType();
            this.retrieveData();
            window.addEventListener('resize', debounce(this.numberOfColumns, 300));
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
            width: 100%;
        }

        li {
            text-indent: 0;
        }

        div, p {
            font-size: 1.2rem;
        }

        p.spacing {
            margin-bottom: 50px;
        }

        i {
            font-size: 9rem;
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

        .record-title {
            margin-top: 25px;
        }

        .thumbnail + .record-title {
            margin-top: 165px;
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