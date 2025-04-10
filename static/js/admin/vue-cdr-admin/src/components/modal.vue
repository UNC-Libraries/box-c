<template>
    <div id="modal-editors">
        <modal-deposit-forms v-if="showFormsModal" />
        <modal-permissions-editor v-else />
    </div>
</template>

<script>
import modalDepositForms from "@/components/forms-app/modalDepositForms.vue";
import modalPermissionsEditor from "@/components/permissions-editor/ModalPermissionsEditor.vue";
import {mapState} from 'pinia';
import {useFormsStore} from '@/stores/forms';

export default {
    name: 'modal',

    components: { modalPermissionsEditor, modalDepositForms },

    computed: {
        ...mapState(useFormsStore, ['showFormsModal'])
    }
}
</script>