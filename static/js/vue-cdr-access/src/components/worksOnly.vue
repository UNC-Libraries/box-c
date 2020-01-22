<template>
    <div id="browse-display-type" class="display-wrapper" v-if="!adminUnit">
        <div class="display-note-btn">?
            <div class="arrow"></div>
            <span class="browse-tip">Show all files without organizational folders.</span>
        </div>
        <input @click="showWorks" type="checkbox" class="checkbox" v-model="works_only">
        <p>Show only works</p>
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
            '$route.query'(params) {
                this.works_only = this.coerceWorksOnly(params.works_only);
            }
        },

        methods: {
            showWorks() {
                this.works_only = !this.works_only;

                let params = this.urlParams();
                params.types = this.updateWorkType(this.adminUnit, this.works_only).types;
                params.works_only = this.works_only;

                this.$router.push({ name: 'displayRecords', query: params });
            }
        },

        mounted() {
            this.works_only = this.$route.query.works_only === 'true';
        }
    }
</script>

<style scoped lang="scss">
    #browse-display-type {
        font-size: 1.2rem;
        padding-top: 25px;
        margin: inherit;

        .checkbox {
            height: 40px;
            margin-top: -10px;
            width: 50px;
            vertical-align: middle;
            zoom: .6; /* Needed for webkit browsers */
        }

        .display-note-btn {
            margin-left: 0;
            margin-right: 8px;
        }

        p {
            font-size: 18px;
            margin: 5px 0;
        }
    }

    @media screen and (max-width: 768px) {
        #browse-display-type {
            justify-content: flex-start;
            margin: auto;
        }
    }
</style>