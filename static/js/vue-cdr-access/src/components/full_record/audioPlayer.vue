<template>
    <div class="audio-player">
        <audio controls :src="fileLink">
            <a :href="downloadLink">Download file</a>
        </audio>
    </div>
</template>

<script>
export default {
    name: 'audioPlayer',

    props: {
        datafileUrl: String
    },

    computed: {
        downloadLink() {
            return `/${this.datafileUrl}?dl=true`;
        },

        fileLink() {
            return `/${this.datafileUrl}`;
        }
    }
}
</script>

<style scoped lang="scss">
    .audio-player {
        border-bottom: 1px solid #E1E1E1;
        padding-bottom: 25px;

        audio {
            margin-top: 25px;
            margin-left: 50px;
        }
    }
</style>