<template>
    <collection-folder-template :record-data="recordData"></collection-folder-template>
</template>

<script>
import fullRecordUtils from '../../mixins/fullRecordUtils';
import collectionFolderTemplate from '@/components/full_record/collectionFolderTemplate.vue';

export default {
    name: 'collectionRecord',

    mixins: [fullRecordUtils],

    components: { collectionFolderTemplate }
}
</script>