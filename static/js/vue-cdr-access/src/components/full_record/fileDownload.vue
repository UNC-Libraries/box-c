<template>
    <div v-if="showNonImageDownload" class="actionlink download">
        <a class="download button action" :href="downloadLink"><i class="fa fa-download"></i> {{ $t('full_record.download') }}</a>
    </div>
    <div v-else-if="showImageDownload"
         class="dropdown actionlink download image-download-options" :class="{'is-active': show_options}">
        <div class="dropdown-trigger">
            <button @click="showOptions()" id="download-images" class="button" aria-haspopup="true" aria-controls="dropdown-menu">
                {{ $t('full_record.download') }} <i class="fas fa-angle-down" aria-hidden="true"></i>
            </button>
        </div>
        <div class="dropdown-menu" id="dropdown-menu" role="menu" :aria-hidden="!show_options">
            <div class="dropdown-content">
                <a v-if="validSizeOption(800)" :href="imgDownloadLink('800')" class="dropdown-item">{{ $t('full_record.small') }} JPG (800px)</a>
                <a v-if="validSizeOption(1600)" :href="imgDownloadLink('1600')" class="dropdown-item">{{ $t('full_record.medium') }} JPG (1600px)</a>
                <a v-if="validSizeOption(2500)" :href="imgDownloadLink('2500')" class="dropdown-item">{{ $t('full_record.large') }} JPG (2500px)</a>
                <template v-if="hasPermission(briefObject, 'viewOriginal')">
                    <a :href="imgDownloadLink('full')" class="dropdown-item">{{ $t('full_record.full_size') }} JPG</a>
                    <template v-if="downloadLink !== ''">
                        <hr class="dropdown-divider">
                        <a :href="downloadLink" class="dropdown-item">{{ $t('full_record.original_file') }}</a>
                    </template>
                </template>
            </div>
        </div>
    </div>
</template>

<script>
import fileDownloadUtils from '../../mixins/fileDownloadUtils';

export default {
    name: 'fileDownload',

    mixins: [fileDownloadUtils],

    props: {
        briefObject: {
            type: Object,
            default: {}
        },
        downloadLink: {
            type: String,
            default: ''
        }
    },

    watch: {
        briefObject(d) {
            this.brief_object = d;
        }
    },

    data() {
        return {
            show_options: false
        }
    },

    computed: {
        showNonImageDownload() {
            return this.hasPermission(this.brief_object, 'viewOriginal') &&
                !this.brief_object.format.includes('Image') && this.downloadLink !== '';
        }
    },

    methods: {
        closeOptions(e) {
            if (e.keyCode === 27 || (e.target.id !== 'download-images' && !/fa-angle-down/.test(e.target.className))) {
                this.show_options = false;
            }
        },

        showOptions() {
            this.show_options = !this.show_options;
        }
    },

    mounted() {
        document.addEventListener('click', this.closeOptions);
        document.addEventListener('keyup', this.closeOptions);
    },

    unmounted() {
        document.removeEventListener('click', this.closeOptions);
        document.removeEventListener('keyup', this.closeOptions);
    }
}
</script>

<style scoped lang="scss">
    .image-download-options {
        button {
            padding: 23px 15px;
        }
    }
</style>