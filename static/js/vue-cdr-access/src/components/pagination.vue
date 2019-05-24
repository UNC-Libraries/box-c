<template>
    <div class="columns pagination">
        <div class="column is-12">
            <ul v-if="numberOfRecords > 0">
                <li v-if="currentPage !== 1"><a class="back-next" @click.prevent="pageUrl(currentPage - 1)" href="#">&lt;&lt;</a></li>
                <li v-else class="no-link">&lt;&lt;</li>
                <li v-for="page in totalPages">
                    <a @click.prevent="pageUrl(page)" href="#" class="page-number" :class="{ current: currentPage === page }">{{ page }}</a>
                </li>
                <li v-if="currentPage < totalPages"><a class="back-next" @click.prevent="pageUrl(currentPage + 1)" href="#">&gt;&gt;</a></li>
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
                }
            },

            _urlParams() {
                let params = window.location.search;
                let params_list = params.split('&');
                let page_params = {};

                if (params_list.length > 1) {
                    params_list.forEach((p) => {
                        let param = p.split('=');
                        let key = param[0].replace('?', '');

                        if (key === 'start') {
                            this.currentPage = '';
                            this.startRecord = +param[1];
                        }

                        page_params[key] = param[1]
                    });
                } else {
                    page_params['page'] = this.currentPage;
                }

                return page_params;
            },

            setPageTotal() {
                this.totalPages = Math.ceil(this.numberOfRecords / this.perPage);
            },

            pageUrl(page_number) {
                let start_record = (parseInt(page_number ) - 1) + (this.perPage - 1);
                let new_params = `?page=${page_number}&start=${start_record}&rows=${this.perPage}`;

                // Can use the commented out version if data returned from the server is fixed to not return the server name
                // let new_url = `${this.pageBaseUrl}${new_params}`;
                let new_url = `${this.pageBaseUrl.replace('milford', 'cdr-qa-fe4-fes')}${new_params}`;

                this.currentPage = page_number;

                window.history.pushState(null, page_number, new_url);
                this.$emit('pagination-records-to-display', new_params);
            }
        },

        updated() {
            this.setPageTotal();
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