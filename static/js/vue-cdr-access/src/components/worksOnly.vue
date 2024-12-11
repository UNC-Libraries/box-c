<!--
Checkbox to switch search result modes between including only works with a flattened hierarchy,
and including all types with hierarchy retained.
-->
<template>
    <div id="browse-display-type" class="field is-narrow">
        <div class="control">
            <input @click="showWorks" type="checkbox" id="works-only" class="checkbox" v-model="works_only">
            <label for="works-only" class="checkbox is-medium">
                {{ $t('works_only.show') }}
                <span class="has-tooltip-arrow has-tooltip-bottom icon is-medium is-text-unc-blue"
                    :data-tooltip="$t('works_only.show_tip')"><i class="fas fa-question-circle fa-lg"></i></span>
            </label>
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
    input.checkbox {
        display: none;
    }

    label.checkbox {
        white-space: nowrap;
        font-size: 18px;
    }

    label.checkbox::before {
        border-radius: 6px;
        content: '';
        display: inline-block;
        width: 50px;
        height: 50px;
        margin-right: 10px;
        border: 1px solid #ddd;
        vertical-align: middle;
    }

    input.checkbox:checked + label.checkbox::before {
        content: "\2713";
        font-size: 30px;
        color: #999999;
        text-align: center;
        line-height: 55px;
        background-color: #ffffff;
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