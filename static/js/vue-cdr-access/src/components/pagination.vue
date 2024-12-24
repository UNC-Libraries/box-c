<!--
Pagination component for search results, listing pages, previous/next buttons, counts.
-->
<template>
    <div class="columns is-mobile">
        <div class="column is-12">
            <nav v-if="numberOfRecords > 0" class="pagination is-centered is-justify-content-center" role="navigation" aria-label="pagination">
                <a class="pagination-previous" :class="{'is-disabled': currentPage === 1 }" @click.prevent="pageUrl(currentPage - 1, $event)" href="#">{{ $t('pagination.previous') }}</a>
                <a class="pagination-next" :class="{'is-disabled': currentPage === totalPageCount }" @click.prevent="pageUrl(currentPage + 1, $event)" href="#">{{ $t('pagination.next') }}</a>

                <ul class="pagination-list is-flex-grow-0">
                    <li id="first-page-link">
                        <a href="#" @click.prevent="pageUrl(1, $event)" class="pagination-link" :class="{ 'is-current': currentPage === 1 }" v-bind="pageAriaFields(1)">
                            1
                        </a>
                    </li>
                    <li v-if="totalPageCount > maxPagesDisplayed && currentPageList[0] !== 2">
                        <span class="pagination-ellipsis pagination-ellipsis-start">&hellip;</span>
                    </li>
                    <li v-if="totalPageCount > 2" v-for="(page, index) in currentPageList">
                        <a href="#" @click.prevent="pageUrl(page, $event)" class="pagination-link" :class="{ 'is-current': currentPage === page }" v-bind="pageAriaFields(page)">
                            {{ page }}
                        </a>
                    </li>
                    <li v-if="totalPageCount > maxPagesDisplayed && currentPageList[currentPageList.length - 1] !== totalPageCount - 1">
                        <span class="pagination-ellipsis pagination-ellipsis-end">&hellip;</span>
                    </li>
                    <li id="last-page-link" v-if="totalPageCount >= 2">
                        <a href="#" @click.prevent="pageUrl(totalPageCount, $event)" class="pagination-link" :class="{ 'is-current': currentPage === totalPageCount }" v-bind="pageAriaFields(totalPageCount)">
                            {{ totalPageCount }}
                        </a>
                    </li>
                </ul>
            </nav>
        </div>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';
    import isEmpty from 'lodash.isempty';
    import range from 'lodash.range';

    export default {
        name: 'pagination',

        props: {
            browseType: String,
            numberOfRecords: Number
        },

        mixins: [routeUtils],

        data() {
            return {
                pageLimit: 5,
                totalPageCount: 1
            }
        },

        computed: {
            currentPage() {
                let query = this.$route.query;
                let start_row = query.start;

                if (isEmpty(query) || parseInt(start_row) === 0 ||  (this.$route.name === 'searchRecords' &&
                    (start_row === undefined || parseInt(start_row) === 0))) {
                    return 1;
                }

                return Math.ceil(parseInt(start_row) / parseInt(this.rows_per_page)) + 1;
            },

            // List of pages to display, minus the first and last pages
            currentPageList() {
                if (this.totalPageCount > this.maxPagesDisplayed) {
                    let current_page = this.currentPage;
                    let padding_page_count = 3; // Target number of pages to display on either side of the current page, including first and last
                    let range_start;
                    let range_end;

                    if (current_page <= padding_page_count) { // first pages
                        range_start = 2;
                        range_end = this.maxPagesDisplayed;
                    } else if (current_page >= this.totalPageCount - padding_page_count) { // last pages
                        range_start = this.totalPageCount - this.pageLimit;
                        range_end = this.totalPageCount;
                    } else { // all other pages
                        range_start = current_page - padding_page_count + 1;
                        range_end = current_page + padding_page_count;
                    }
                    return range(range_start, range_end);
                }

                return range(2, this.totalPageCount);
            },

            maxPagesDisplayed() {
                return this.pageLimit + 2;
            }
        },

        methods: {
            pageAriaFields(page_number) {
                if (this.currentPage === page_number) {
                    return {
                        'aria-label': `Page ${page_number}`,
                        'aria-current': 'page'
                    }
                }
                return {
                    'aria-label': `Goto page ${page_number}`
                }
            },

            setPageTotal() {
                this.totalPageCount = Math.ceil(this.numberOfRecords / this.rows_per_page);
            },

            pageUrl(page_number, event) {
                // Prevent default action if the button is disabled
                var targetClasses = event.target.closest('a').classList;
                if (targetClasses.contains('is-disabled') || targetClasses.contains('is-active')) {
                    return;
                }
                if (page_number === undefined) page_number = 1;

                let start_record = this.rows_per_page * (parseInt(page_number) - 1);
                let update_params = {
                    start: start_record,
                    rows: this.rows_per_page + ''
                };

                if (this.browseType === 'display') {
                    this.$router.push({ name: 'displayRecords', query: this.urlParams({
                            start: start_record,
                            rows: this.rows_per_page + ''
                        })
                    });
                } else {
                    this.$router.push({ path: this.$route.path, query: this.urlParams(update_params = {
                            start: start_record,
                            rows: this.rows_per_page + ''
                        }, true)
                    }).catch((e) => {
                        if (e.name !== 'NavigationDuplicated') {
                            throw e;
                        }
                    });
                }
            }
        },

        mounted() {
            this.setPageTotal();
        },

        updated() {
            this.setPageTotal();
        }
    }
</script>

<style scoped lang="scss">
    .is-current {
        background-color: #007FAE;
    }

    .is-current:hover, .is-disabled:hover {
        text-decoration: none;
    }
</style>