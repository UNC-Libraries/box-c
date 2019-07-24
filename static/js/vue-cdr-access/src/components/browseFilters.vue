<template>
    <span class="filter-format">
        <span v-if="containerType === 'Collection' || containerType === 'Folder' || browseType ==='structure-display'">
            {{ noteText }}? <input @click="updateFilter" :title="noteText" class="checkbox" type="checkbox" v-model="filtered">
        </span>
    </span>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';

    export default {
        name: 'browseFilters',

        props: {
            browseType: String,
            containerType: String
        },

        mixins: [routeUtils],

        data() {
            return {
                browse_type_updated: false,
                filtered: false,
                update_params: {}
            }
        },

        watch: {
            '$route.query'(d) {
                this.filtered = 'format' in d || this.foldersOnly(d.types);
            },

            browseType(d) {
                this.browse_type_updated = true;
                this.updateFilter();
            }
        },

        computed: {
            noteText() {
                return this.browseType === 'structure-display' ? 'Show folders only' : 'Show images only';
            }
        },

        methods: {
            updateFilter() {
                let update_params = {
                    start: 0
                };

                let url_params = this.urlParams(update_params);

                if (this.browse_type_updated) {
                    this.filtered = false;
                    this.browse_type_updated = false; // reset browse change
                } else {
                    this.filtered = !this.filtered;
                }

                if (this.filtered && this.browseType === 'gallery-display') {
                    url_params.types = 'Work';
                    url_params.format = 'image';
                } else if (this.filtered && this.browseType === 'structure-display') {
                    url_params.types = 'Work,Folder';
                    delete url_params.format;
                } else {
                    url_params.types = 'Work';
                    delete url_params.format;
                }

                this.$router.push({ name: 'browseDisplay', query: url_params });
            }
        },

        mounted() {
            this.filtered = this.paramExists('format', this.urlParams()) || this.foldersOnly(this.$route.query.types);
        }
    }
</script>

<style lang="scss" scoped>
    $unc-blue: #4B9CD3;

    .filter-format {
        color: $unc-blue;

        .checkbox {
            height: 40px;
            width: 50px;
            vertical-align: middle;
            zoom: .6; /* Needed for webkit browsers */
        }
    }
</style>