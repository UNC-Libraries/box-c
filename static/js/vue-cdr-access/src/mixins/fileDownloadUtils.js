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

        largestImageEdge(brief_object) {
            const file_info = this.getOriginalFile(brief_object).split('|');
            const edge_sizes = file_info[file_info.length - 1].split('x').map(x => parseInt(x));
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

                if (this.validSizeOption(brief_object, 800)) {
                    html += `<a href="${this.imgDownloadLink(brief_object.id, '800')}" class="dropdown-item">${this.$t('full_record.small') } JPG (800px)</a>`;
                }
                if (this.validSizeOption(brief_object, 1600)) {
                    html += `<a href="${this.imgDownloadLink(brief_object.id, '1600')}" class="dropdown-item">${this.$t('full_record.medium') } JPG (1600px)</a>`;
                }
                if (this.validSizeOption(brief_object, 2500)) {
                    html += `<a href="${this.imgDownloadLink(brief_object.id, '2500')}" class="dropdown-item">${this.$t('full_record.large') } JPG (2500px)</a>`;
                }

                if (this.hasPermission(brief_object, 'viewOriginal')) {
                    html += `<a href="${this.imgDownloadLink(brief_object.id, 'full')}" class="dropdown-item">${this.$t('full_record.full_size')} JPG</a>`;
                    html += '<hr class="dropdown-divider">';
                    html += `<a href="/indexablecontent/${brief_object.id}?dl=true" class="dropdown-item">${this.$t('full_record.original_file')}</a>`;
                }

                html += '</div>';
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