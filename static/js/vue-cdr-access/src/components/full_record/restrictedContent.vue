<template>
    <div class="column is-narrow action-btn item-actions">
        <div v-if="restrictedContent && !isLoggedIn" class="column is-narrow item-actions has-text-centered">
            <div class="restricted-access">
                <h2>{{ $t('full_record.restricted_content', { resource_type: recordData.briefObject.type.toLowerCase() }) }}</h2>
                <a v-if="hasGroupRole(recordData, 'canViewOriginals', 'authenticated')" class="button login-link action is-primary" :href="loginUrl"><span class="icon"><i class="fa fa-id-card"></i></span><span>{{ $t('access.login') }}</span></a>
                <a class="button contact action is-primary" href="https://library.unc.edu/contact-us/?destination=wilson"><span class="icon"><i class="fa fa-envelope"></i></span><span>{{ $t('access.contact') }}</span></a>
            </div>
        </div>
        <div v-if="recordData.resourceType === 'File' && hasDownloadAccess(recordData)" class="field is-grouped is-justify-content-right">
            <div class="header-button" v-html="downloadButtonHtml(recordData.briefObject)"></div>
            <a v-if="hasPermission(recordData, 'viewOriginal')" class="button view action is-primary" :href="recordData.dataFileUrl">
                <span class="icon"><i class="fa fa-search" aria-hidden="true"></i></span><span>View</span></a>
        </div>
        <div v-if="hasPermission(recordData, 'editDescription')" class="field is-grouped is-justify-content-right">
            <a class="edit button action is-primary" :href="editDescriptionUrl(recordData.briefObject.id)"><span class="icon"><i class="fa fa-edit"></i></span><span>{{ $t('full_record.edit') }}</span></a>
        </div>
        <div v-if="recordData.resourceType === 'File' && hasPermission(recordData, 'viewHidden')" class="is-justify-content-right">
            <single-use-link :uuid="recordData.briefObject.id"></single-use-link>
        </div>
    </div>
</template>

<script>
import singleUseLink from '@/components/full_record/singleUseLink.vue';
import fileDownloadUtils from '../../mixins/fileDownloadUtils';
import fullRecordUtils from '../../mixins/fullRecordUtils';

export default {
    name: 'restrictedContent',

    components: {singleUseLink},

    mixins: [fileDownloadUtils, fullRecordUtils],

    props: {
        recordData: Object
    }
}
</script>

<style scoped lang="scss">
 .button {
     white-space: normal;
 }

 .restricted-access {
     text-align: center;
 }

 .header-button {
     display: inline;
 }

 @media (max-width: 768px) {
     .actionlink {
         text-align: left;
         margin: auto;
         justify-content: left;
         width: 99%;
     }

     .header-button {
         display: block;
         margin-bottom: 3px;
         text-align: left;
     }
 }
</style>