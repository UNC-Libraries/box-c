<template>
    <div id="browse-display-type" class="display-wrapper">
        <div id="browse-btns" @click="setMode">
            <i id="gallery-display" title="Gallery View" class="fas fa-th" :class="{'is-selected': isGallery}"></i>
            <i id="list-display" title="List View" class="fas fa-th-list" :class="{'is-selected': !isGallery}"></i>
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
                browse_type: 'gallery-display'
            }
        },

        watch: {
            '$route.query'(params) {
                this.browse_type = params.browse_type;
            }
        },

        computed: {
            isGallery() {
                return this.browse_type === 'gallery-display';
            }
        },

        methods: {
            setMode(e) {
                e.preventDefault();
                this.browse_type = e.target.id;
                let update_params = { browse_type: encodeURIComponent(this.browse_type) };
                this.$router.push({ name: 'browseDisplay', query: this.urlParams(update_params) });
            }
        },

        mounted() {
            let current_url_params = this.urlParams();

            if (this.paramExists('browse_type', current_url_params)) {
                this.browse_type = current_url_params.browse_type;
            }
        }
    }
</script>

<style scoped lang="scss">
    #browse-display-type {
        justify-content: flex-end;

        #browse-btns {
            margin-top: 10px;

            i {
                border: 1px solid lightgray;
                border-radius: 5px;
                color: lightgray;
                font-size: 2.6rem;
                padding: 5px;
            }

            #gallery-display {
                border-bottom-right-radius: 0;
                border-top-right-radius: 0;
            }

            #list-display {
                border-bottom-left-radius: 0;
                border-top-left-radius: 0;
            }

            .is-selected {
                background-color: slategray;
                border-color: slategray;
                color: white;
            }
        }

        .browse-tip {
            line-height: 20px;
            width: 250px;
        }
    }
</style>