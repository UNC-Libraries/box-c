<!--
Top level component for full record pages with searching/browsing, including AdminUnits, Collections, and Folders.
-->
<template>
    <div>
        <div class="columns is-tablet">
            <div class="column is-6">
                <browse-search :object-type="container_metadata.type"></browse-search>
            </div>
            <div class="column is-2" v-if="showWidget">
                <browse-sort browse-type="display"></browse-sort>
            </div>
            <div class="column is-2 container-note" v-if="showWorksOnly">
                <works-only :admin-unit="is_admin_unit"></works-only>
            </div>
            <div class="column is-narrow-tablet" v-if="showWidget">
                <view-type></view-type>
            </div>
        </div>
        <div v-if="showWidget">
            <browse-display v-if="isBrowseDisplay" :record-list="record_list"></browse-display>
            <list-display v-else :record-list="record_list" :is-record-browse="true"></list-display>
        </div>
        <p v-else class="spacing">{{ $t('search.no_results') }}</p>
        <modal-metadata :uuid="uuid" :title="container_name"></modal-metadata>
        <pagination browse-type="display" :number-of-records="record_count"></pagination>
    </div>
</template>

<script>
    import browseDisplay from '@/components/browseDisplay.vue';
    import browseSearch from '@/components/browseSearch.vue';
    import browseSort from '@/components/browseSort.vue';
    import listDisplay from '@/components/listDisplay.vue';
    import modalMetadata from '@/components/modalMetadata.vue';
    import pagination from '@/components/pagination.vue';
    import viewType from '@/components/viewType.vue';
    import worksOnly from '@/components/worksOnly.vue';
    import get from 'axios';
    import isEmpty from 'lodash.isempty';
    import routeUtils from '../mixins/routeUtils';

    export default {
        name: 'displayWrapper',

        watch: {
            '$route.query': {
                handler(d) {
                    if (!this.is_page_loading) {
                        this.retrieveData();
                    }
                },
                deep: true
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
                search_method: 'listJson',
                uuid: ''
            }
        },

        computed: {
            isBrowseDisplay() {
                return this.urlParams().browse_type === 'gallery-display';
            },

            showWidget() {
                return this.record_list.length > 0;
            },

            showWorksOnly() {
                return this.showWidget || this.coerceWorksOnly(this.$route.query.works_only);
            },


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
                this.$router.push({ name: 'displayRecords', query: params }).catch((e) => {
                    if (this.nonDuplicateNavigationError(e)) {
                        throw e;
                    }
                });
            },

            hasSearchQuery() {
                let query = this.$route.query.anywhere;
                return query !== undefined && query !== '';
            },

            updateParams() {
                let params = this.setTypes();
                this.search_method = (this.coerceWorksOnly(params.works_only) || this.hasSearchQuery()) ? 'searchJson' : 'listJson';
                return params;
            },

            setTypes() {
                let params = this.updateWorkType(this.urlParams().works_only);
                this.default_work_type = params.types;
                return params;
            },

            findPageType() {
                // Small hack to check outside of Vue controlled DOM to see what page type we're on
                this.is_admin_unit = document.getElementById('is-admin-unit') !== null;
                this.is_collection = document.getElementById('is-collection') !== null;
                this.is_folder = document.getElementById('is-folder') !== null;

                // Don't update route if no url parameters are passed in
                if (!isEmpty(this.$route.query)) {
                    this.updateUrl();
                }
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

    i {
        font-size: 9rem;
    }

    .no_results {
        margin-top: 25px;
    }

    p.spacing {
        color: #222;
        font-size: 20px;
        margin-top: 50px;
        margin-bottom: 20px;
        text-align: center;
    }

    .container-note {
        padding: 20px 0;
    }

    .is-6 {
        padding-left: 50px;
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
        }

        .is-6 {
            padding-left: 25px;
        }

        .container-note {
            padding: 25px 0 0 25px;
        }

        .is-narrow-tablet {
            padding-top: 0;
        }
    }
</style>