<template>
    <iframe id="streaming-player" :src="streamingLink" :class="{ audio: briefObject.type === 'audio' }"
            allow="fullscreen" :title="briefObject.title"></iframe>
</template>

<script>

const STREAMREAPER_PREFIX_URL = "https://durastream.lib.unc.edu/player"

export default {
    name: 'listDisplay',

    props: {
        briefObject: Object
    },

    computed: {
        streamingLink() {
            return `https://durastream.lib.unc.edu/player?spaceId=${this.briefObject.folder}`
            + `&filename=${this.briefObject.filename}&refUrl=${location.href}`;
        }
    }
}
</script>

<style scoped lang="scss">
    #streaming-player {
        border: none;
        display: flex;
        height: 90vh;
        justify-content: center;
        margin: 10px auto;
        width: 90vw;
    }

    #streaming-player.audio {
        height: 30px;
        max-width: 500px
    }
</style>