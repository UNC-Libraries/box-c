<template>
    <div class="clear">
        <template v-if="(recordData.viewerType === 'uv' && hasPermission(recordData, 'viewAccessCopies')) ||
                (recordData.viewerType === 'pdf' && hasPermission(recordData, 'viewOriginal'))">
            <iframe :src="viewer(recordData.viewerType)" allow="fullscreen" scrolling="no"></iframe>
        </template>
        <template v-else-if="recordData.viewerType === 'audio' && hasPermission(recordData, 'viewAccessCopies')">
            <audio-player :datafile-url="recordData.dataFileUrl"></audio-player>
        </template>
    </div>
</template>

<script>
import audioPlayer from '@/components/full_record/audioPlayer.vue';
import permissionUtils from '../../mixins/permissionUtils';

export default {
    name: 'player',

    components: {audioPlayer},

    mixins: [permissionUtils],

    props: {
        recordData: Object
    },

    methods: {
        viewer(viewer_type) {
            return `record/${this.recordData.briefObject.id}/${viewer_type}Viewer`;
        }
    }
}
</script>