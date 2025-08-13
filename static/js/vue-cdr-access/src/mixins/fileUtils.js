export default {
    methods: {
        getOriginalFileValue(datastream_info, type) {
            for (let i in datastream_info) {
                const ds_parts = datastream_info[i].split("\|");
                if (ds_parts.length < 5 || ds_parts[0] !== 'original_file') {
                    continue;
                }
                if (type === 'file_type') {
                    return ds_parts[3];
                } else {
                    return this.formatFilesize(ds_parts[4])
                }
            }
            return '';
        },

        /**
         * @param record
         * @returns {string} File Type value for a work or file object. Will return the descriptive
         *      form of the file type if available, and fall back to the mimetype if not
         */
        getFileType(record) {
            let fileType = this.determineFileType(record.fileDesc);
            if (!fileType) {
                fileType = this.determineFileType(record.fileType);
            }
            return fileType || '';
        },

        /**
         * Determines which filetype should be shown
         * For multiple filetypes it de-dupes the array and if only one value show that, otherwise show 'Various'
         * @param fileTypes
         * @returns {string|*|undefined}
         */
        determineFileType(fileTypes) {
            if (fileTypes && fileTypes.length === 1) {
                return fileTypes[0];
            } else if (fileTypes && fileTypes.length > 1) {
                return ([...new Set(fileTypes)].length === 1) ? fileTypes[0] : 'Various';
            } else {
                return undefined;
            }
        },

        formatFilesize(bytes) {
            const fileBytes = parseInt(bytes);
            if (isNaN(fileBytes) || fileBytes === 0) {
                return '0 B';
            }

            const k = 1024;
            const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
            const i = Math.floor(Math.log(fileBytes) / Math.log(k));
            const val = (fileBytes / Math.pow(k, i)).toFixed(1);
            const floored_val = Math.floor(val);

            if (val - floored_val === 0) {
                return `${floored_val} ${sizes[i]}`;
            }

            return `${val} ${sizes[i]}`;
        }
    }
}