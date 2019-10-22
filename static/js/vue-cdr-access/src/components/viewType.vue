<template>
    <div id="browse-display-type" class="display-wrapper">
        <p>How would you like to browse?</p>
        <div class="display-note-btn">?<div class="arrow"></div>
            <span class="browse-tip">
                <p><strong>Gallery</strong> view shows you all the files only, without the folders that store the files.</p>
                <p><strong>Structured</strong> view allows you to navigate one level at a time.</p>
            </span></div>
        <div id="browse-btns" @click="setMode">
            <button id="gallery-display" class="button is-light" :class="{'is-selected': isGallery}">Gallery</button>
            <button id="structure-display" class="button is-light" :class="{'is-selected': !isGallery}">Structured</button>
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
    .button.is-light {
        border: 1px solid darkgray;
    }

    .button.is-selected {
        background-color: slategray;
        border: 1px solid black;
        color: whitesmoke;
    }
</style>