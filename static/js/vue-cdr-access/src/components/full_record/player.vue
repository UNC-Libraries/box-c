<template>
    <div>
        <template v-if="recordData.viewerType === 'streaming' && hasPermission(recordData, 'viewAccessCopies')">
            <streaming-player :record-data="recordData"></streaming-player>
        </template>
        <template v-else-if="recordData.viewerType === 'clover' && hasPermission(recordData, 'viewAccessCopies')">
            <clover-viewer :iiifContent="manifestPath"
                           :canvasOptions="cloverOptions"
                           download="false"
                           high-contrast
                           panel-open="false"></clover-viewer>
        </template>
        <template v-else-if="recordData.viewerType === 'pdf' && hasPermission(recordData, 'viewOriginal') && pdfFileAcceptableForDisplay">
          <div id="vpv" class="boxc-pdf-viewer">
            <VPdfViewer :src="pdfPath" initial-scale="pageWidth"/>
          </div>
        </template>
    </div>
</template>

<script>
import '@nulib/clover/web-components';
import { VPdfViewer, useLicense } from '@vue-pdf-viewer/viewer';
import streamingPlayer from '@/components/full_record/streamingPlayer.vue';
import permissionUtils from '../../mixins/permissionUtils';

const MAX_PDF_VIEWER_FILE_SIZE = 200000000; // ~200mb

export default {
    name: 'player',

    components: {streamingPlayer, VPdfViewer },

    mixins: [permissionUtils],

    props: {
        recordData: Object
    },

    data() {
      return {
        src: '',
      };
    },

    computed: {
        pdfFileAcceptableForDisplay() {
            const file_size = this.recordData.viewerFileSize;
            // Disable viewer if the file exceeds 200mb in size
            return file_size <= MAX_PDF_VIEWER_FILE_SIZE;
        },

        baseUrl() {
            const url_info = window.location;
            const port = url_info.port !== '' ? `:${url_info.port}` : '';
            return `${url_info.hostname}${port}`;
        },

        manifestPath() {
            return `https://${this.baseUrl}/services/api/iiif/v3/${this.recordData.briefObject.id}/manifest`
        },

        cloverOptions() {
            return {
                gestures: {
                    scrollToZoom: true
                }
            }
        },

        pdfPath() {
            return `https://${this.baseUrl}/indexablecontent/${this.recordData.viewerPid}`;
        }
    },

    beforeMount() {
        useLicense(window.pdfViewerLicense);
    }
}
</script>

<style>
    .clover-template-default {
        --clover-body-height: 100vh;
    }

    .clover-title:is(h2, h3) {
        color: black;
        font-size: 1.25rem;
        font-weight: bold;
    }

    .clover-canvas {
        --clover-canvas-background: #252523;
    }

    .clover-canvas-button {
        background-color: black;
        color: white;
        &:focus,
        &:active,
        &:hover {
            background-color: #0070D9;
        }
    }

    .clover-tip {
        --clover-tip-background: initial;
        --clover-color-primary: initial;
    }

    .vpv-container {
        padding-top: 25px;
    }

    .boxc-pdf-viewer {
        height: 700px;
        width: auto;
    }

    @media (max-width: 768px) {
        .boxc-pdf-viewer {
            height: 100vh;
        }
    }
</style>