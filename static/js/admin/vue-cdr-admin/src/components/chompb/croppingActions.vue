<template>
    <div class="buttons has-addons" :class="{ 'pull-right': actionType === 'problem' }">
        <span class="button is-outlined search problem">
            <input type="text" v-model="search" :placeholder="actionType" @keyup="searchData()" @click="showTable()"/>
        </span>
        <span class="button is-outlined" title="Search">
            <span class="icon"><i class="fas fa-search search"></i></span>
        </span>
        <span class="button is-outlined" title="Clear Search" @click="clearSearch()">
            <span class="icon"><i class="fas fa-times"></i></span>
        </span>
        <span class="button is-outlined" title="Sort Alphabetically" @click="updateSortingDirection('alpha')">
            <span class="icon">
                <i class="fas" :class="{
                    'fa-sort-alpha-down': sorting.alpha === 'asc',
                    'fa-sort-alpha-up': sorting.alpha === 'desc'
                }"></i>
            </span>
        </span>
        <span class="button is-outlined" title="Sort Numerically" @click="updateSortingDirection('numeric')">
            <span class="icon">
                <i class="fas" :class="{
                    'fa-sort-numeric-down': sorting.numeric === 'asc',
                    'fa-sort-numeric-up': sorting.numeric === 'desc'
                }"></i>
            </span>
        </span>
        <span class="button is-outlined" title="Hide" @click="hide_table = !hide_table">
            <i class="fas" :class="{ 'fa-chevron-down': hide_table, 'fa-chevron-up': !hide_table }"></i>
        </span>
    </div>
    <div :class="{ 'pull-right': actionType === 'problem' }">
        <table :class="{'is-hidden': hide_table}">
            <tr v-for="value in filtered_cropping_data">
                <td>
                    <div>
                        <span class="name">{{ value.name }}</span>
                        <span class="count">{{ value.count }}</span>
                    </div>
                </td>
            </tr>
        </table>
    </div>
</template>

<script>
export default {
    name: 'croppingActions',

    props: {
        actionType: String,
        bulkActions: {
            type: Object,
            default: () => {
                return {
                    collapse_all: false,
                    show_all: false,
                    clear_all: false
                }
            }
        },
        croppingData: {
            type: Array,
            default: () => {
                return [];
            }
        }
    },

    data() {
        return {
            filtered_cropping_data: [],
            grouped_cropping_data: [],
            hide_table: true,
            search: '',
            sorting: {
                alpha: 'asc',
                numeric: 'asc'
            }
        }
    },

    watch: {
        bulkActions: {
            handler(newActions) {
                if (newActions.collapse_all) {
                    this.hide_table = true
                }
                if (newActions.show_all) {
                    this.hide_table = false
                }
                if (newActions.clear_all) {
                    this.search = '';
                    this.filtered_cropping_data = this.grouped_cropping_data;
                }
            },
            deep: true
        }
    },

    computed: {
        groupingType() {
            return this.actionType === 'class' ? 'pred_class' : 'problem';
        }
    },

    methods: {
        showTable() {
            this.hide_table = false;
        },

        /**
         * Group cropping data by a specific field and then turn this into an array of objects
         */
        groupCroppingData() {
            let grouped_data = Object.groupBy(this.croppingData, d => d[this.groupingType]);
            let grouped_data_to_array = Object.keys(grouped_data).map((key) => {
                return { name: key, count: grouped_data[key].length }
            });
            this.grouped_cropping_data = grouped_data_to_array;
            this.filtered_cropping_data = grouped_data_to_array;
        },

        updateSortingDirection(sort_type) {
            this.showTable();
            const sorting_types = ['alpha', 'numeric'];
            sorting_types.forEach((type) => {
                if (type === sort_type) {
                    this.sorting[type] = this.sorting[type] === 'asc' ? 'desc' : 'asc';
                    this.sortResults(type);
                } else {
                    this.sorting[type] = 'asc';
                }
            });
        },

        sortResults(sort_type) {
            this.grouped_cropping_data.sort((a, b) => {
                if (sort_type === 'alpha') {
                    return this.sorting[sort_type] === 'asc' ? a.name.localeCompare(b.name) : b.name.localeCompare(a.name);
                } else if (sort_type === 'numeric') {
                    return this.sorting[sort_type] === 'asc' ? a.count - b.count : b.count - a.count;
                }
            });
        },

        searchData() {
            this.showTable();
            this.filtered_cropping_data = this.grouped_cropping_data.filter(d => d.name.startsWith(this.search));
            if (this.search === '') {
                this.filtered_cropping_data = this.grouped_cropping_data;
            }
        },

        /**
         * Reset the search display
         */
        clearSearch() {
            this.search = '';
            this.filtered_cropping_data = this.grouped_cropping_data;
        }
    },

    mounted() {
        this.groupCroppingData();
    }
}
</script>

<style scoped lang="scss">
    i, .icon {
        color: #655967;
        pointer-events: none;
    }

    .search:has(input) {
        padding: 0;
    }

    .search {
        input {
            height: 100%;
            width: 250px;
        }
        input::placeholder {
            text-transform: capitalize
        }
    }



    table {
        border: 1px solid lightgray;
        margin: 0;
        clear: both;
        border-collapse: separate;
        border-spacing: 0;
        width: 78%;

        td {
            padding: 10px;
            div {
                display: flex;
            }
        }
    }

    .name {
        text-overflow: ellipsis;
        overflow: hidden;
        display: inline-block;
        vertical-align: middle;
        white-space: nowrap;
        flex-grow: 1;
        text-align: left;
    }

    .count {
        display: inline-block;
        background-color: #cfcfcf;
        text-align: center;
        border-radius: 10px;
        width: auto;
        min-width: 30px;
        color: black;
        font-size: .9em;
        padding: 0 4px;
    }
</style>