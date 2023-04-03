<template>
    <div class="clear">
        <template v-if="(recordData.viewerType === 'uv' && hasPermission('viewAccessCopies')) ||
                (recordData.viewerType === 'pdf' && hasPermission('viewOriginal'))">
            <iframe :src="viewer(recordData.viewerType)" allow="fullscreen" scrolling="no"></iframe>
        </template>
        <template v-else-if="recordData.viewerType === 'audio' && hasPermission('viewAccessCopies')">
            <audio-player :datafile-url="recordData.dataFileUrl"></audio-player>
        </template>
    </div>
</template>

<script>
import audioPlayer from '@/components/full_record/audioPlayer.vue';
import fullRecordUtils from '../../mixins/fullRecordUtils';

export default {
    name: 'player',

    components: {audioPlayer},

    mixins: [fullRecordUtils],

    props: {
        recordData: Object
    },

    methods: {
        viewer(viewer_type) {
            return `record/${this.recordData.briefObject.id}/${viewer_type}Viewer`;
        },

        hasPermission(permission) {
            if (this.recordData.briefObject.permissions === undefined) {
                return false;
            }
            return this.recordData.briefObject.permissions.includes(permission);
        }
    }
}
</script>