<template>
    <div class="columns pagination">
        <div class="column is-12">
            <ul v-if="numberOfRecords > 0">
                <li v-if="currentPage !== 1"><a class="back-next" :href="pageUrl(currentPage - 1)">&lt;&lt;</a></li>
                <li v-else class="no-link">&lt;&lt;</li>
                <li v-for="page in totalPages">
                    <a :href="pageUrl(page)" class="page-number" :class="{ current: currentPage === page }">{{ page }}</a>
                </li>
                <li v-if="currentPage < totalPages"><a class="back-next" :href="pageUrl(currentPage + 1)">&gt;&gt;</a></li>
                <li v-else class="no-link">&gt;&gt;</li>
            </ul>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'pagination',

        props: {
            numberOfRecords: Number,
            perPage: Number,
            pageBaseUrl: String
        },

        data() {
            return {
                currentPage: 1,
                totalPages: 1
            }
        },

        methods: {
            setPage() {
                let params = window.location.search;
                let page_number = params.split('=');

                if (page_number.length > 1 && page_number[1] <= this.totalPages) {
                    this.currentPage = parseInt(page_number[1]);
                } else {
                    this.currentPage = 1;
                }
            },

            setPageTotal() {
                this.totalPages = Math.ceil(this.numberOfRecords / this.perPage);
            },

            pageUrl(page_number) {
                return `${this.pageBaseUrl}?page=${page_number}`
            },

            // Should work with a zero based index
            currentPageRecordSet() {
                let  start_record = (this.perPage * this.currentPage) - this.perPage;
                this.$emit('pagination-records-to-display', {
                    start: start_record,
                    end: start_record + this.perPage,
                    totalPages: this.totalPages
                });
            },

            vueEventsWrapper() {
                this.setPageTotal();
                this.setPage();
                this.currentPageRecordSet();
            }
        },

        mounted() {
            this.vueEventsWrapper();
        },

        updated() {
            this.vueEventsWrapper();
        }
    }
</script>

<style scoped lang="scss">
    .pagination ul {
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