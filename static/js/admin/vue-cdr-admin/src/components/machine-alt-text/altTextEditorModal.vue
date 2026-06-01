<template>
    <div class="modal" :class="{'is-active': showAltTextModal}">
        <div class="modal-background"></div>
        <div class="modal-card">
            <header class="modal-card-head modal-head">
                <p class="modal-card-title is-capitalized">{{ modalHeader }}</p>
                <button class="delete" aria-label="close" @click=" closeModalWindow()"><span class="is-sr-only">Close</span></button>
            </header>
            <section class="modal-card-body">
                <textarea v-if="viewType ==='edit'" v-model="updated_text" class="modal-edit textarea" rows="20" aria-label="Edit"></textarea>
                <div v-else class="modal-view">{{ fieldText }}</div>
            </section>
            <footer class="modal-card-foot">
                <div class="buttons">
                    <button v-if="viewType ==='edit'" @click="updateValue()" class="button is-info" :class="{'is-loading': saving_data}">Update Text</button>
                    <button v-if="viewType ==='view'" @click="setViewType('edit')" class="button is-info">Edit Text</button>
                    <button @click=" closeModalWindow()" class="button is-danger">{{ closeModalButtonText }}</button>
                </div>
            </footer>
        </div>
    </div>
</template>

<script>
import fetchUtils from "@/mixins/fetchUtils";
import {mapActions, mapState} from "pinia";
import {useAltTextStore} from "@/stores/alt-text";

export default {
    name: 'altTextEditorModal',

    mixins: [fetchUtils],

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
        ...mapState(useAltTextStore, ['activeField', 'currentRow', 'currentUuid', 'showAltTextModal', 'viewType']),

        fieldTitle() {
            return this.activeField.split('_').join(' ');
        },

        fieldText() {
            return this.currentRow?.[this.activeField] ?? '';
        },

        closeModalButtonText() {
            return this.viewType === 'edit' ? 'Cancel' : 'Close';
        },

        modalHeader() {
            let header_text = (this.viewType === 'edit') ? 'Editing' : 'Viewing';
            return `${header_text} ${this.fieldTitle} for ${this.currentRow?.title}`;
        },

        /**
         * Returns the endpoint to be used for updating the value based on the active field.
         * It removes the leading mg prefix and converts the first character to lowercase to match the expected endpoint format.
         * @returns {string}
         */
        updateEndpoint() {
            let endpoint = this.activeField.replace(/^mg/, '');
            return endpoint[0].toLowerCase() + endpoint.slice(1);
        }
    },

    methods: {
        ...mapActions(useAltTextStore, ['setActiveField', 'setAlertMessage', 'setAlertMessageType',
            'setCurrentRow', 'setCurrentRowFieldValue', 'setLastSuccessfulEdit',
            'setShowAltTextModal', 'setViewType', 'closeModalWindow']),

        async updateValue() {
            this.saving_data = true;
            await this.updateRowData();
        },

        async updateRowData() {
            try {
                const endpoint = this.updateEndpoint;
                const targetId = this.currentRow?.id || this.currentUuid;
                const formBody = new URLSearchParams();
                formBody.append(endpoint, this.updated_text);

                await this.fetchWrapper(`/services/api/edit/${endpoint}/${targetId}`,
                    true, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                        body: formBody.toString()
                    });
                this.setCurrentRowFieldValue(this.activeField, this.updated_text);
                this.setLastSuccessfulEdit({
                    id: targetId,
                    field: this.activeField,
                    value: this.updated_text
                });
                this.setAlertMessage(`${this.activeField} updated successfully for ${this.currentRow?.title}`);
                this.setAlertMessageType('success');
            } catch {
                this.setAlertMessage(`Unable to update ${this.activeField} for ${this.currentRow?.title}`);
                this.setAlertMessageType('error');
            } finally {
                this.saving_data = false;
                this.closeModalWindow();
                setTimeout(() => {
                    this.setAlertMessage('');
                    this.setAlertMessageType('');
                }, 3500);
            }
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