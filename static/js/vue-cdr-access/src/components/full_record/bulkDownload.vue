<template>
    <div v-if="hasDownloadableContent" class="column pr-0 has-text-right">
        <a v-if="showTotalFilesize" class="bulk-download bulk-download-link button action is-primary" :href="downloadBulkUrl(workId)">
                    <span class="icon">
                        <i class="fa fa-archive"></i>
                    </span>
            <span>{{ $t('full_record.bulk_download') }} ({{ formatFilesize(totalDownloadSize) }})</span>
        </a>
        <a v-else class="bulk-download bulk-download-email button action" href="https://library.unc.edu/contact-us/?destination=wilson">
            <span class="icon">
                <i class="fa fa-envelope"></i>
            </span>
            <span>{{ $t('access.contact') }}</span>
        </a>
    </div>
</template>

<script>
import fileUtils from '../../mixins/fileUtils';
import fullRecordUtils from '../../mixins/fullRecordUtils';

const ONE_GIGABYTE = 1073741824;

export default {
    name: 'bulkDownload',

    mixins: [fileUtils, fullRecordUtils],

    props: {
        totalDownloadSize: {
            default: null,
            type: Number
        },
        viewOriginalAccess: {
            default: false,
            type: Boolean
        },
        workId: String
    },

    computed: {
        hasDownloadableContent() {
            return this.viewOriginalAccess && this.totalDownloadSize !== null;
        },

        showTotalFilesize() {
            return this.hasDownloadableContent && this.totalDownloadSize <= ONE_GIGABYTE;
        }
    }
}
</script>