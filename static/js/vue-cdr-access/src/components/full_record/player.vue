<template>
    <div>
        <template v-if="(recordData.viewerType === 'uv' && hasPermission(recordData, 'viewAccessCopies')) ||
                (recordData.viewerType === 'pdf' && hasPermission(recordData, 'viewOriginal') && pdfFileAcceptableForDisplay)">
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

const MAX_PDF_VIEWER_FILE_SIZE = 200000000; // ~200mb

export default {
    name: 'player',

    components: {audioPlayer},

    mixins: [permissionUtils],

    props: {
        recordData: Object
    },

    computed: {
        pdfFileAcceptableForDisplay() {
            const original_file = this.recordData.briefObject.datastream.find(file => file.startsWith('original_file'));
            if (original_file === undefined) {
                return false;
            }
            const file_info = original_file.split('|');
            const file_size = file_info[4];
            // Disable viewer if the file exceeds 200mb in size
            return file_size <= MAX_PDF_VIEWER_FILE_SIZE;
        }
    },

    methods: {
        viewer(viewer_type) {
            return `/record/${this.recordData.briefObject.id}/${viewer_type}Viewer`;
        }
    }
}
</script>