<template>
    <div class="audio-player">
        <audio controls>
            <source :src="fileLink" type="audio/mpeg">
            <a :href="downloadLink">{{ $t('full_record.download_file') }}</a>
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
            background: black;
            margin-top: 25px;
            margin-left: 50px;
            min-height: 40px;
            width: 400px;
        }
    }
</style>