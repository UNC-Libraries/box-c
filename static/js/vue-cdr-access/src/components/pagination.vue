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
                startRecord: 1,
                totalPages: 1
            }
        },

        methods: {
            setPage() {
                let params = this._urlParams();

                if (params.page <= this.totalPages) {
                    this.currentPage = parseInt(params.page);
                } else {
                    this.currentPage = 1;
                }
            },

            _urlParams() {
                let params = window.location.search;
                let params_list = params.split('&');

                if (params_list.length === 1 && params_list[0] === '') {
                    this.startRecord = 1;

                    return {
                        page: this.currentPage,
                        rows: this.perPage,
                        start: this.startRecord
                    };
                }

                let page_params = {};
                params_list.forEach((p) => {
                   let param = p.split('=');
                   let key = param[0].replace('?', '');

                   if (key === 'start') {
                       this.startRecord = +param[1];
                   }

                   page_params[key] = param[1]
                });

                return page_params;
            },

            setPageTotal() {
                this.totalPages = Math.ceil(this.numberOfRecords / this.perPage);
            },

            pageUrl(page_number) {
                return `${this.pageBaseUrl}?page=${page_number}&start=${(parseInt(page_number ) - 1) + (this.perPage - 1)}&rows=${this.perPage}`
            },

            vueEventsWrapper() {
                this.setPageTotal();
                this.setPage();
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
    .pagination {
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