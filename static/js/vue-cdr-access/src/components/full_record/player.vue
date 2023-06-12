<template>
    <div>
        <template v-if="(recordData.viewerType === 'uv' && hasPermission(recordData, 'viewAccessCopies')) ||
                (recordData.viewerType === 'pdf' && hasPermission(recordData, 'viewOriginal'))">
            <iframe :src="viewer(recordData.viewerType)" allow="fullscreen" scrolling="no"></iframe>
            <file-download v-if="recordData.resourceType === 'File'" :brief-object="this.recordData.briefObject"></file-download>
        </template>
        <template v-else-if="recordData.viewerType === 'audio' && hasPermission(recordData, 'viewAccessCopies')">
            <audio-player :datafile-url="recordData.dataFileUrl"></audio-player>
        </template>
    </div>
</template>

<script>
import audioPlayer from '@/components/full_record/audioPlayer.vue';
import fileDownload from '@/components/full_record/fileDownload.vue';
import permissionUtils from '../../mixins/permissionUtils';

export default {
    name: 'player',

    components: {audioPlayer, fileDownload},

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