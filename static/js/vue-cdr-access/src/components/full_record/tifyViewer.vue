<template>
    <div id="tify" ref="tifyViewer"></div>
</template>

<script>
import 'tify'
import 'tify/dist/tify.css'

export default {
    name: 'Viewer',

    props: {
        objectId: String
    },

    data() {
        return {
            viewer: null
        }
    },

    mounted() {
        this.viewer = new Tify({
            container: this.$refs.tifyViewer,
            manifestUrl: `/jp2Proxy/${this.objectId}/jp2/manifest`
        });
    },

    beforeDestroy() {
        this.viewer.destroy();
    }
}
</script>

<style scoped lang="scss">
    #tify {
        height: 70vh;
        min-height: 500px;
        width: 90vw;
        z-index: 10;
        margin: auto;
    }
</style>