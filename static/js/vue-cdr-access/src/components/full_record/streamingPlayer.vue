<template>
    <iframe id="streaming-player" :src="streamingLink" :class="{ audio: recordData.streamingType === 'sound' }"
            allow="fullscreen" :title="recordData.briefObject.title"></iframe>
</template>

<script>

export default {
    name: 'listDisplay',

    props: {
        recordData: Object
    },

    computed: {
        streamingLink() {
            return `${this.recordData.streamingUrl}&refUrl=${location.href}`;
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