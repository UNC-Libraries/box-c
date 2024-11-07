<template>
    <div class="column is-narrow action-btn item-actions">
        <div v-if="hasPermission(recordData, 'editDescription')" class="actionlink">
            <a class="edit button action" :href="editDescriptionUrl(recordData.briefObject.id)"><i class="fa fa-edit"></i> {{ $t('full_record.edit') }}</a>
        </div>
        <template v-if="recordData.resourceType === 'File'">
            <div v-if="restrictedContent && !isLoggedIn" class="column is-narrow item-actions">
                <div class="restricted-access actions">
                    <h2 class="has-text-centered">{{ $t('full_record.restricted_content', { resource_type: recordData.briefObject.type.toLowerCase() }) }}</h2>
                    <download-options :record-data="recordData.briefObject" :t="$t"></download-options>
                </div>
            </div>
            <template v-else>
                <download-options :record-data="recordData.briefObject" :t="$t"></download-options>
                <div class="actionlink" v-if="hasPermission(recordData, 'viewOriginal')">
                    <a class="button view action" :href="recordData.dataFileUrl">
                        <i class="fa fa-search" aria-hidden="true"></i> View</a>
                </div>
            </template>
            <template v-if="hasPermission(recordData, 'viewHidden')">
                <single-use-link :uuid="recordData.briefObject.id"></single-use-link>
            </template>
        </template>
    </div>
</template>

<script>
import downloadOptions from '@/components/full_record/downloadOptions.vue';
import singleUseLink from '@/components/full_record/singleUseLink.vue';
import fullRecordUtils from '../../mixins/fullRecordUtils';

export default {
    name: 'restrictedContent',

    components: {downloadOptions, singleUseLink},

    mixins: [fullRecordUtils],

    props: {
        recordData: Object
    }
}
</script>

<style scoped lang="scss">
 .button {
     white-space: normal;
 }

 .is-narrow {
     .dropdown {
         margin-top: 0;
     }
 }

 @media (max-width: 768px) {
     .actionlink {
         text-align: left;
         margin: auto;
         justify-content: left;
         width: 99%;
     }

     .action-btn {
         padding-right: 0;
     }
 }
</style>