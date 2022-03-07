<template>
    <div id="browse-display-type" class="display-wrapper" v-if="!adminUnit">
        <div class="field">
            <input @click="showWorks" type="checkbox" id="works-only" class="is-checkradio is-large" v-model="works_only">
            <label for="works-only"><p>{{ $t('works_only.show')}}</p></label>
        </div>
        <div class="note">
            <div class="display-note-btn">?
                <div class="arrow"></div>
                <span class="browse-tip">{{ $t('works_only.show_tip') }}</span>
            </div>
        </div>
    </div>
</template>

<script>
    import routeUtils from '../mixins/routeUtils';

    export default {
        name: 'worksOnly',

        props: {
            adminUnit: Boolean
        },

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

                this.$router.push({ name: 'displayRecords', query: params }).catch((e) => {
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
            padding-top: 10px;
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