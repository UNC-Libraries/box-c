<template>
    <div class="column is-narrow action-btn item-actions">
        <template v-if="recordData.resourceType !== 'Work'">
            <div v-if="restrictedContent && !isLoggedIn" class="column is-narrow item-actions has-text-centered">
                <div class="restricted-access">
                    <h2>{{ $t('full_record.restricted_content', { resource_type: recordData.briefObject.type.toLowerCase() }) }}</h2>
                    <download-options :record-data="recordData.briefObject" :t="$t"></download-options>
                </div>
            </div>
            <div v-else class="field is-grouped is-justify-content-right">
                <download-options :record-data="recordData.briefObject" :t="$t"></download-options>
                <a class="button view action" :href="recordData.dataFileUrl" v-if="hasPermission(recordData, 'viewOriginal')">
                    <i class="fa fa-search" aria-hidden="true"></i> View</a>
            </div>
        </template>
        <div v-if="hasPermission(recordData, 'editDescription')" class="field is-grouped is-justify-content-right">
            <a class="edit button action is-primary" :href="editDescriptionUrl(recordData.briefObject.id)"><span class="icon"><i class="fa fa-edit"></i></span><span>{{ $t('full_record.edit') }}</span></a>
        </div>
        <div v-if="recordData.resourceType === 'File' && hasPermission(recordData, 'viewHidden')" class="is-justify-content-right">
            <single-use-link :uuid="recordData.briefObject.id"></single-use-link>
        </div>
    </div>
</template>

<script>
import downloadOptions from '@/components/full_record/downloadOptions.vue';
import singleUseLink from '@/components/full_record/singleUseLink.vue';
import fullRecordUtils from '../../mixins/fullRecordUtils';

export default {
    name: 'objectActions',

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
     .action-btn {
         padding-right: 0;
     }
 }
</style>