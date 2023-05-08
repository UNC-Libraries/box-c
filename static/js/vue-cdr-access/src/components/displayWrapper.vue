<!--
Top level component for full record pages with searching/browsing, including AdminUnits, Collections, and Folders.
-->
<template>
    <header-small/>
    <div v-if="is_page_loading" class="loading-icon">
        <img :src="nonVueStaticImageUrl('ajax-loader-lg.gif')" alt="data loading icon">
    </div>
    <div v-if="!is_page_loading" class="browse-header">
        <div class="columns">
            <div class="column crumbs">
                <bread-crumbs :object-path="container_info.briefObject.objectPath">
                </bread-crumbs>
            </div>
        </div>
        <admin-unit v-if="container_info.resourceType === 'AdminUnit'" :record-data="container_info"></admin-unit>
        <collection-folder v-if="container_info.resourceType === 'Collection' || container_info.resourceType === 'Folder'"
                           :record-data="container_info"></collection-folder>
        <aggregate-record v-if="container_info.resourceType === 'Work'" :record-data="container_info"></aggregate-record>
        <file-record v-if="container_info.resourceType === 'File'" :record-data="container_info"></file-record>

        <div v-if="container_info.resourceType !== 'Work' && container_info.resourceType !== 'File'" class="background-white">
            <div class="columns is-tablet">
                <div class="column is-6">
                    <browse-search :object-type="container_metadata.type"></browse-search>
                </div>
                <div class="column is-2" v-if="showWidget">
                    <browse-sort browse-type="display"></browse-sort>
                </div>
                <div class="column is-2 container-note" v-if="showWidget">
                    <works-only></works-only>
                </div>
                <div class="column is-narrow-tablet" v-if="showWidget">
                    <view-type></view-type>
                </div>
            </div>
            <clear-filters :filter-parameters="filter_parameters"></clear-filters>

            <div v-if="showWidget" class="columns">
                <div class="facet-list column is-one-quarter facets-border">
                    <facets :facet-list="facet_list" :min-created-year="minimumCreatedYear"></facets>
                </div>
                <div id="fullRecordSearchResultDisplay" class="column is-three-quarters">
                    <gallery-display v-if="isBrowseDisplay" :record-list="record_list"></gallery-display>
                    <list-display v-else :record-list="record_list" :is-record-browse="true"></list-display>
                </div>
            </div>
            <p v-else class="spacing">{{ $t('search.no_results') }}</p>
            <pagination browse-type="display" :number-of-records="record_count"></pagination>
        </div>
    </div>
</template>

<script>
    import adminUnit from '@/components/full_record/adminUnit.vue';
    import aggregateRecord from '@/components/full_record/aggregateRecord.vue';
    import breadCrumbs from '@/components/full_record/breadCrumbs.vue';
    import browseSearch from '@/components/browseSearch.vue';
    import browseSort from '@/components/browseSort.vue';
    import clearFilters from '@/components/clearFilters.vue';
    import collectionFolder from '@/components/full_record/collectionFolder.vue';
    import fileRecord from '@/components/full_record/fileRecord.vue';
    import galleryDisplay from '@/components/galleryDisplay.vue';
    import headerSmall from '@/components/header/headerSmall.vue';
    import listDisplay from '@/components/listDisplay.vue';
    import facets from "@/components/facets.vue";
    import pagination from '@/components/pagination.vue';
    import viewType from '@/components/viewType.vue';
    import worksOnly from '@/components/worksOnly.vue';
    import get from 'axios';
    import isEmpty from 'lodash.isempty';
    import analyticsUtils from '../mixins/analyticsUtils';
    import imageUtils from '../mixins/imageUtils';
    import routeUtils from '../mixins/routeUtils';

    const FACETS_REMOVE_ADMIN_UNIT = [ 'unit' ];
    const FACETS_REMOVE_COLLECTION_AND_CHILDREN = [ 'unit', 'collection' ];
    const GET_SEARCH_RESULTS = ['AdminUnit', 'Collection', 'Folder'];

    export default {
        name: 'displayWrapper',

        watch: {
            '$route': {
                handler(new_data, old_data) {
                    if (this.is_page_loading) {
                        return;
                    }
                    let path_changed = new_data.path !== old_data.path;
                    if (path_changed) {
                        // If the object being viewed has changed, then need to ensure search results
                        // are requested after it finishes loading.
                        this.getBriefObject().then(() => {
                            if (this.needsSearchResults) {
                                this.retrieveSearchResults();
                            }
                        });
                    } else {
                        if (this.needsSearchResults) {
                            this.retrieveSearchResults();
                        }
                    }
                },
                deep: true
            }
        },

        components: {
            aggregateRecord,
            adminUnit,
            breadCrumbs,
            browseSearch,
            browseSort,
            clearFilters,
            collectionFolder,
            fileRecord,
            galleryDisplay,
            headerSmall,
            listDisplay,
            pagination,
            viewType,
            worksOnly,
            facets
        },

        mixins: [analyticsUtils, imageUtils, routeUtils],

        data() {
            return {
                container_info: {},
                container_name: '',
                container_metadata: {},
                default_work_type: 'Work',
                is_page_loading: true,
                record_count: 0,
                record_list: [],
                facet_list: [],
                search_method: 'listJson',
                uuid: '',
                filter_parameters: {}
            }
        },

        head() {
            return {
                title: this.container_info.pageSubtitle || ''
            }
        },

        computed: {
            isBrowseDisplay() {
                let browse_type = this.urlParams().browse_type;
                if (browse_type === undefined) {
                    browse_type = sessionStorage.getItem('browse-type');
                }
                return browse_type === 'gallery-display';
            },

            showWidget() {
                return this.record_list.length > 0;
            },

            needsSearchResults() {
                return GET_SEARCH_RESULTS.includes(this.container_info.resourceType);
            }
        },

        methods: {
            retrieveSearchResults() {
                let param_string = this.formatParamsString(this.updateParams()) + '&getFacets=true';
                this.uuid = location.pathname.split('/')[2];
                get(`/${this.search_method}/${this.uuid}${param_string}`).then((response) => {
                    this.record_count = response.data.resultCount;
                    this.record_list = response.data.metadata;
                    this.facet_list = response.data.facetFields;
                    this.container_name = response.data.container.title;
                    this.container_metadata = response.data.container;
                    this.min_created_year = response.data.minSearchYear;
                    this.filter_parameters = response.data.filterParameters;
                    this.is_page_loading = false;
                    if (!isEmpty(this.$route.query)) {
                        this.updateUrl();
                    }
                }).catch(function (error) {
                    console.log(error);
                });
            },

            getBriefObject() {
                let link = window.location.pathname;
                if (!(/\/$/.test(link))) {
                    link += '/';
                }

               return get(`${link}json`).then((response) => {
                   this.container_info = response.data;
                   this.pageEvent(response.data);
                   this.pageView(this.container_info.pageSubtitle)
                   if (this.needsSearchResults) {
                       this.adjustFacetsForRetrieval();
                   } else {
                       this.is_page_loading = false;
                   }
                }).catch(error => {
                    console.log(error);
                });
            },

            getCollectionName(briefObject) {
                let has_collection_name = briefObject.parentCollectionName !== undefined &&
                    briefObject.parentCollectionName !== '';

                if (has_collection_name) {
                    return briefObject.parentCollectionName;
                } else if (!has_collection_name && briefObject.resourceType === 'Collection') {
                    return briefObject.title;
                } else {
                    return '(no collection)';
                }
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
                let query_params = this.$route.query;
                return Object.keys(query_params).some(key => query_params[key]
                    && this.allPossibleSearchParameters.indexOf(key) >= 0);
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

            /**
             * Adjusts which facets should be retrieved and displayed based on what type of object is being viewed
             */
            adjustFacetsForRetrieval() {
                let facets_to_remove = [];
                if (this.container_info.resourceType === 'AdminUnit') {
                    facets_to_remove = FACETS_REMOVE_ADMIN_UNIT;
                } else if (this.container_info.resourceType === 'Collection' ||
                    this.container_info.resourceType === 'Folder') {
                    facets_to_remove = FACETS_REMOVE_COLLECTION_AND_CHILDREN;
                }
                this.$store.commit('removePossibleFacetFields', facets_to_remove);
            }
        },

        created() {
            this.getBriefObject().then(() => {
                if (this.needsSearchResults) {
                    this.retrieveSearchResults();
                }
            });
        }
    }
</script>

<style scoped lang="scss">
    .loading-icon {
        margin-top: 50px;
        text-align: center;
    }

    .crumbs {
        padding-right: 0;
        padding-bottom: 0;
    }

    .collection-info-bottom,
    .collinfo_metadata {
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

    .columns {
        margin-bottom: 0;
    }

    .tags {
        margin-bottom: 0;
        margin-left: 30px;
    }

    .is-6 {
        padding-left: 50px;
    }

    #facetList {
        .facet-header {
            padding: 0 0 20px 0;
        }
    }

    #facetList.contentarea {
        margin: 0 20px 20px 38px;
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