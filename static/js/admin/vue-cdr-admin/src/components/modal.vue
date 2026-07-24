<template>
    <div id="modal-editors">
        <modal-deposit-forms v-if="showFormsModal" />
        <modal-file-versioning v-else-if="showFileVersioningModal" />
        <modal-permissions-editor v-else />
    </div>
</template>

<script>
import modalDepositForms from "@/components/forms-app/modalDepositForms.vue";
import modalFileVersioning from "@/components/file-versioning/modalFileVersioning.vue";
import modalPermissionsEditor from "@/components/permissions-editor/modalPermissionsEditor.vue";
import {mapState} from 'pinia';
import {useFormsStore} from '@/stores/forms';
import {useFileVersioningStore} from "@/stores/file-versioning";

export default {
    name: 'modal',

    components: { modalPermissionsEditor, modalDepositForms, modalFileVersioning },

    computed: {
        ...mapState(useFormsStore, ['showFormsModal']),
        ...mapState(useFileVersioningStore, ['showFileVersioningModal'])
    }
}
</script>