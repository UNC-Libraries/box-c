<template>
    <div>
        <template v-if="recordData.viewerType === 'streaming' && hasPermission(recordData, 'viewAccessCopies')">
            <streaming-player :record-data="recordData"></streaming-player>
        </template>
        <template v-else-if="recordData.viewerType === 'pdf' && hasPermission(recordData, 'viewOriginal') && pdfFileAcceptableForDisplay">
            <iframe :src="viewer(recordData.viewerType)" allow="fullscreen" scrolling="no"></iframe>
        </template>
        <template v-else-if="recordData.viewerType === 'clover' && hasPermission(recordData, 'viewAccessCopies')">
            <clover :iiifContent="manifestPath" :options="cloverOptions"></clover>
        </template>
    </div>
</template>

<script>
import { applyPureReactInVue } from 'veaury';
import Viewer from '@samvera/clover-iiif/viewer';
import streamingPlayer from '@/components/full_record/streamingPlayer.vue';
import permissionUtils from '../../mixins/permissionUtils';

const MAX_PDF_VIEWER_FILE_SIZE = 200000000; // ~200mb

export default {
    name: 'player',

    components: {streamingPlayer, clover: applyPureReactInVue(Viewer) },

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
        },

        manifestPath() {
            const url_info = window.location;
            const port = url_info.port !== '' ? `:${url_info.port}` : '';
            return `https://${url_info.hostname}${port}/services/api/iiif/v3/${this.recordData.briefObject.id}/manifest`
        },

        cloverOptions() {
            return {
                canvasBackgroundColor: '#252523',
                informationPanel: {
                    open: false
                },
                openSeadragon: {
                    gestureSettingsMouse: {
                        scrollToZoom: true
                    }
                },
                showIIIFBadge: false
            }
        }
    },

    methods: {
        viewer(viewer_type) {
            return `/record/${this.recordData.briefObject.id}/${viewer_type}Viewer`;
        }
    }
}
</script>