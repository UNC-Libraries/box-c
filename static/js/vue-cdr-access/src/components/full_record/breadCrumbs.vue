<template>
    <div id="full_record_trail">
        <span class="hierarchicalTrail">
            <template v-for="(path, index) in objectPath">
                <template v-if="index === (breadcrumbSize - 1)">
                    <span class="quote">&raquo;</span>
                    <span>{{ path.name }}</span>
                </template>
                <template v-else-if="index <= 2 || breadcrumbSize <= 5 || (index === breadcrumbSize - 2)">
                    <a v-if="index === 0" href="/collections">{{ $t('breadcrumbs.collections') }}</a>
                    <template v-else>
                        <span class="quote">&raquo;</span>
                        <a :href="shiftFacetUrl(path.pid)">{{ path.name }}</a>
                    </template>
                </template>
                <template v-else>
                    <template v-if="index === 3 && !showFullBreadCrumb">
                        <span class="quote">&raquo;</span>
                        <a id="expand-breadcrumb" @click.prevent="showFullCrumb" href="#">&hellip;</a>
                    </template>
                    <template v-else>
                        <span class="full-crumb quote">&raquo;</span>
                        <a :href="shiftFacetUrl(path.pid)" class="full-crumb">{{ path.name }}</a>
                    </template>
                </template>
            </template>
        </span>
    </div>
</template>

<script>
export default {
    name: 'breadCrumbs',

    props: {
        objectPath: Object,
        queryPath: {
            type: String,
            default: 'record'
        }
    },

    data() {
        return {
            showFullBreadCrumb: false
        }
    },

    computed: {
        breadcrumbSize() {
            return this.objectPath.length;
        }
    },

    methods: {
        shiftFacetUrl(pid) {
            return `${this.queryPath}/${pid}`;
        },

        showFullCrumb() {
            this.showFullBreadCrumb = !this.showFullBreadCrumb;
        }
    }
}
</script>

<style scoped lang="scss">
    #full_record_trail {
        border: 1px solid #E0E0E0;
        background-color: #E0E0E0;
        border-radius: 5px;
        font-size: 18px;
        line-height: 1.2;
        margin: 0 auto 25px auto;
        padding: 20px;
        text-indent: 0;
        width: 100%;
        word-wrap: anywhere;

        a {
            color: #1A698C;
        }

        .quote {
            margin: 3px;
        }
    }

    #expand-breadcrumb {
        font-size: 24px;
    }
</style>