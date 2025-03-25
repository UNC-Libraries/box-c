<template>
    <div>
        <template v-if="recordData.viewerType === 'streaming' && hasPermission(recordData, 'viewAccessCopies')">
            <streaming-player :record-data="recordData"></streaming-player>
        </template>
        <template v-else-if="recordData.viewerType === 'clover' && hasPermission(recordData, 'viewAccessCopies')">
            <clover :iiifContent="manifestPath" :options="cloverOptions"></clover>
        </template>
        <template v-else-if="recordData.viewerType === 'pdf' && hasPermission(recordData, 'viewOriginal') && pdfFileAcceptableForDisplay">
          <div id="vpv" class="boxc-pdf-viewer">
            <VPdfViewer :src="pdfPath" :initial-scale="pageWidth"/>
          </div>
        </template>
    </div>
</template>

<script>
import { applyPureReactInVue } from 'veaury';
import { VPdfViewer, useLicense } from '@vue-pdf-viewer/viewer';
import Viewer from '@samvera/clover-iiif/viewer';
import streamingPlayer from '@/components/full_record/streamingPlayer.vue';
import permissionUtils from '../../mixins/permissionUtils';

const MAX_PDF_VIEWER_FILE_SIZE = 200000000; // ~200mb

export default {
    name: 'player',

    components: {streamingPlayer, VPdfViewer, clover: applyPureReactInVue(Viewer) },

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
            const original_file = this.recordData.briefObject.datastream.find(file => file.startsWith('original_file'));
            if (original_file === undefined) {
                return false;
            }
            const file_info = original_file.split('|');
            const file_size = file_info[4];
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

<style lang="scss">
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