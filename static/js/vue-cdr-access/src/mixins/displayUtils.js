export default {
    methods: {
        recordUrl(id, browse_type) {
            return `/record/${id}/?browse_type=${browse_type}`;
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