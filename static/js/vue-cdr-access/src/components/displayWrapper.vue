<template>
    <div>
        <div v-if="record_list.length > 0">
            <div class="columns is-tablet">
                <div class="column is-three-fifths">
                    <browse-search></browse-search>
                </div>
                <div class="column is-one-fifth">
                    <browse-sort></browse-sort>
                </div>
                <div class="column is-one-fifth">
                    <view-type></view-type>
                </div>
            </div>
            <div class="columns">
                <div class="column is-11 container-note">
                    <works-only :admin-unit="is_admin_unit"></works-only>
                </div>
                <div class="column is-1">
                    <modal-metadata :uuid="uuid" :title="container_name"></modal-metadata>
                </div>
            </div>
            <browse-display v-if="isBrowseDisplay" :record-list="record_list"></browse-display>
            <list-display v-else :record-list="record_list"></list-display>
        </div>
        <p v-else class="spacing">No records were found.</p>
        <pagination :number-of-records="record_count"
                    :page-base-url="container_metadata.uri">
        </pagination>
    </div>
</template>

<script>
    import browseDisplay from './browseDisplay';
    import browseSearch from './browseSearch';
    import browseSort from './browseSort';
    import listDisplay from './listDisplay';
    import modalMetadata from './modalMetadata';
    import pagination from './pagination';
    import viewType from './viewType';
    import worksOnly from './worksOnly';
    import get from 'axios';
    import routeUtils from '../mixins/routeUtils';

    export default {
        name: 'displayWrapper',

        watch: {
            '$route.query'(d) {
                if (!this.is_page_loading) {
                    this.retrieveData();
                }
            }
        },

        components: {
            browseDisplay,
            browseSearch,
            browseSort,
            listDisplay,
            modalMetadata,
            pagination,
            viewType,
            worksOnly
        },

        mixins: [routeUtils],

        data() {
            return {
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
            isBrowseDisplay() {
                return this.urlParams().browse_type === 'gallery-display';
            }
        },

        methods: {
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

            updateUrl() {
                let params = this.setTypes();
                this.$router.push({ name: 'displayRecords', query: params });
            },

            updateParams() {
                let params = this.setTypes();
                this.search_method = (params.browse_type === 'list-display') ? 'listJson' : 'searchJson';
                return params;
            },

            setTypes() {
                let params = this.updateWorkType(this.is_admin_unit, this.urlParams().works_only);
                this.default_work_type = params.types;
                return params;
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
        },
    }
</script>

<style scoped lang="scss">
    .collection-info-bottom, .collinfo_metadata {
        margin-top: 0;
    }

    p {
        font-size: 1.2rem;
    }

    i {
        font-size: 9rem;
    }

    .no_results {
        margin-top: 25px;
    }

    p.spacing {
        margin-bottom: 50px;
        text-align: center;
    }

    @media screen and (max-width: 768px) {
        .browse-records-display {
            .spacing {
                p {
                    line-height: 20px;
                    text-align: left;

                    &.no_results {
                        text-align: center;
                    }
                }
            }

            input {
                margin-top: 0;
            }

            .is-2 {
                margin-top: inherit;
            }

            .is-tablet {
                display: inherit;
                width: inherit;
            }

            .column.is-three-fifths {
                padding-bottom: 0;
            }
        }
    }
</style>