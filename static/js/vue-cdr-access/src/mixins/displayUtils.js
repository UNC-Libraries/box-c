export default {
    methods: {
        recordUrl(id, browse_type) {
            return `/record/${id}/?browse_type=${browse_type}`;
        },

        recordType(type) {
            if (type === 'AdminUnit') {
                return 'fa-university';
            } else if (type === 'Collection') {
                return 'fa-archive';
            } else if (type === 'Folder') {
                return 'fa-folder';
            } else {
                return 'fa-file';
            }
        },

        thumbnailPresent(thumb) {
            return thumb !== undefined && thumb !== '';
        },

        altText(title) {
            return `Thumbnail for ${title}`;
        }
    }
}