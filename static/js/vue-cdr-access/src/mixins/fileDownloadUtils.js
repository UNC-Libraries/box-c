import permissionUtils from "./permissionUtils";

export default {
    mixins: [permissionUtils],

    methods: {
        showNonImageDownload(brief_object) {
            return this.hasPermission(brief_object, 'viewOriginal') &&
                !brief_object.format.includes('Image') && this.getOriginalFile(brief_object) !== undefined;
        },

        getOriginalFile(brief_object) {
            const original_file =  brief_object.datastream.find(file => file.startsWith('original_file'));
            if (original_file === undefined) {
                return undefined;
            }

            return original_file;
        },

        getJp2FileDimensions(brief_object) {
            const jp2_file =  brief_object.datastream.find(file => file.startsWith('jp2'));
            if (jp2_file === undefined) {
                return undefined;
            }

            // Check for jp2 dimensions
            const jp2_dimensions = this.getImageDimensions(jp2_file);
            if (jp2_dimensions === '') {
                return undefined;
            }

            return jp2_dimensions;
        },

        getImageDimensions(image_metadata) {
            const image_dimensions = image_metadata.split('|');
            return image_dimensions[image_dimensions.length - 1];
        },

        largestImageEdge(brief_object) {
            let dimensions_info = this.getJp2FileDimensions(brief_object);
            if (dimensions_info === undefined) {
                dimensions_info = this.getImageDimensions(this.getOriginalFile(brief_object));
            }
            const edge_sizes = dimensions_info.split('x').map(x => parseInt(x));

            return edge_sizes[0] > edge_sizes[1] ? edge_sizes[0] : edge_sizes[1];
        },

        showImageDownload(brief_object) {
            return this.hasPermission(brief_object, 'viewReducedResImages') &&
                brief_object.format.includes('Image') && this.getOriginalFile(brief_object) !== undefined
        },
        
        imgDownloadLink(file_id, size) {
            return `/services/api/downloadImage/${file_id}/${size}`
        },

        validSizeOption(brief_object, size) {
            return size <= this.largestImageEdge(brief_object);
        },

        downloadButtonHtml(brief_object) {
            if (this.showNonImageDownload(brief_object)) {
                return `<div class="actionlink download">
                            <a class="download button action" href="/content/${brief_object.id}?dl=true"><i class="fa fa-download"></i> ${this.$t('full_record.download')}</a>
                        </div>`;
            } else if (this.showImageDownload(brief_object)) {
                let html = `<div class="dropdown actionlink download image-download-options">
                <div class="dropdown-trigger">
                    <button id="dcr-download-${brief_object.id}" class="button download-images" aria-haspopup="true" aria-controls="dropdown-menu">
                    ${this.$t('full_record.download')} <i class="fas fa-angle-down" aria-hidden="true"></i>
                    </button>
                </div>
                <div class="dropdown-menu table-downloads" id="dropdown-menu" role="menu" aria-hidden="true">
                    <div class="dropdown-content">`;

                let opt_html = "";
                if (this.validSizeOption(brief_object, 800)) {
                    opt_html += `<a href="${this.imgDownloadLink(brief_object.id, '800')}" class="dropdown-item">${this.$t('full_record.small') } JPG (800px)</a>`;
                }
                if (this.validSizeOption(brief_object, 1600)) {
                    opt_html += `<a href="${this.imgDownloadLink(brief_object.id, '1600')}" class="dropdown-item">${this.$t('full_record.medium') } JPG (1600px)</a>`;
                }
                if (this.validSizeOption(brief_object, 2500)) {
                    opt_html += `<a href="${this.imgDownloadLink(brief_object.id, '2500')}" class="dropdown-item">${this.$t('full_record.large') } JPG (2500px)</a>`;
                }

                if (this.hasPermission(brief_object, 'viewOriginal')) {
                    opt_html += `<a href="${this.imgDownloadLink(brief_object.id, 'max')}" class="dropdown-item">${this.$t('full_record.full_size')} JPG</a>`;
                    opt_html += '<hr class="dropdown-divider">';
                    opt_html += `<a href="/indexablecontent/${brief_object.id}?dl=true" class="dropdown-item">${this.$t('full_record.original_file')}</a>`;
                }
                // No download options were available, so return no download button
                if (opt_html === '') {
                    return '';
                }

                html += opt_html + '</div>';
                html += '</div>';
                html += '</div>';

                return html;
            } else {
                return '';
            }
        },

        showDropdownList(e) {
            // Close any currently open dropdowns
            this.closeDropdownLists(e);

            if (e.target.id.startsWith('dcr-download')) {
                let drop_down = e.target.parentElement.parentElement.querySelector('.dropdown-menu');
                if (drop_down !== null) {
                    drop_down.setAttribute('aria-hidden', 'false');
                    drop_down.classList.add('show-list');
                }
            }
        },

        closeDropdownLists() {
            document.querySelectorAll('.show-list').forEach(element => {
                element.setAttribute('aria-hidden', 'true');
                element.classList.remove('show-list');
            });
        }
    },

    mounted() {
        document.addEventListener('click', this.showDropdownList);
        document.addEventListener('keyup', this.closeDropdownLists);
    },

    unmounted() {
        document.removeEventListener('click', this.showDropdownList);
        document.removeEventListener('keyup', this.closeDropdownLists);
    }
}