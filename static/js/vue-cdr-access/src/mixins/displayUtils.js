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
        },

        linkLabel(title) {
            return `Visit ${title}`;
        },

        markedForDeletion(record) {
            if (record.status === undefined) return false;
            return /marked.*?deletion/i.test(this.restrictions(record));
        },

        isRestricted(record) {
            if (record.type === 'AdminUnit') return false;
            if (record.status === undefined) return true;
            return !record.status.includes('Public Access');
        },

        restrictions(record) {
            return record.status.join(',').toLowerCase();
        }
    }
}