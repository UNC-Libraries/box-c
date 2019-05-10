<template>
    <div class="columns pagination">
        <div class="column is-12">
            <ul v-if="numberOfRecords > 0">
                <li v-if="currentPage !== 1"><a class="back-next" :href="pageUrl(currentPage - 1)"><<</a></li>
                <li v-else class="no-link"><<</li>
                <li v-for="page in totalPages">
                     <a :href="pageUrl(page)" class="page-number" :class="{ current: currentPage === page }">{{ page }}</a>
                </li>
                <li v-if="currentPage < totalPages"><a class="back-next" :href="pageUrl(currentPage + 1)">>></a></li>
                <li v-else class="no-link">>></li>
            </ul>
        </div>
    </div>
</template>

<script>
    define(['Vue'], function(Vue) {
        Vue.component('pagination', {
            template: template,

            props: {
                numberOfRecords: Number,
                perPage: Number,
                pageBaseUrl: String
            },

            data: function() {
                return {
                    currentPage: 1,
                    totalPages: 1
                }
            },

            methods: {
                setPage: function() {
                    let params = window.location.search;
                    let page_number = params.split('=');

                    if (page_number.length > 1 && page_number[1] <= this.totalPages) {
                        this.currentPage = parseInt(page_number[1]);
                    } else {
                        this.currentPage = 1;
                    }
                },

                setPageTotal: function() {
                    this.totalPages = Math.ceil(this.numberOfRecords / this.perPage);
                },

                pageUrl: function(page_number) {
                    return `${this.pageBaseUrl}?page=${page_number}`
                },

                // Should work with a zero based index
                currentPageRecordSet: function() {
                    let  start_record = (this.perPage * this.currentPage) - this.perPage;
                    this.$emit('pagination-records-to-display', {
                        start: start_record,
                        end: start_record + this.perPage,
                        totalPages: this.totalPages
                    });
                },

                vueEventsWrapper: function() {
                    this.setPageTotal();
                    this.setPage();
                    this.currentPageRecordSet();
                }
            },

            mounted: function() {
                this.vueEventsWrapper();
            },

            updated: function() {
                this.vueEventsWrapper();
            }
        });
    });
</script>

<style>
    .pagination ul {
        display: inline;
    }

    .pagination ul li {
        display: inline;
        margin: 5px;
    }

    .pagination .page-number {
        background-color: #007FAE;
        padding: 15px;
    }

    .pagination a {
        color: white;
    }

    .pagination a.back-next {
        color: #007FAE;
    }

    .pagination .current {
        background-color: gray;
    }

    .pagination .no-link {
        color: gray;
    }

    .pagination .page-number:hover {
        color: white;
        opacity: .8;
        text-decoration: none;
    }
</style>