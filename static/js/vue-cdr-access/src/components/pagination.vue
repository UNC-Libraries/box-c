<template>
    <div class="columns pagination">
        <div class="column is-12">
            <ul v-if="numberOfRecords > 0">
                <li v-if="pageFromUrl !== 1"><a class="back-next" @click.prevent="pageUrl(pageFromUrl - 1)" href="#">&lt;&lt;</a></li>
                <li v-else class="no-link">&lt;&lt;</li>
                <li v-if="pageFromUrl > pageLimit"><a @click.prevent="pageUrl(1)" href="#" class="page-number"
                                             :class="{ current: pageFromUrl === 1 }">1</a> ...</li>
                <li v-for="(page, index) in currentPages">
                    <a v-if="index < pageLimit" @click.prevent="pageUrl(page)" href="#" class="page-number" :class="{ current: pageFromUrl === page }">{{ page }}</a>
                </li>
                <li v-if="pageFromUrl < pageLimit">... <a @click.prevent="pageUrl(totalPageCount)" href="#"
                                                             class="page-number" :class="{ current: pageFromUrl === totalPageCount }">{{totalPageCount }}</a></li>
                <li v-if="pageFromUrl < totalPageCount"><a class="back-next" @click.prevent="pageUrl(pageFromUrl + 1)" href="#">&gt;&gt;</a></li>
                <li v-else class="no-link">&gt;&gt;</li>
            </ul>
        </div>
    </div>
</template>

<script>
    import {utils} from '../utils/helper_methods';
    import isEmpty from 'lodash.isempty';

    export default {
        name: 'pagination',

        props: {
            numberOfRecords: Number,
            pageBaseUrl: String
        },

        data() {
            return {
                pageLimit: 5,
                perPage: 1,
                startRecord: 1,
                totalPageCount: 1
            }
        },

        computed: {
            pageFromUrl() {
               if (isEmpty(this.$route.query)) {
                   return 1;
               }

               return parseInt(this.$route.query.page);
            },

            currentPages() {
                let page_list = [...Array(this.totalPageCount).keys()];
                let current_page = this.pageFromUrl;

                if (this.totalPageCount > this.pageLimit) {
                    if ((current_page + this.pageLimit) > this.totalPageCount) {
                        return page_list.slice((current_page + 1) - this.pageLimit, current_page + this.pageLimit);
                    }

                    return page_list.slice(current_page, current_page + this.pageLimit);
                }

                return page_list;
            }
        },

        methods: {
            setPageTotal() {
                this.totalPageCount = Math.ceil(this.numberOfRecords / this.perPage);
            },

            pageUrl(page_number) {
                if (page_number === undefined) page_number = 1;
                let start_record = this.perPage * (parseInt(page_number) - 1);
                let params = utils.urlParams();
                params.page = page_number;
                params.start = start_record;
                params.rows = this.perPage + ''; // Need to be converted to a string

                this.$router.push({ name: 'browseDisplay', query: params });
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