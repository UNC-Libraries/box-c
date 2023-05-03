/*
Utilities for search/browse result pages
*/
export default {
    methods: {
        recordUrl(id, browse_type = null) {
            if (browse_type) {
                return `/record/${id}/?browse_type=${browse_type}`;
            } else {
                return `/record/${id}`;
            }
        },

        linkLabel(title) {
            return `Visit ${title}`;
        }
    }
}