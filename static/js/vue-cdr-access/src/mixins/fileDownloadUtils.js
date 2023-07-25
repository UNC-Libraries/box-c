import permissionUtils from "./permissionUtils";

export default {
    mixins: [permissionUtils],

    methods: {
        getOriginalFile(brief_object) {
            const original_file =  brief_object.datastream.find(file => file.startsWith('original_file'));
            if (original_file === undefined) {
                return undefined;
            }

            return original_file;
        },

        largestImageEdge(brief_object) {
            const file_info = this.getOriginalFile(brief_object).split('|');
            const edge_sizes = file_info[file_info.length - 1].split('x');
            return edge_sizes[0] > edge_sizes[1] ? edge_sizes[0] : edge_sizes[1];
        },

        showImageDownload(brief_object) {
            return this.hasPermission(brief_object, 'viewOriginal') &&
                brief_object.format.includes('Image') && this.getOriginalFile(brief_object) !== undefined
        },
        
        imgDownloadLink(file_id, size) {
            return `/services/api/downloadImage/${file_id}/${size}`
        },

        validSizeOption(brief_object, size) {
            return size <= this.largestImageEdge(brief_object);
        }
    }
}