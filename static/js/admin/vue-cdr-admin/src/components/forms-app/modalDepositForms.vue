<template>
    <div id="dcr-forms-app" class="vf-boxc vue-dcr-admin-wrapper">
        <div class="columns is-centered">
            <div class="column is-12">
                <div class="modal" :class="{'is-active': showFormsModal}">
                    <div @click="closeModal()" class="modal-background"></div>
                    <div class="modal-content">
                        <div v-if="form === ''">
                            <h1 class="has-text-centered">Add a work to the current collection</h1>
                            <div class="column has-text-centered">
                                <div class="select">
                                    <select v-model="form" @change="getSchema()">
                                        <option value="">-- Please select a form --</option>
                                        <option value="generic_work">Generic Work</option>
                                        <option value="continuing_resource_item">Continuing Resource</option>
                                    </select>
                                </div>
                            </div>
                        </div>
                        <Vueform v-else
                                 :schema="schema"
                                 :endpoint="async (FormData, form$) => submitForm(FormData, form$)"
                                 @response="handleResponse"/>
                    </div>
                    <button @click="closeModal()" class="modal-close is-large" aria-label="close"></button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import {mapActions, mapState} from 'pinia';
import {useFormsStore} from '@/stores/forms';
import fetchUtils from '@/mixins/fetchUtils';

export default {
    name: 'modalDepositForms',

    mixins: [fetchUtils],

    data() {
        return {
            form: '',
            schema: {}
        }
    },

    computed: {
        ...mapState(useFormsStore, ['alertHandler', 'containerId', 'showFormsModal'])
    },

    methods: {
        ...mapActions(useFormsStore, ['setAlertHandler', 'setContainerId', 'setShowFormsModal']),

        async getSchema() {
            if (this.form !== '') {
                try {
                    this.schema = await this.fetchWrapper(`/static/deposit-forms/${this.form}.json`);
                } catch (error) {
                    console.log(error);
                }
            }
        },

        closeModal() {
            this.setShowFormsModal(false);
            this.setContainerId('');
            this.form = '';
            this.schema = {};
        },

        closeModalKeys(event) {
            if (event.code === 'Escape') {
                this.closeModal();
            }
        },

        /**
         * See https://vueform.com/docs/handling-form-data#submit-via-function
         * for different types of ways to submit data as multipart/form-data or application/json
         * @param FormData
         * @param form$
         * @returns {Promise<axios.AxiosResponse<any>>}
         */
        async submitForm(FormData, form$) {
            // Setting cancel token
            form$.cancelToken = form$.$vueform.services.axios.CancelToken.source();
            // Create a new FormData object for our custom submission
            const submissionData = new window.FormData();

            // Convert form data to a JSON string
            const jsonString = JSON.stringify(form$.requestData);
            const jsonFile = new File([jsonString], 'form-data.json', { type: 'application/json' });
            submissionData.append('file', jsonFile);
            submissionData.append('type', 'https://library.unc.edu/dcr/packaging/WorkFormJson1.0');

            return await form$.$vueform.services.axios.post(`/services/api/edit/ingest/${this.containerId}`,
                submissionData, {
                    cancelToken: form$.cancelToken.token,
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                }
            );
        },

        // axios response
        handleResponse(response, form$) {
            if (response.status === 200) {
                this.closeModal();
                this.alertHandler.alertHandler('success', 'Form submitted successfully.');
            } else {
                this.alertHandler.alertHandler('error', 'There was an error submitting the form.');
                console.log(response)
            }
        }
    },

    mounted() {
        window.addEventListener('keyup', this.closeModalKeys);
    },

    beforeUnmount() {
        window.removeEventListener('keyup', this.closeModalKeys);
    }
}
</script>

<style>
#dcr-forms-app {
    --forms-warning-color: #ef4444;
    --forms-warning-size: 1.5em;

    h1 {
        font-size: 2rem;
        font-weight: bold;
    }
    .vf-static-tag-h2 h2 {
        color: #005B90;
    }
    h2 {
        color: #005B90;
    }

    select {
        margin: auto;
    }
    .required-note {
        color: var(--forms-warning-color);
        font-size: var(--forms-warning-size);
    }
    .required {
        color: inherit;

        label {
            &:after {
                color: var(--forms-warning-color);
                content: " *";
                font-size: var(--forms-warning-size);
            }
        }
    }
    label span {
        font-weight: bold;
    }

    .modal {
        z-index: 99;
    }

    .modal-content {
        background: white;
        padding: 25px;
        min-height: 300px;
        width: 90%;
    }
}
</style>