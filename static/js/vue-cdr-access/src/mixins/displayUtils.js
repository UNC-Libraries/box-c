export default {
    methods: {
        recordUrl(id) {
            return `/record/${id}`;
        },

        recordType(type) {
            if (type === 'Collection') {
                return 'fa-archive';
            } else if (type === 'Folder') {
                return 'fa-folder';
            } else {
                return 'fa-file';
            }
        },

        hasThumbnail(thumb) {
            return thumb !== undefined && thumb !== '';
        }
    }
}