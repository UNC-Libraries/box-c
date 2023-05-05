/*
Utilities for search/browse result pages
*/
export default {
    methods: {
        recordUrl(idOrObject, browse_type = null) {
            let id = null;
            if (typeof idOrObject === 'string') {
                id = idOrObject;
            } else {
                id = idOrObject.id;
                // Don't apply browse types to works and files
                if (idOrObject.type == 'Work' || idOrObject.type == 'File') {
                    browse_type = null;
                }
            }
            if (browse_type) {
                return `/record/${id}/?browse_type=${browse_type}`;
            } else {
                return `/record/${id}`;
            }
        }
    }
}