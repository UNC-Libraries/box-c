<!--
Checkbox to switch search result modes between including only works with a flattened hierarchy,
and including all types with hierarchy retained.
-->
<template>
    <div id="browse-display-type" class="display-wrapper">
        <div class="field">
            <input @click="showWorks" type="checkbox" id="works-only" class="is-checkradio is-large" v-model="works_only">
            <label for="works-only"><p>{{ $t('works_only.show')}}</p></label>
        </div>
        <div class="note">
            <div class="display-note-btn has-tooltip-arrow has-tooltip-bottom"
                 :data-tooltip="$t('works_only.show_tip')">?
            </div>
        </div>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';

    export default {
        name: 'worksOnly',

        mixins: [routeUtils],

        data() {
            return {
                works_only: false
            }
        },

        watch: {
            '$route.query': {
                handler(params) {
                    this.works_only = this.coerceWorksOnly(params.works_only);
                },
                deep: true
            }
        },

        methods: {
            showWorks() {
                this.works_only = !this.works_only;

                let params = this.urlParams();
                params.types = this.updateWorkType(this.works_only).types;
                params.works_only = this.works_only;

                this.$router.push({ name: 'displayRecords', query: this.resetStartRow(params) }).catch((e) => {
                    if (e.name !== 'NavigationDuplicated') {
                        throw e;
                    }
                });
            }
        },

        mounted() {
            this.works_only = this.coerceWorksOnly(this.$route.query.works_only);
        }
    }
</script>

<style scoped lang="scss">
    #browse-display-type {
        font-size: 1rem;
        margin: inherit;

        label {
            display: inherit;
            font-size: 1rem;
            margin-right: 0;
            padding-right: 0;
            width: auto;
        }

        .note {
            padding-left: 0;
            padding-top: 10px;
        }

        .display-note-btn {
            margin-left: 5px;
            margin-right: 8px;
            margin-top: 5px;
        }

        p {
            font-size: 18px;
            margin-left: 0;
            padding-top: 10px;
        }

        [data-tooltip]:not(.is-disabled).has-tooltip-bottom.has-tooltip-arrow::after,
        [data-tooltip]:not(.is-loading).has-tooltip-bottom.has-tooltip-arrow::after,
        [data-tooltip]:not([disabled]).has-tooltip-bottom.has-tooltip-arrow::after {
            bottom: -5px;
        }

        [data-tooltip]:not(.is-disabled).has-tooltip-bottom::before,
        [data-tooltip]:not(.is-loading).has-tooltip-bottom::before,
        [data-tooltip]:not([disabled]).has-tooltip-bottom::before {
            margin-bottom: -10px;
        }
    }

    @media screen and (max-width: 1408px) {
        #browse-display-type {
            p {
                max-width: 90px;
            }
        }
    }

    @media screen and (max-width: 768px) {
        #browse-display-type {
            justify-content: flex-start;
            margin: 15px auto;

            input[type="checkbox"] {
                margin-left: 5px;
            }

            label {
                margin-right: 5px;
                min-width: 180px;
            }

            p {
                max-width: inherit;
            }
        }

        .note-wrapper {
            display: inherit;
        }
    }
</style>