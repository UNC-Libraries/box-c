<!--
Buttons for switching display modes in a search result between gallery and list format.
-->
<template>
    <div id="browse-display-type" class="display-wrapper">
        <div id="browse-btns" @click="setMode">
            <i id="list-display" :title="$t('view.list')" class="fas fa-th-list" :class="{'is-selected': !isGallery}"></i>
            <i id="gallery-display" :title="$t('view.gallery')" class="fas fa-th" :class="{'is-selected': isGallery}"></i>
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
            isGallery() {
                return this.browse_type === 'gallery-display';
            }
        },

        methods: {
            setMode(e) {
                e.preventDefault();
                this.browse_type = e.target.id;
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

        #browse-btns {
            margin-top: 10px;

            i {
                border: 1px solid lightgray;
                border-radius: 5px;
                color: lightgray;
                font-size: 38px;
                height: 50px;
                padding: 5px;
            }

            #list-display {
                border-bottom-right-radius: 0;
                border-top-right-radius: 0;
            }

            #gallery-display {
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

    @media screen and (max-width: 768px) {
        #browse-display-type {
            justify-content: flex-start;
            margin-left: 15px;
            margin-top: 5px;
        }
    }
</style>