<template>
    <div class="column is-narrow action-btn item-actions">
        <div v-if="showRestrictedActions" class="column is-narrow item-actions">
            <div class="restricted-access">
                <h2>{{ $t('full_record.restricted_content', { resource_type: recordData.briefObject.type.toLowerCase() }) }}</h2>
                <div v-if="hasGroupRole(recordData, 'canViewOriginals', 'authenticated')" class="actionlink"><a class="button login-link action" :href="loginUrl"><i class="fa fa-id-card"></i> {{ $t('access.login') }}</a></div>
                <div class="actionlink">
                    <a class="button contact action" href="https://library.unc.edu/wilson/contact/"><i class="fa fa-envelope"></i> {{ $t('access.contact') }}</a>
                </div>
            </div>
        </div>
        <div v-if="hasPermission(recordData, 'editDescription')" class="actionlink">
            <a class="edit button action" :href="editDescriptionUrl(recordData.briefObject.id)"><i class="fa fa-edit"></i> {{ $t('full_record.edit') }}</a>
        </div>
        <template v-if="recordData.resourceType === 'File' || recordData.resourceType === 'Work'">
            <file-download :download-link="downloadLink" :class="{has_restricted: showRestrictedActions }"
                           :brief-object="recordData.briefObject"></file-download>

            <div v-if="recordData.dataFileUrl && hasPermission(recordData, 'viewOriginal') && recordData.resourceType === 'File'" class="actionlink">
                <a class="button view action" :href="recordData.dataFileUrl">
                    <i class="fa fa-search" aria-hidden="true"></i> View</a>
            </div>
        </template>
        <div v-if="fieldExists(recordData.briefObject.embargoDate)" class="noaction">
            {{ $t('full_record.available_date', { available_date: formatDate(recordData.briefObject.embargoDate) }) }}
        </div>
    </div>
</template>

<script>
import fileDownload from '@/components/full_record/fileDownload.vue';
import fullRecordUtils from '../../mixins/fullRecordUtils';

export default {
    name: 'restrictedContent',

    components: {fileDownload},

    mixins: [fullRecordUtils],

    props: {
        recordData: Object
    },

    computed: {
        showRestrictedActions() {
            return this.restrictedContent && !this.isLoggedIn;
        }
    }
}
</script>

<style scoped lang="scss">
 .button {
     white-space: normal;
 }
 .restricted-access .actionlink {
     display: block;
 }

 .has_restricted {
     margin-left: 12px;
 }

 @media (max-width: 768px) {
     .actionlink {
         text-align: center;
         margin: auto;
         justify-content: center;
         width: 99%;
     }
 }
</style>