<template>
    <div v-if="!isLoggedIn && restrictedFiles(recordData) && hasGroupRole(recordData, 'canViewOriginals', 'authenticated')" class="actionlink download">
        <a @click.prevent="modal_open = true" class="download login-modal-link button action" href="#">Contact Wilson/Log in to access</a>
    </div>
    <div v-else-if="!isLoggedIn && restrictedFiles(recordData)" class="download">
        <div class="actionlink">
            <a class="button contact action" href="https://library.unc.edu/contact-us/?destination=wilson"><i class="fa fa-envelope"></i> {{ t('access.contact') }}</a>
        </div>
    </div>
    <div v-else-if="showNonImageDownload(recordData)" class="actionlink download">
        <a class="download button action" :href="nonImageDownloadLink(recordData.id)"><i class="fa fa-download"></i> {{ t('full_record.download') }}</a>
    </div>
    <div v-else-if="showImageDownload(recordData) && hasDownloadOptions(recordData)" class="dropdown actionlink download image-download-options">
        <div class="dropdown download image-download-options">
            <div class="dropdown-trigger">
                <button @click="toggleDownloadOptions()" id="dropdown-menu-button" class="button download-images" aria-haspopup="true" aria-controls="dropdown-menu">
                    {{ t('full_record.download') }} <i class="fas fa-angle-down" aria-hidden="true"></i>
                </button>
            </div>
            <div class="dropdown-menu table-downloads" :class="{ 'show-list': download_options_open }" id="dropdown-menu" role="menu" :aria-hidden="!download_options_open">
                <div class="dropdown-content">
                    <a v-if="validSizeOption(recordData, 800)" :href="imgDownloadLink(recordData.id, '800')" class="dropdown-item">{{ t('full_record.small') }} JPG (800px)</a>
                    <a v-if="validSizeOption(recordData, 1600)" :href="imgDownloadLink(recordData.id, '1600')" class="dropdown-item">{{ t('full_record.medium') }} JPG (1600px)</a>
                    <a v-if="validSizeOption(recordData, 2500)" :href="imgDownloadLink(recordData.id, '2500')" class="dropdown-item">{{ t('full_record.large') }} JPG (2500px)</a>
                    <template v-if="hasPermission(recordData, 'viewOriginal')">
                        <a :href="imgDownloadLink(recordData.id, 'max')" class="dropdown-item">{{ t('full_record.full_size') }} JPG</a>
                        <hr class="dropdown-divider">
                        <a :href="originalImgDownloadLink(recordData.id)" class="dropdown-item">{{ t('full_record.original_file') }}</a>
                    </template>
                </div>
            </div>
        </div>
    </div>
    <div v-else-if="showImageDownload(recordData) && !hasDownloadOptions(recordData)" class="download">
        <div class="actionlink">
            <a class="button contact action" href="https://library.unc.edu/contact-us/?destination=wilson"><i class="fa fa-envelope"></i> {{ t('access.contact') }}</a>
        </div>
    </div>

    <div v-if="!isLoggedIn && restrictedFiles(recordData) && hasGroupRole(recordData, 'canViewOriginals', 'authenticated')" class="modal" :class="{ 'is-active': modal_open }">
        <div class="modal-background"></div>
        <div class="modal-card">
            <header class="modal-card-head">
                <p class="modal-card-title">{{ t('full_record.restricted_content', { resource_type: recordData.type.toLowerCase() }) }}</p>
                <button @click="modal_open = false" class="delete close" aria-label="close"></button>
            </header>
            <section class="modal-card-body">
                <div class="restricted-access downloads">
                    <div v-if="hasGroupRole(recordData, 'canViewOriginals', 'authenticated')" class="actionlink">
                        <a class="button login-link action" :href="loginUrl"><i class="fa fa-id-card"></i> {{ t('access.login') }}</a>
                    </div>
                    <div class="actionlink">
                        <a class="button contact action" href="https://library.unc.edu/contact-us/?destination=wilson"><i class="fa fa-envelope"></i> {{ t('access.contact') }}</a>
                    </div>
                </div>
            </section>
        </div>
    </div>
</template>

<script>
import fullRecordUtils from '../../mixins/fullRecordUtils';
import permissionUtils from "../../mixins/permissionUtils";
import {mapState} from "pinia";
import {useAccessStore} from "../../stores/access";

export default {
    name: 'downloadOptions',

    mixins: [fullRecordUtils, permissionUtils],

    props: {
        recordData: Object,
        t: Function
    },

    data() {
        return {
            download_options_open: false,
            modal_open: false

        }
    },

    computed: {
        ...mapState(useAccessStore, [
            'isLoggedIn'
        ])
    },

    methods: {
        restrictedFiles(recordData) {
            if (!this.hasGroups(recordData) ||
                recordData.groupRoleMap.everyone === undefined) {
                return false;
            }
            if (recordData.groupRoleMap.everyone.includes('canViewOriginals')) {
                return false;
            }
            // For File objects, content is not restricted if the user can at least download low res files
            // Record is assumed to be a file
            if (recordData.type === 'File' && this.hasDownloadAccess(recordData) && this.hasDownloadOptions(recordData)) {
                return false;
            }
            return true;
        },

        toggleDownloadOptions() {
            this.download_options_open = !this.download_options_open;
        },

        closeOverlays(event) {
            if (event.code === 'Escape' || parseInt(event.code) === 27) {
                this.download_options_open = false;
                this.modal_open = false;
            }
        },

        hasDownloadOptions(recordData) {
            if (this.hasPermission(recordData, 'viewOriginal')) {
                return true;
            }

            let has_download_options = false;
            const valid_sizes = [800, 1600, 2500];
            for (let i=0; i < valid_sizes.length; i++) {
                if (this.validSizeOption(recordData, valid_sizes[i])) {
                    has_download_options = true;
                    break;
                }
            }

            return has_download_options;
        },

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

        originalImgDownloadLink(id) {
            return `/content/${id}?dl=true`;
        },

        nonImageDownloadLink(id) {
            return `/content/${id}?dl=true`;
        },

        /**
         * Create a global listener to close the menu by clicking anywhere on the screen
         */
        closeDropdownLists(e) {
            if (e.target.id !== 'dropdown-menu-button') {
                this.download_options_open = false;
            }
        }
    },

    mounted() {
        window.addEventListener('click', this.closeDropdownLists);
        window.addEventListener('keyup', this.closeOverlays);
    },

    beforeUnmount() {
        window.removeEventListener('click', this.closeDropdownLists);
        window.removeEventListener('keyup', this.closeOverlays);
    }
}
</script>
<style scoped lang="scss">
    .modal-card-title {
        font-weight: bold;
        text-align: center;
    }

    .modal-card {
        border-radius: 5px;

        .modal-card-head {
            align-items: flex-start;
        }

        .button {
            display: flex;
            font-size: 1rem !important;
            padding: 0 10px !important;
            height: 2rem;
        }
    }

    .actionlink .contact i,
    .restricted-access .actionlink i {
        padding-right: 8px;
    }

    .restricted-access {
        display: flex;
        justify-content: center;

        .actionlink {
            display: flex;
            flex-wrap: wrap;
            margin: auto;
            width: fit-content;

            &:last-of-type {
                margin-left: 15px;
            }
        }
    }

    @media screen and (max-width: 576px) {
        .modal-card-body {
            .restricted-access {
                flex-direction: column;
                align-items: center;

                .actionlink + .actionlink {
                    margin-left: 0;
                    margin-right: 0;
                    margin-top: 8px;
                }
            }
        }

        .item-actions {
            .image-download-options {
                .image-download-options .dropdown-menu {
                    left: 0;
                }
            }
        }
    }
</style>