<!--
Checkbox to switch search result modes between including only works with a flattened hierarchy,
and including all types with hierarchy retained.
-->
<template>
    <div class="field is-grouped is-narrow">
        <div id="browse-display-type" class="field has-addons">
            <div class="control">
                <input @click="showWorks" type="checkbox" id="works-only" class="is-sr-only" v-model="works_only">
                <button @click="showWorks" id="works-only-off" class="button is-medium" :class="offButtonClasses">
                    Off
                </button>
            </div>
            <div class="control">
                <button @click="showWorks" id="works-only-on" class="button is-medium" :class="onButtonClasses">
                    On
                </button>
            </div>
        </div>
        <div class="field is-narrow">
            <div class="control">
                <label for="works-only" class="button is-medium">
                    <span>{{ $t('works_only.show') }}</span>
                    <span class="has-tooltip-arrow has-tooltip-bottom icon is-medium is-text-unc-blue"
                        :data-tooltip="$t('works_only.show_tip')"><i class="fas fa-question-circle fa-lg"></i></span>
                </label>
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

        computed: {
            offButtonClasses() {
                return !this.works_only
                    ? "is-selected has-text-white has-background-primary"
                    : "has-text-grey";
            },
            onButtonClasses() {
                return this.works_only
                    ? "is-selected has-text-white has-background-primary"
                    : "has-text-grey";
            }
        },

        methods: {
            showWorks() {
                this.works_only = !this.works_only;

                let params = this.urlParams({ user_set_params: true });
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
    label.button {
        border: none;
        box-shadow: none;
        padding-left: 0;

        &:hover {
            background-color: transparent;
        }
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
</style>