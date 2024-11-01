<template>
    <div v-if="!isLoggedIn" class="actionlink download">
        <a @click.prevent="modal_open = true" class="download button action" href="#">Contact Wilson/Log in to access</a>
    </div>
    <div v-else-if="showNonImageDownload(recordData)" class="actionlink download">
        <a class="download button action" :href="nonImageDownloadLink(recordData.id)"><i class="fa fa-download"></i> {{ t('full_record.download') }}</a>
    </div>
    <div v-else-if="showImageDownload(recordData)" class="dropdown actionlink download image-download-options">
        <div class="dropdown actionlink download image-download-options">
            <div class="dropdown-trigger">
                <button id="dcr-download-${brief_object.id}" class="button download-images" aria-haspopup="true" aria-controls="dropdown-menu">
                    {{ t('full_record.download') }} <i class="fas fa-angle-down" aria-hidden="true"></i>
                </button>
            </div>
            <div class="dropdown-menu table-downloads" id="dropdown-menu" role="menu" aria-hidden="true">
                <div class="dropdown-content">
                    <a v-if="validSizeOption(recordData, 800)" :href="imgDownloadLink(recordData.id, '800')" class="dropdown-item">{{ t('full_record.small') }} JPG (800px)</a>
                    <a v-if="validSizeOption(recordData, 1600)" :href="imgDownloadLink(recordData.id, '1600')" class="dropdown-item">{{ t('full_record.medium') }} JPG (1600px)</a>
                    <a v-if="validSizeOption(recordData, 2500)" :href="imgDownloadLink(recordData.id, '2500')" class="dropdown-item">{{ t('full_record.large') }} JPG (2500px)</a>
                    <a v-if="hasPermission(recordData, 'viewOriginal')" :href="imgDownloadLink(recordData.id, 'max')" class="dropdown-item">{{ t('full_record.full_size') }} JPG</a>
                    <hr class="dropdown-divider">
                    <a :href="originalImgDownloadLink(recordData.id)" class="dropdown-item">{{ t('full_record.original_file') }}</a>
                </div>
            </div>
        </div>
    </div>
    <div v-else></div>

    <div class="modal" :class="{ 'is-active': modal_open }">
        <div class="modal-background"></div>
        <div class="modal-card">
            <header class="modal-card-head">
                <p class="modal-card-title">{{ t('full_record.restricted_content', { resource_type: recordData.type.toLowerCase() }) }}</p>
                <button @click="modal_open = false" class="delete" aria-label="close"></button>
            </header>
            <section class="modal-card-body">
                <div class="restricted-access">
                    <div class="actionlink">
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
import fileDownloadUtils from "../../mixins/fileDownloadUtils";
import fullRecordUtils from '../../mixins/fullRecordUtils';
import {mapState} from "pinia";
import {useAccessStore} from "../../stores/access";

export default {
    name: 'downloadOptions',

    mixins: [fileDownloadUtils, fullRecordUtils],

    props: {
        recordData: Object,
        t: Function
    },

    data() {
        return {
            modal_open: false
        }
    },

    computed: {
        ...mapState(useAccessStore, [
            'isLoggedIn'
        ])
    }
}
</script>
<style scoped lang="scss">
.restricted-access {
    display: flex;
    justify-content: center;

    .actionlink {
        display: flex;
        flex-wrap: wrap;
        width: fit-content;

        &:last-of-type {
            margin-left: 15px !important;
        }

        i {
            padding-right: 8px;
        }
    }
}

@media screen and (max-width: 576px) {
    .restricted-access {
        flex-direction: column;

        .actionlink {
            &:last-of-type {
                margin-left: 0 !important;
                margin-top: 15px !important;
            }
        }
    }
}

</style>