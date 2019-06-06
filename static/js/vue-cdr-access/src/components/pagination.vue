<template>
    <div class="columns pagination">
        <div class="column is-12">
            <ul v-if="numberOfRecords > 0">
                <li v-if="pageFromUrl !== 1"><a class="back-next" @click.prevent="pageUrl(pageFromUrl - 1)" href="#">&lt;&lt;</a></li>
                <li v-else class="no-link">&lt;&lt;</li>
                <li v-for="page in totalPages">
                    <a @click.prevent="pageUrl(page)" href="#" class="page-number" :class="{ current: pageFromUrl === page }">{{ page }}</a>
                </li>
                <li v-if="pageFromUrl < totalPages"><a class="back-next" @click.prevent="pageUrl(pageFromUrl + 1)" href="#">&gt;&gt;</a></li>
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
                perPage: 20,
                startRecord: 1,
                totalPages: 1
            }
        },

        computed: {
            pageFromUrl() {
               if (isEmpty(this.$route.query)) {
                   return 1;
               }

               return parseInt(this.$route.query.page);
            }
        },

        methods: {
            setPageTotal() {
                this.totalPages = Math.ceil(this.numberOfRecords / this.perPage);
            },

            pageUrl(page_number) {
                let start_record = (parseInt(page_number ) - 1) + (this.perPage - 1);
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