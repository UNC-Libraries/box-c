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

        getFileType(record) {
            let fileTypes = record.fileDesc;
            let fileType;
            if (fileTypes && fileTypes.length > 0) {
                fileType = fileTypes[0];
            }
            if (!fileType) {
                fileTypes = record.fileType;
                if (fileTypes && fileTypes.length > 0) {
                    fileType = fileTypes[0];
                }
            }
            return fileType || '';
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