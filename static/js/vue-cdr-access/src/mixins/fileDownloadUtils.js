import permissionUtils from "./permissionUtils";

export default {
    mixins: [permissionUtils],

    data() {
        return {
            brief_object: this.briefObject || {}
        }
    },

    computed: {
        getOriginalFile() {
            const original_file =  this.brief_object.datastream.find(file => file.startsWith('original_file'));
            if (original_file === undefined) {
                return undefined;
            }

            return original_file;
        },

        largestImageEdge() {
            const file_info = this.getOriginalFile.split('|');
            const edge_sizes = file_info[file_info.length - 1].split('x');
            return edge_sizes[0] > edge_sizes[1] ? edge_sizes[0] : edge_sizes[1];
        },

        showImageDownload() {
            return this.hasPermission(this.brief_object, 'viewAccessCopies') &&
                this.brief_object.format.includes('Image') && this.getOriginalFile !== undefined
        }
    },

    methods: {
        imgDownloadLink(size) {
            return `/services/api/downloadImage/${this.brief_object.id}/${size}`
        },

        validSizeOption(size) {
            return size <= this.largestImageEdge;
        }
    }
}