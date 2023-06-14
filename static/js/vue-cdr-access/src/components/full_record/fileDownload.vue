<template>
    <div v-if="hasPermission(briefObject, 'viewAccessCopies') && getOriginalFile !== undefined"
         class="dropdown" :class="{'is-active': show_options}" id="image-download-options">
        <div class="dropdown-trigger">
            <button @click="showOptions()" id="download-images" class="button" aria-haspopup="true" aria-controls="dropdown-menu">
                Download <i class="fas fa-angle-down" aria-hidden="true"></i>
            </button>
        </div>
        <div class="dropdown-menu" id="dropdown-menu" role="menu">
            <div class="dropdown-content">
                <a v-if="validSizeOption(800)" :href="downloadLink('800')" class="dropdown-item">Small JPG (800px)</a>
                <a v-if="validSizeOption(1600)" :href="downloadLink('1600')" class="dropdown-item">Medium JPG (1600px)</a>
                <a v-if="validSizeOption(2500)" :href="downloadLink('2500')" class="dropdown-item">Large JPG (2500px)</a>
                <a :href="downloadLink('full')" class="dropdown-item">Full Size JPG</a>
                <hr class="dropdown-divider">
                <a v-if="hasPermission(briefObject, 'viewOriginal')"
                   :href="downloadOriginalLink" class="dropdown-item">Original File</a>
            </div>
        </div>
    </div>
</template>

<script>
import permissionUtils from '../../mixins/permissionUtils';

export default {
    name: 'fileDownload',

    mixins: [permissionUtils],

    props: {
        briefObject: {
            type: Object,
            default: {}
        }
    },

    data() {
        return {
            show_options: false
        }
    },

    computed: {
        downloadOriginalLink() {
            return `/content/${this.briefObject.id}?dl=true`
        },

        getOriginalFile() {
            const original_file =  this.briefObject.datastream.find(file => file.startsWith('original_file'));
            if (original_file === undefined) {
                return undefined;
            }

            return original_file;
        },

        largestImageEdge() {
            const file_info = this.getOriginalFile.split('|');
            const edge_size = file_info[file_info.length - 1].split('x');
            return edge_size.sort((a, b) => a - b)[edge_size.length - 1];
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
        },

        downloadLink(size) {
            return `/services/api/downloadImage/${this.briefObject.id}/${size}`
        },

        validSizeOption(size) {
            return size <= this.largestImageEdge;
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
    #image-download-options {
        display: flex;
        justify-content: flex-end;
        margin: -25px auto 10px auto;
        width: 91.5%;

        button {
            background-color: #1A698C;
            color: white;

            &:hover,
            &:focus {
                background-color: #084b6b;
            }
        }

        .dropdown-menu {
            left: unset;
            right: 0;
        }

        .fa-angle-down {
            margin-left: 8px;
        }
    }
</style>