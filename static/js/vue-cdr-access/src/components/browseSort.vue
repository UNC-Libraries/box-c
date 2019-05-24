<template>
    <div class="browse-sort select is-medium">
        <select @change="sortRecords" v-model="sort_order">
            <option value="">Sort by...</option>
            <option value="title,normal">Title A-Z</option>
            <option value="title,reverse">Title Z-A</option>
            <option value="dateAdded,normal">Date Created (newest)</option>
            <option value="dateAdded,reverse">Date Created (oldest)</option>
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
                if (this.sort_order === '') {
                    this.sort_order = 'title,normal';
                }

                let sorted = `sort=${encodeURIComponent(this.sort_order)}`;
                let old_url = window.location.href;
                let old_params = window.location.search;
                let new_url;

                if (old_params !== '') {
                    let new_params = (/sort=/.test(old_params)) ? old_params.replace(/sort=.*?/, sorted) : `&${sorted}`;
                    new_url = `${old_url}${new_params}`;
                } else {
                    new_url = `${old_url}?${sorted}`
                }
                window.history.pushState(null, 'sort order', new_url);
                this.$emit('sort-ordering', sorted);
                this.sort_order = '';
            }
        }
    };
</script>

<style scoped lang="scss">
    .browse-sort {
        margin-top: 15px;
    }
</style>