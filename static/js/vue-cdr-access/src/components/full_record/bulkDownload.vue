<template>
    <div v-if="hasDownloadableContent" class="actionlink column pr-0 is-justify-content-flex-end">
        <a v-if="showTotalFilesize" class="bulk-download bulk-download-link button action" :href="downloadBulkUrl(workId)">
                    <span class="icon">
                        <i class="fa fa-archive"></i>
                    </span>
            <span>{{ $t('full_record.bulk_download') }} ({{ totalDownloadSize }})</span>
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
import fullRecordUtils from '../../mixins/fullRecordUtils';

export default {
    name: 'bulkDownload',

    mixins: [fullRecordUtils],

    props: {
        totalDownloadSize: {
            default: null,
            type: String
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
            return this.hasDownloadableContent && parseInt(this.totalDownloadSize) !== -1;
        }
    }
}
</script>