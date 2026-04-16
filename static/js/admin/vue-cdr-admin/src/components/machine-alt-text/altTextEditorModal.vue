<template>
    <div class="modal" :class="{'is-active': showAltTextModal}">
        <div class="modal-background"></div>
        <div class="modal-card">
            <header class="modal-card-head modal-head">
                <p class="modal-card-title is-capitalized">{{ modalHeader }}</p>
                <button class="delete" aria-label="close" @click="closeModal()"></button>
            </header>
            <section class="modal-card-body">
                <textarea v-if="viewType ==='edit'" v-model="updated_text" class="modal-edit textarea" rows="20" aria-label="Edit"></textarea>
                <div v-else class="modal-view">{{ fieldText }}</div>
            </section>
            <footer class="modal-card-foot">
                <div class="buttons">
                    <button v-if="viewType ==='edit'" @click="updateValue()" class="button is-info" :class="{'is-loading': saving_data}">Update Text</button>
                    <button @click="closeModal()" class="button is-danger">{{ closeModalButtonText }}</button>
                </div>
            </footer>
        </div>
    </div>
</template>

<script>
import {mapActions, mapState} from "pinia";
import {useAltTextStore} from "@/stores/alt-text";

export default {
    name: 'altTextEditorModal',

    data() {
        return {
            saving_data: false,
            updated_text: ''
        }
    },

    watch: {
        viewType(viewType) {
            this.updated_text = (viewType === 'edit') ? this.fieldText : '';
        },

        currentRow: {
            deep: true,
            handler() {
                if (this.viewType === 'edit') {
                    this.updated_text = this.fieldText;
                }
            }
        }
    },

    computed: {
        ...mapState(useAltTextStore, ['activeField', 'currentRow', 'showAltTextModal', 'viewType']),

        fieldTitle() {
            return this.activeField.split('_').join(' ');
        },

        fieldText() {
            return this.currentRow?.[this.activeField] ?? '';
        },

        closeModalButtonText() {
            return this.viewType === 'edit' ? 'Cancel' : 'Close';
        },

        /**
         * Extracts the image name from the filename path in the current row.
         * It can initially be undefined, so always check that it's been set
         * @returns {*|string}
         */
        imageName() {
            const img_path = this.currentRow?.filename?.split('/');
            return img_path !== undefined ? img_path[img_path.length - 1] : '';
        },

        modalHeader() {
            let header_text = (this.viewType === 'edit') ? 'Editing' : 'Viewing';
            return `${header_text} ${this.fieldTitle} for ${this.imageName}`;
        }
    },

    methods: {
        ...mapActions(useAltTextStore, ['setActiveField', 'setAlertMessage', 'setAlertMessageType',
            'setCurrentRow', 'setCurrentRowFieldValue', 'setShowAltTextModal', 'setViewType']),

        updateValue() {
            this.saving_data = true;
            this.setCurrentRowFieldValue(this.activeField, this.updated_text);
            this.setAlertMessage('Value updated successfully updated');
            this.setAlertMessageType('success');
            this.saving_data = false;
            this.closeModal();
        },

        closeModal() {
            this.setShowAltTextModal(false);
            this.setViewType('view');
            this.setActiveField('');
            this.setCurrentRow(null);
        }
    }
}
</script>

<style scoped>
    .modal-head {
        align-items: baseline;
        justify-content: space-between;

        .modal-card-title {
            max-width: 94%
        }
    }
</style>