<template>
    <div class="browse-sort select is-medium">
        <select @change="sortRecords" v-model="sort_order">
            <option value="">Sort by...</option>
            <option value="title-asc">Title A-Z</option>
            <option value="title-desc">Title Z-A</option>
            <option value="added-desc">Date Created (newest)</option>
            <option value="added-asc">Date Created (oldest)</option>
        </select>
    </div>
</template>

<script>
    export default {
        name: 'browseSort',

        props: {
            records: Array
        },

        data() {
            return {
                sort_order: ''
            }
        },

        methods: {
            sortRecords() {
                let sort_values = this.sort_order.split('-');
                let sort_field = sort_values[0] || 'title';
                let sort_order = sort_values[1] || 'asc';

                this.$emit('sort-ordering', this._doSort(sort_field, sort_order));
                this.sort_order = '';
            },

            _doSort(sort_field, sort_order) {
                let sorted;

                if (sort_order === 'asc') {
                    sorted = this.records.sort(function (a, b) {
                        if (a[sort_field] < b[sort_field]) {
                            return -1;
                        }

                        if (a[sort_field] > b[sort_field]) {
                            return 1;
                        }

                        return 0;
                    });
                } else {
                    sorted = this.records.sort(function (a, b) {
                        if (b[sort_field] < a[sort_field]) {
                            return -1;
                        }

                        if (b[sort_field] > a[sort_field]) {
                            return 1;
                        }

                        return 0;
                    });
                }

                return sorted;
            }
        }
    };
</script>

<style scoped lang="scss">
    .browse-sort {
        margin-top: 15px;
    }
</style>