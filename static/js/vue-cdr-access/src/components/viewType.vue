<!--
Buttons for switching display modes in a search result between gallery and list format.
-->
<template>
    <div id="browse-display-type" class="field has-addons" @click="setMode">
        <div class="control">
            <button id="list-display" :title="$t('view.list')" class="button is-medium" :class="listButtonClasses">
                <span class="icon is-medium"><i class="fas fa-th-list fa-2x"></i></span>
            </button>
        </div>
        <div class="control">
            <button id="gallery-display" :title="$t('view.gallery')" class="button is-medium" :class="galleryButtonClasses">
                <span class="icon is-medium"><i class="fas fa-th fa-2x"></i></span>
            </button>
        </div>
    </div>
</template>

<script>
    import routeUtils from "../mixins/routeUtils";
    export default {
        name: 'viewType',

        mixins: [routeUtils],

        data() {
            return {
                browse_type: 'list-display'
            }
        },

        watch: {
            '$route.query': {
                handler(params) {
                    this.browse_type = params.browse_type;
                },
                deep: true
            }
        },

        computed: {
            listButtonClasses() {
                return this.browse_type === "list-display"
                    ? "is-selected has-text-white has-background-primary"
                    : "has-text-grey-lighter";
            },
            galleryButtonClasses() {
                return this.browse_type === "gallery-display"
                    ? "is-selected has-text-white has-background-primary"
                    : "has-text-grey-lighter";
            }
        },

        methods: {
            setMode(e) {
                e.preventDefault();
                this.browse_type = e.target.closest('button').id;
                let update_params = { browse_type: encodeURIComponent(this.browse_type) };
                this.$router.push({ name: 'displayRecords', query: this.urlParams(update_params) }).catch((e) => {
                    if (e.name !== 'NavigationDuplicated') {
                        throw e;
                    }
                });
                sessionStorage.setItem('browse-type', this.browse_type);
            }
        },

        mounted() {
            let current_url_params = this.urlParams();

            if (this.paramExists('browse_type', current_url_params)) {
                this.browse_type = current_url_params.browse_type;
                sessionStorage.setItem('browse-type', this.browse_type);
            } else {
                this.browse_type = sessionStorage.getItem('browse-type');
            }
        }
    }
</script>

<style scoped lang="scss">
    #browse-display-type {
        justify-content: flex-end;
    }

    @media screen and (max-width: 768px) {
        #browse-display-type {
            justify-content: flex-start;
        }
    }
</style>