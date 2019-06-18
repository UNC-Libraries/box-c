<template>
    <div class="columns pagination">
        <div class="column is-12">
            <ul v-if="numberOfRecords > 0">
                <li v-if="currentPage !== 1"><a class="back-next" @click.prevent="pageUrl(currentPage - 1)" href="#">&lt;&lt;</a></li>
                <li v-else class="no-link">&lt;&lt;</li>
                <li v-if="currentPage >= pageLimit - 1"><a @click.prevent="pageUrl(1)" href="#" class="page-number"
                                                           :class="{ current: currentPage === 1 }">1</a> ...</li>
                <li v-for="(page, index) in currentPageList">
                    <a v-if="index < pageLimit" @click.prevent="pageUrl(page)" href="#" class="page-number" :class="{ current: currentPage === page }">{{ page }}</a>
                </li>
                <li v-if="totalPageCount > pageLimit && (currentPage < totalPageCount - pageOffset)">
                    ... <a @click.prevent="pageUrl(totalPageCount)" href="#" class="page-number"
                           :class="{ current: currentPage === totalPageCount }">{{totalPageCount }}</a>
                </li>
                <li v-if="currentPage < totalPageCount"><a class="back-next" @click.prevent="pageUrl(currentPage + 1)" href="#">&gt;&gt;</a></li>
                <li v-else class="no-link">&gt;&gt;</li>
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
            numberOfRecords: Number,
            pageBaseUrl: String
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
               if (isEmpty(this.$route.query) || parseInt(this.$route.query.start) === 0) {
                   return 1;
               }

               return Math.ceil(parseInt(this.$route.query.start) / this.rows_per_page) + 1;
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

                this.$router.push({ name: 'browseDisplay', query: this.urlParams(update_params) });
            }
        },

        updated() {
            this.setPageTotal();
        }
    }
</script>

<style scoped lang="scss">
    .pagination {
        margin-bottom: 1px;

        ul {
            display: inline;

            li {
                display: inline;
                margin: 5px;
            }
        }

        .page-number {
            background-color: #007FAE;
            padding: 15px;

            &:hover {
                color: white;
                opacity: .8;
                text-decoration: none;
            }
        }

        a {
            color: white;

            &.back-next {
                color: #007FAE;
            }
        }

        .current {
            background-color: gray;
        }

        .no-link {
            color: gray;
        }
    }
</style>