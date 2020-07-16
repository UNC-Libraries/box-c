<template>
    <div class="columns pagination is-mobile">
        <div class="column is-12">
            <ul v-if="numberOfRecords > 0">
                <li v-if="currentPage !== 1"><a class="back-next start" @click.prevent="pageUrl(currentPage - 1)" href="#">Previous</a></li>
                <li v-else class="no-link start">Previous</li>
                <li id="first-page-link" v-if="currentPage >= pageLimit - 1 && totalPageCount > pageLimit"><a @click.prevent="pageUrl(1)" href="#" class="page-number"
                                                           :class="{ current: currentPage === 1 }">1</a> ...</li>
                <li v-for="(page, index) in currentPageList">
                    <a v-if="index < pageLimit" @click.prevent="pageUrl(page)" href="#" class="page-number" :class="{ current: currentPage === page }">{{ page }}</a>
                </li>
                <li id="last-page-link" v-if="totalPageCount > pageLimit && (currentPage < totalPageCount - pageOffset)">
                    ... <a @click.prevent="pageUrl(totalPageCount)" href="#" class="page-number"
                           :class="{ current: currentPage === totalPageCount }">{{totalPageCount }}</a>
                </li>
                <li v-if="currentPage < totalPageCount"><a class="back-next end" @click.prevent="pageUrl(currentPage + 1)" href="#">Next</a></li>
                <li v-else class="no-link end">Next</li>
            </ul>
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
                pageOffset: 2,
                startRecord: 1,
                totalPageCount: 1
            }
        },

        computed: {
            currentPage() {
                let query = this.$route.query;
                let display_type = (this.$route.name === 'searchRecords') ? query['a.setStartRow'] : query.start;

                if (isEmpty(query) || parseInt(query.start) === 0 ||  (this.$route.name === 'searchRecords' &&
                    (query['a.setStartRow'] === undefined || parseInt(query['a.setStartRow']) === 0))) {
                    return 1;
                }

                return Math.ceil(parseInt(display_type) / parseInt(this.rows_per_page)) + 1;
            },

            currentPageList() {
                let page_list = range(1, this.totalPageCount + 1);

                if (this.totalPageCount > this.pageLimit) {
                    let current_page = this.currentPage;
                    let end_offset = 1;
                    let slice_offset = 3;
                    let slice_start;
                    let slice_end;

                    if (current_page === 1 || current_page === 2) { // first pages
                        slice_start = 0;
                        slice_end = this.pageLimit;
                    } else if (current_page === this.totalPageCount) { // last page
                        slice_start = current_page - this.pageLimit;
                        slice_end = this.totalPageCount;
                    } else if (current_page === this.totalPageCount - end_offset) { // next to last page
                        slice_start = (current_page - this.pageLimit) + end_offset;
                        slice_end = this.totalPageCount;
                    } else { // all other pages
                        slice_start = current_page - slice_offset;
                        slice_end = current_page + this.pageOffset;
                    }

                    return page_list.slice(slice_start, slice_end);
                }

                return page_list;
            }
        },

        methods: {
            setPageTotal() {
                this.totalPageCount = Math.ceil(this.numberOfRecords / this.rows_per_page);
            },

            pageUrl(page_number) {
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
                            'a.setStartRow': start_record,
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
    $link-color: #007FAE;
    $no-link-color: #686868;

    .pagination {
        display: inline-block;
        margin-bottom: 1px;
        margin-top: 20px;
        width: 100%;

        ul {
            display: inline;

            li {
                display: inline;
                margin: 5px;
            }
        }

        .page-number {
            background: linear-gradient(to bottom, #4B9CD3 0%, $link-color 100%);
            border: 1px solid $link-color;
            border-radius: 5px;
            padding: 0.5em 1em;

            &:hover {
                color: white;
                opacity: .8;
                text-decoration: none;
            }
        }

        a {
            color: white;

            &.back-next {
                color: $link-color;
            }
        }

        .current {
            background: linear-gradient(to bottom, #fff 0%, #dcdcdc 100%);
            border-color: $no-link-color;
            color: black;

            &:hover {
                color: black;
            }
        }

        .no-link {
            color: $no-link-color;
        }
    }
</style>