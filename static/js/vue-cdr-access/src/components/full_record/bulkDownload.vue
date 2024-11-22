<template>
    <div v-if="hasDownloadableContent" class="column pr-0 has-text-right">
        <a v-if="showTotalFilesize"
           class="bulk-download bulk-download-link button action is-primary"
           :href="downloadBulkUrl(workId)"
           @click.prevent="handleDownload">
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
        hasBulkDownloadAccess: {
            default: false,
            type: Boolean
        },
        childCount: {
            default: null,
            type: Number
        },
        workId: String
    },

    computed: {
        hasTooManyChildren() {
            // Based off of the limit set in DownloadBulkService
            return this.childCount > 100;
        },

        hasDownloadableContent() {
            return this.hasBulkDownloadAccess && this.totalDownloadSize !== null;
        },

        showTotalFilesize() {
            return this.hasBulkDownloadAccess && this.totalDownloadSize <= ONE_GIGABYTE;
        }
    },

    methods: {
        handleDownload(event) {
            if (this.hasTooManyChildren) {
                // Prevent navigation if user cancels
                if (!confirm("Number of files exceeds the download limit, only the first 100 will be exported, do you want continue?")) {
                    return;
                }
            }

            // Trigger the download
            const link = event.target.closest('a');
            window.location.href = link.href;
        }
    }
}
</script>